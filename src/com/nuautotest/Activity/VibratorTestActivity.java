package com.nuautotest.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Vibrator测试
 *
 * @author xie-hang
 *
 */

public class VibratorTestActivity extends Activity {
	private ModuleTestApplication application;
	private Vibrator mVibrator;
	private FileWriter mLogWriter;
	private static final int MSG_TIMEOUT = 0x101;
	private boolean mAutomatic;
	private int mTimeout;
	private TimerHandler mTimerHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_vibrator.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Vibrator Test---");

		setContentView(R.layout.vibrator_test);

		mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		if (mVibrator == null)
			postError("In onCreate():Get VIBRATOR_SERVICE failed");
		if (!mVibrator.hasVibrator()) {
			application = ModuleTestApplication.getInstance();
			application.setTestState(getString(R.string.vibrator_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
			this.finish();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mAutomatic = this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 20;
			mTimerHandler = new TimerHandler();
			TimerThread mTimer = new TimerThread();
			mTimer.start();
		}
	}

	@Override
	public void onDestroy() {
		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.onDestroy();
	}

	public void onVibrate(View view) {
		mVibrator.vibrate(1000);
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.vibrator_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.vibrator_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
		if (mAutomatic) mTimeout = -1;
	}

	@Override
	public void onBackPressed() {

	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "VibratorTestActivity"+"======"+error+"======");
		application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.vibrator_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
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
			if (mTimeout == 0) mTimerHandler.sendEmptyMessage(MSG_TIMEOUT);
		}
	}

	protected class TimerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_TIMEOUT) {
				if (ModuleTestApplication.getInstance().getTestState(getString(R.string.vibrator_test))
						== ModuleTestApplication.TestState.TEST_STATE_NONE) {
					ModuleTestApplication.getInstance().setTestState(getString(R.string.vibrator_test),
							ModuleTestApplication.TestState.TEST_STATE_TIME_OUT);
					VibratorTestActivity.this.finish();
				}
			}
		}
	}
}
