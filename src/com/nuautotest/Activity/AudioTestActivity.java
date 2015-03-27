package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 音频测试
 *
 * @author xie-hang
 *
 */

public class AudioTestActivity extends Activity {
	private TextView text;
	private Button startButton, stopButton, playButton, stopPlayButton, playFileButton;
	private static String mRecordName = null;

	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;

	boolean mStartRecording = false;
	boolean mStopRecording = false;
	boolean mStartPlaying = false;
	boolean mStopPlaying = false;
	boolean mPlayingFile = false;

	protected static final int START = 0x101;
	protected static final int STOP = 0x102;
	protected static final int PLAY = 0x103;
	protected static final int STOPPLAY = 0x104;
	protected static final int PLAYFILE = 0x105;

	public Thread thread;

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
				case AudioTestActivity.START:
					onRecord();
					break;
				case AudioTestActivity.STOP:
					stopRecord();
					break;
				case AudioTestActivity.PLAY:
					onPlay();
					break;
				case AudioTestActivity.STOPPLAY:
					stopPlay();
					break;
				case AudioTestActivity.PLAYFILE:
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
		setContentView(R.layout.audio_test);
		initCreate();
		InitPathRecord();
		text = (TextView) findViewById(R.id.sound);
		startButton = (Button) findViewById(R.id.start);
		stopButton = (Button) findViewById(R.id.stop);
		playButton = (Button) findViewById(R.id.play);
		stopPlayButton = (Button) findViewById(R.id.stopPlay);
		playFileButton = (Button) findViewById(R.id.playFile);

		stopButton.setVisibility(View.INVISIBLE);
		playButton.setVisibility(View.INVISIBLE);
		stopPlayButton.setVisibility(View.INVISIBLE);

		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!mStartRecording) {
					mStopRecording = false;
					onRecord();
					mStartRecording = true;
				}
			}
		});

		stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mStartRecording) {
					mStartRecording = false;
					stopRecord();
					mStopRecording = true;
				}
			}
		});

		playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mStopRecording) {
					mStopPlaying = false;
					onPlay();
					mStartPlaying = true;
				}
			}
		});

		stopPlayButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mStartPlaying) {
					mStartPlaying = false;
					stopPlay();
					mStopPlaying = true;
				}
			}
		});

		playFileButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				playFile();
				mPlayingFile = !mPlayingFile;
			}
		});

//		thread = new Thread(new myThread());
//		thread.start();
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean mAutomatic = AudioTestActivity.this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 30;
			mTimerHandler = new TimerHandler();
			TimerThread mTimer = new TimerThread();
			mTimer.start();
		}
	}

//	class myThread implements Runnable {
//
//		public void run() {
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			// 开始录音
//			if (!mStartRecording && !mStopPlaying) {
//				mStartRecording = true;
//				Message message = new Message();
//				message.what = AudioTestActivity.START;
//				AudioTestActivity.this.handler.sendMessage(message);
//			}
//
//			try {
//				Thread.sleep(3000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			// 停止录音
//			if (!mStopRecording && mStartRecording) {
//				mStopRecording = true;
//				Message message = new Message();
//				message.what = AudioTestActivity.STOP;
//				AudioTestActivity.this.handler.sendMessage(message);
//			}
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			// 开始播放
//			if (!mStartPlaying && mStopRecording) {
//				mStartPlaying = true;
//				Message message = new Message();
//				message.what = AudioTestActivity.PLAY;
//				AudioTestActivity.this.handler.sendMessage(message);
//			}
//
//			try {
//				Thread.sleep(3000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//			// 停止播放
//			if (!mStopPlaying && mStartPlaying) {
//				mStopPlaying = true;
//				Message message = new Message();
//				message.what = AudioTestActivity.STOPPLAY;
//				AudioTestActivity.this.handler.sendMessage(message);
//			}
//		}
//	}

	public void InitPathRecord() {
		mRecordName = ModuleTestApplication.LOG_DIR + "/audiorecordtest.3gp";
		System.out.println(mRecordName);
	}

	private void onRecord() {
		startRecording();
	}

	private void stopRecord() {
		stopRecording();
	}

	private void onPlay() {
		startPlaying();
	}

	private void stopPlay() {
		stopPlaying();
	}

	private void startPlaying() {
		try {
			mPlayer = new MediaPlayer();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setDataSource(mRecordName);
			mPlayer.prepare();
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

		text.setText("开始播放...");
		stopPlayButton.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.INVISIBLE);
		startButton.setVisibility(View.INVISIBLE);
		stopButton.setVisibility(View.INVISIBLE);
		playFileButton.setVisibility(View.INVISIBLE);
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}

		text.setText("播放完毕");
		startButton.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.VISIBLE);
		stopButton.setVisibility(View.INVISIBLE);
		stopPlayButton.setVisibility(View.INVISIBLE);
		playFileButton.setVisibility(View.VISIBLE);
		mStartRecording=false;
	}

	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mRecordName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(ModuleTestApplication.TAG, "prepare() failed");
		}
		mRecorder.start();

		text.setText("开始录制...");
		stopButton.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.INVISIBLE);
		startButton.setVisibility(View.INVISIBLE);
		stopPlayButton.setVisibility(View.INVISIBLE);
	}

	private void stopRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}

		text.setText("已停止录制");
		stopButton.setVisibility(View.INVISIBLE);
		startButton.setVisibility(View.VISIBLE);
		if (!mPlayingFile) playButton.setVisibility(View.VISIBLE);
		stopPlayButton.setVisibility(View.INVISIBLE);
	}

	private void playFile() {
		if (!mPlayingFile) {
			mPlayer = MediaPlayer.create(this, R.raw.audiofiletest);
			mPlayer.start();

			text.setText("开始播放文件...");
			playFileButton.setText("停止播放文件");
			playButton.setVisibility(View.INVISIBLE);
		} else {
			if (mPlayer != null) {
				mPlayer.release();
				mPlayer = null;
			}

			text.setText("播放文件完毕");
			playFileButton.setText("播放音频文件");
			if (mStopRecording) playButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPause() {
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
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
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.audio_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.audio_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.audio_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.audio_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					AudioTestActivity.this.finish();
				}
			}
		}
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_record.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Record Test---");
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
	}

	public void startAutoTest() {
		isFinished = false;

		initCreate();

		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.audio_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		mPlayer = new MediaPlayer();
		mPlayer = MediaPlayer.create(mContext, R.raw.audiofiletest);
		if (mPlayer == null) stopAutoTest(false);
		mPlayer.start();
	}

	public void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.audio_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.audio_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
			while ( (!isFinished) && (time<2000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 2000) {
				stopAutoTest(false);
			}
		}
	}
}