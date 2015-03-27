package com.nuautotest.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;

/**
 * 耳机测试
 *
 * @author xie-hang
 *
 */

public class HeadsetTestActivity extends Activity
{
	private static final String mRecordName = ModuleTestApplication.LOG_DIR + "/headsetrecordtest.aac";

	private TextView mHeadsetState, mKeyHeadset;
	private Button mPlayMusic, mbtRecord, mbtPlay;
	private LinearLayout mllHeadsetKey, mllHeadsetRecord;
	private BroadcastReceiver mPlugListener;
	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null, mPlayer2 = null;

	boolean mPlayingFile = false, mRecording = false, mPlaying = false;

	protected static final int PLAYFILE = 0X105;

	public Thread thread;

	private boolean isAutomatic;
	private boolean isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	private static final int MSG_TIMEOUT = 0x106;
	private int mTimeout;
	private TimerHandler mTimerHandler;

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case HeadsetTestActivity.PLAYFILE:
					playFile();
					break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.headset_test);
		initCreate();

		mHeadsetState = (TextView)findViewById(R.id.headsetState);
		mPlayMusic = (Button)findViewById(R.id.btPlayMusic);
		mPlayMusic.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPlayingFile = !mPlayingFile;
				playFile();
			}
		});
		mllHeadsetKey = (LinearLayout)findViewById(R.id.llHeadsetKey);
		mKeyHeadset = (TextView)findViewById(R.id.keyHeadset);
		mllHeadsetRecord = (LinearLayout)findViewById(R.id.llHeadsetRecord);
		mbtRecord = (Button)findViewById(R.id.btHeadsetRecord);
		mbtPlay = (Button)findViewById(R.id.btHeadsetPlay);
		mbtRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRecording = !mRecording;
				record();
			}
		});
		mbtPlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlaying = !mPlaying;
				play();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean mAutomatic = HeadsetTestActivity.this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 28;
			mTimerHandler = new TimerHandler();
			TimerThread mTimer = new TimerThread();
			mTimer.start();
		}
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if (mAudioManager == null) Log.e(ModuleTestApplication.TAG, "Get AudioManager failed");
		else {
			if (mAudioManager.isWiredHeadsetOn()) mHeadsetState.setText("已插入");
		}
	}

	private void playFile() {
		if (mPlayingFile) {
			mPlayer = MediaPlayer.create(this, R.raw.audiofiletest);
			mPlayer.start();
			mPlayMusic.setText("停止播放");
		} else {
			if (mPlayer != null) {
				mPlayer.release();
				mPlayer = null;
			}
			mPlayMusic.setText("播放音乐");
		}
	}

	private void record() {
		if (mRecording) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
			mRecorder.setOutputFile(mRecordName);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			try {
				mRecorder.prepare();
			} catch (IOException e) {
				Log.e(ModuleTestApplication.TAG, "prepare() failed");
			}
			mRecorder.start();
			mbtRecord.setText("停止录音");
		} else {
			if (mRecorder != null) {
				mRecorder.stop();
				mRecorder.release();
				mRecorder = null;
			}
			mbtRecord.setText("开始录音");
		}
	}

	class OnRecordCompletionListener implements MediaPlayer.OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			try {
				MediaPlayer next;
				if (mp == mPlayer)
					next = mPlayer2;
				else
					next = mPlayer;

				int current = mp.getCurrentPosition();
				next.seekTo(current);
				Log.d(ModuleTestApplication.TAG, "complete on: " + current);

				mp.reset();
				mp.setDataSource(mRecordName);
				mp.prepare();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void play() {
		if (mPlaying) {
			try {
				mPlayer = new MediaPlayer();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(mRecordName);
				mPlayer.setOnCompletionListener(new OnRecordCompletionListener());
				mPlayer.setNextMediaPlayer(mPlayer2);
				mPlayer.prepare();

				mPlayer2 = new MediaPlayer();
				mPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer2.setDataSource(mRecordName);
				mPlayer2.setOnCompletionListener(new OnRecordCompletionListener());
				mPlayer2.setNextMediaPlayer(mPlayer);
				mPlayer2.prepare();

				mPlayer.start();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mbtPlay.setText("停止播放");
		} else {
			if (mPlayer != null) {
				mPlayer.release();
				mPlayer = null;
			}
			if (mPlayer2 != null) {
				mPlayer2.release();
				mPlayer2 = null;
			}
			mbtPlay.setText("开始播放");
		}
	}

	@Override
	public void onPause() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}

		super.onPause();
	}

	@Override
	public void onDestroy() {
		releaseDestroy();
		super.onDestroy();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.headset_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.headset_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	protected class TimerThread extends Thread {
		@Override
		public void run() {
			while (mTimeout > 0) {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mTimeout--;
			}
			mTimerHandler.sendEmptyMessage(MSG_TIMEOUT);
		}
	}

	protected class TimerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_TIMEOUT) {
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.headset_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.headset_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					HeadsetTestActivity.this.finish();
				}
			}
		}
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_headset.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Headset Test---");

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
		mPlugListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int state, microphone;

				if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
					state = intent.getIntExtra("state", 0);
					microphone = intent.getIntExtra("microphone", 0);
					if (state == 1) {
						if (isAutomatic) stopAutoTest(true);
						else if (microphone == 1) {
							mHeadsetState.setText("四段式耳机已插入");
							mKeyHeadset.setBackgroundColor(Color.RED);
							mllHeadsetKey.setVisibility(View.VISIBLE);
							mllHeadsetRecord.setVisibility(View.VISIBLE);
						} else {
							mHeadsetState.setText("三段式耳机已插入");
							mllHeadsetKey.setVisibility(View.INVISIBLE);
							mllHeadsetRecord.setVisibility(View.INVISIBLE);
						}
					} else {
						if (!isAutomatic) {
							mHeadsetState.setText("未插入");
							mllHeadsetKey.setVisibility(View.INVISIBLE);
							mllHeadsetRecord.setVisibility(View.INVISIBLE);
						}
					}
				}
			}
		};
		mContext.registerReceiver(mPlugListener, intentFilter);
	}

	public void releaseDestroy() {
		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mPlugListener != null) mContext.unregisterReceiver(mPlugListener);
	}

	public boolean onKeyDown(int keycode, KeyEvent event) {
		if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
			mKeyHeadset.setBackgroundColor(Color.GREEN);
			mPlayingFile = !mPlayingFile;
			playFile();
			return true;
		}
		return super.onKeyDown(keycode, event);
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;

		initCreate();

		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.headset_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.headset_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.headset_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;

		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}

		releaseDestroy();
		this.finish();
	}

	public class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Handler handler) {
			super();
			mContext = context;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
			while ( (!isFinished) && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				stopAutoTest(false);
			}
		}
	}
}