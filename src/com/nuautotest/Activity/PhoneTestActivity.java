package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.File;
import java.io.IOException;

public class PhoneTestActivity extends Activity {
	public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";

	private MediaRecorder mediaRecorder=null;
	private MediaPlayer   mPlayer=null;
	private MyPhoneStateListener mListener;
	Button m_phone1Button, m_phone2Button;
	TelephonyManager manager;
	Context m_context;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phone_test);

		m_phone1Button = (Button) findViewById(R.id.btPhone1);
		m_phone2Button = (Button)findViewById(R.id.btPhone2);
		m_context = getApplicationContext();

		m_phone1Button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String strNum = "10010";
				Intent intent = new Intent(ACTION_CALL_PRIVILEGED, Uri.parse("tel:"+strNum));
				try {
					startActivity(intent);
				} catch (Exception e) {
					intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+strNum));
					startActivity(intent);
				}
			}
		});

		m_phone2Button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String strNum = "112";
				Intent intent = new Intent(ACTION_CALL_PRIVILEGED, Uri.parse("tel:"+strNum));
				try {
					startActivity(intent);
				} catch (Exception e) {
					intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+strNum));
					startActivity(intent);
				}
			}
		});

		manager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
		Log.d(ModuleTestApplication.TAG, "==oncreate  00");
		mListener = new MyPhoneStateListener();
		manager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);

//		m_context.getContentResolver().registerContentObserver(
//				Uri.parse("content://call_log"), true, new CallContentObserver(m_context, null));

		mediaRecorder= new MediaRecorder();
	}

	@Override
	protected void onPause()
	{
		if (mPlayer != null) {
			if (mPlayer.isPlaying()) mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}

		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		Log.d(ModuleTestApplication.TAG, "start onDestroy~~~");
		if (mediaRecorder != null) {
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
		}
		manager.listen(mListener, PhoneStateListener.LISTEN_NONE);
		super.onDestroy();
	}

	class MyPhoneStateListener extends PhoneStateListener{
		File audioFile;
		private boolean iscall=false;
		private boolean isStart=false;

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					if (iscall) {
						Log.d(ModuleTestApplication.TAG, "==CALL_STATE_IDLE");
						if (isStart) {
							mediaRecorder.stop();
							mediaRecorder.reset();
//							mediaRecorder.release();
//							mediaRecorder=null;
							isStart = false;
						}

						iscall=false;
						try {
							Process p;
							int status;
							try {
								Log.d(ModuleTestApplication.TAG, "==CALL_STATE_IDLE  00");
								p = Runtime.getRuntime().exec("chmod 777 " +  audioFile.getAbsolutePath());
								Log.d(ModuleTestApplication.TAG, "==CALL_STATE_IDLE  000000");
								try {
									status = p.waitFor();
									Log.d(ModuleTestApplication.TAG, "==CALL_STATE_IDLE  11");
									if (status == 0) {
										Log.d(ModuleTestApplication.TAG, "==chmod success");
									} else {
										Log.d(ModuleTestApplication.TAG, "==chmod failed");
									}
								} catch (InterruptedException e) {
									Log.d(ModuleTestApplication.TAG, "==process waitfor() failed");
									e.printStackTrace();
								}
							} catch (IOException e) {
								Log.e(ModuleTestApplication.TAG, "exec chmod failed");
							}

							Log.d(ModuleTestApplication.TAG, "==CALL_STATE_IDLE  22");

							Uri uri = Uri.parse(audioFile.getAbsolutePath());
							mPlayer = MediaPlayer.create(m_context, uri);

							if (mPlayer != null) {
								mPlayer.stop();
							}

							Log.d(ModuleTestApplication.TAG, "=play audio name ="+audioFile.getAbsolutePath());
							mPlayer.prepare();
							Log.d(ModuleTestApplication.TAG, "=after mediaplayer prepare");
							mPlayer.start();
							Log.d(ModuleTestApplication.TAG, "=after mediaplayer start");
							Toast.makeText(m_context, "开始播放录音", Toast.LENGTH_LONG).show();
							mPlayer.setOnCompletionListener(new OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer mp) {
									Toast.makeText(m_context, "录音播放完毕", Toast.LENGTH_LONG).show();
								}
							});
						}
						catch (IOException e) {
							Log.e(ModuleTestApplication.TAG, "prepare() failed");
						}
					}
					break;

				case TelephonyManager.CALL_STATE_OFFHOOK:
					Log.d(ModuleTestApplication.TAG, "==CALL_STATE_OFFHOOK");
					try {
						if(!iscall) {
							iscall = true;
							recordCallComment();
						}
					} catch (IOException e) {
						Log.e(ModuleTestApplication.TAG, "==mediaRecorder start failed");
						e.printStackTrace();
						mediaRecorder.stop();
					}
					break;

				default:
					break;
			}
			super.onCallStateChanged(state, incomingNumber);
		}

		public void recordCallComment() throws IOException{
			if (!isStart) {
				Log.d(ModuleTestApplication.TAG, "==recordCallComment start");
				isStart = true;
//				if (mediaRecorder==null) {
				if (mediaRecorder != null) {
					mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start 0");
					mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start 1");
					mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start2");

					audioFile = new File(ModuleTestApplication.LOG_DIR + "/record_1.amr");
					System.out.println(audioFile.getAbsolutePath());
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start3");
					mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start4");
					System.out.println("==audioFile="+audioFile.getAbsolutePath());
					Log.d(ModuleTestApplication.TAG, "==recordCallComment start5");

					mediaRecorder.prepare();
					mediaRecorder.start();
				}
//				}
			}
		}
	}

/*	public class CallContentObserver extends ContentObserver {
		private final static String strUriInbox = "content://call_log/calls";
		private final Uri uriCall = Uri.parse(strUriInbox);
		private Context context;

		public CallContentObserver(Context context, Handler handler) {
			super(handler);
			this.context = context;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Log.d(ModuleTestApplication.TAG, "==CallContentObserver onChange  0000");
			Cursor c = context.getContentResolver().query(uriCall,
					new String[] { "_id", "number", "date" }, null, null, null);
			if (c != null && c.moveToFirst()) {
				String num = c.getString(1);
				String id = c.getString(0);
				Log.d(ModuleTestApplication.TAG, "==CallContentObserver onChange  1111");
				if (num != null) {
					Log.d(ModuleTestApplication.TAG, "==CallContentObserver onChange 2222");
					context.getContentResolver().delete(uriCall, "_id=" + id, null);
				}
				c.close();
			}
		}
	}*/

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.phone_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.phone_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}
}
