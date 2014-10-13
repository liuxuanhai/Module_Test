package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 耳机测试
 *
 * @author xie-hang
 *
 */

public class HeadsetTestActivity extends Activity
{
	private TextView mHeadsetState, mKeyHeadset;
	private Button mPlayMusic;
	private LinearLayout mllHeadsetKey;
	private BroadcastReceiver mPlugListener;
	private MediaPlayer mPlayer = null;

	boolean mPlayingFile = false;

	private ModuleTestApplication application;
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
				playFile();
				mPlayingFile = !mPlayingFile;
			}
		});
		mllHeadsetKey = (LinearLayout)findViewById(R.id.llHeadsetKey);
		mKeyHeadset = (TextView)findViewById(R.id.keyHeadset);
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
		if (!mPlayingFile) {
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
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.headset_test))] = "失败";
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.headset_test))] = "成功";
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
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
				if (ModuleTestApplication.getInstance().getListViewState()
						[ModuleTestApplication.getInstance().getIndex(getString(R.string.headset_test))].equals("未测试")) {
					ModuleTestApplication.getInstance().getListViewState()
							[ModuleTestApplication.getInstance().getIndex(getString(R.string.headset_test))] = "操作超时";
					HeadsetTestActivity.this.finish();
				}
			}
		}
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_record.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Record Test---");

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
						} else {
							mHeadsetState.setText("三段式耳机已插入");
							mllHeadsetKey.setVisibility(View.INVISIBLE);
						}
					} else {
						if (!isAutomatic) {
							mHeadsetState.setText("未插入");
							mllHeadsetKey.setVisibility(View.INVISIBLE);
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
			playFile();
			mPlayingFile = !mPlayingFile;
			return true;
		}
		return super.onKeyDown(keycode, event);
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;

		initCreate();

		application.getListViewState()[application.getIndex(mContext.getString(R.string.headset_test))]="测试中";
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success)
			application.getListViewState()[application.getIndex(mContext.getString(R.string.headset_test))]="成功";
		else
			application.getListViewState()[application.getIndex(mContext.getString(R.string.headset_test))]="失败";
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

		public AutoTestThread(Context context, Application app, Handler handler) {
			super();
			mContext = context;
			application = (ModuleTestApplication) app;
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