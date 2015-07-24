package com.nuautotest.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * HDMI测试
 *
 * @author xie-hang
 *
 */

public class HDMITestActivity extends Activity
{
	private FileWriter mLogWriter;
	private static final int MSG_TIMEOUT = 0x101;
	private int mTimeout;
	private TimerHandler mTimerHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hdmi_test);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_hdmi.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---HDMI Test---");
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean mAutomatic = this.getIntent().getBooleanExtra("Auto", false);
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


	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.hdmi_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.hdmi_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	private class TimerThread extends Thread {
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

	private class TimerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_TIMEOUT) {
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.hdmi_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.hdmi_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					HDMITestActivity.this.finish();
				}
			}
		}
	}
}