package com.nuautotest.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

/**
 * 收音机测试
 *
 * @author xie-hang
 *
 */

public class FMTestActivity extends Activity {
	private static final int MSG_TIMEOUT = 0x101;
	private static final String PACKAGE_FM = "com.broadcom.bt.app.fm";

	private boolean mAutomatic;
	private int mTimeout;
	private TimerHandler mTimerHandler;
	private Intent mIntentFM;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fm_test);

		PackageManager pm = getPackageManager();
		if (pm != null) {
			mIntentFM = pm.getLaunchIntentForPackage(PACKAGE_FM);
			if (mIntentFM == null) {
				Log.e(ModuleTestApplication.TAG, "getLaunchIntentForPackage for fm failed");
				this.finish();
			}
		}
	}

	@Override
	public void onDestroy() {
		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		am.killBackgroundProcesses(PACKAGE_FM);
		super.onDestroy();
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

	public void onFM(View view) {
		mIntentFM.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(mIntentFM);
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.fm_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.fm_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
		if (mAutomatic) mTimeout = -1;
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
			if (mTimeout == 0) mTimerHandler.sendEmptyMessage(MSG_TIMEOUT);
		}
	}

	private class TimerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_TIMEOUT) {
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.fm_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.fm_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					FMTestActivity.this.finish();
				}
			}
		}
	}
}