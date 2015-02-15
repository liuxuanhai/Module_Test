package com.nuautotest.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import com.nuautotest.NativeLib.ProcessThread;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * TP测试
 *
 * @author xie-hang
 *
 */

public class TPTestActivity extends Activity {

	public static final String ACTION_TPSUCCESS
			= "com.nuautotest.Activity.TPTestActivity.ACTION_TPSUCCESS";
	public static final String ACTION_TPSHOWBUTTON
			= "com.nuautotest.Activity.TPTestActivity.ACTION_TPSHOWBUTTON";
	private ModuleTestApplication application;
	private BroadcastReceiver mBcrTPTest;
	private Activity self;
	private LinearLayout mLayout;
	private FileWriter mLogWriter;
	private static final int MSG_TIMEOUT = 0x101;
	private boolean mAutomatic;
	private int mTimeout;
	private TimerHandler mTimerHandler;
	private ProcessThread mProcessThread = null;
	private int mPrevPointerLocation, mPrevShowTouches;
	private int mSdkVersion = Build.VERSION.SDK_INT;

	public class TPTestBroadcastReceiver extends BroadcastReceiver {
		private View mBottomView;
		private LayoutParams mParam;
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_TPSUCCESS.equals(intent.getAction())) {
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.tp_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				self.finish();
			} else if (ACTION_TPSHOWBUTTON.equals(intent.getAction())) {
				mBottomView = self.getLayoutInflater().inflate(R.layout.bottom_button, null);
				mParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				mLayout.addView(mBottomView, mParam);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (mSdkVersion >= Build.VERSION_CODES.KITKAT)
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_tp.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---TP Test---");

		self = this;

		if (mSdkVersion < Build.VERSION_CODES.KITKAT) {
			mProcessThread = new ProcessThread((ActivityManager) this.getSystemService(ACTIVITY_SERVICE), this);
			mProcessThread.start();
		}

		setContentView(R.layout.tp_test);
		mLayout = (LinearLayout)this.findViewById(R.id.tpView);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_TPSUCCESS);
		intentFilter.addAction(ACTION_TPSHOWBUTTON);
		mBcrTPTest = new TPTestBroadcastReceiver();
		registerReceiver(mBcrTPTest, intentFilter);

		try {
			mPrevShowTouches = Settings.System.getInt(getContentResolver(), "show_touches");
			mPrevPointerLocation = Settings.System.getInt(getContentResolver(), "pointer_location");
		} catch (Settings.SettingNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		mAutomatic = this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 60;
			mTimerHandler = new TimerHandler();
			TimerThread mTimer = new TimerThread();
			mTimer.start();
		}
		if (mSdkVersion < Build.VERSION_CODES.KITKAT) {
			if (mProcessThread == null)
				mProcessThread = new ProcessThread((ActivityManager) this.getSystemService(ACTIVITY_SERVICE), this);
			try {
				if (!mProcessThread.isAlive())
					mProcessThread.start();
			} catch (IllegalThreadStateException e) {
				e.printStackTrace();
			}
		}

		Settings.System.putInt(getContentResolver(), "show_touches", 1);
		Settings.System.putInt(getContentResolver(), "pointer_location", 1);

		mLayout.postInvalidate();
	}

	@Override
	public void onPause() {
		Settings.System.putInt(getContentResolver(), "show_touches", mPrevShowTouches);
		Settings.System.putInt(this.getContentResolver(), "pointer_location", mPrevPointerLocation);

		if (mSdkVersion < Build.VERSION_CODES.KITKAT)
			mProcessThread.handler.sendEmptyMessage(ProcessThread.MSG_KILLTHREAD);
		super.onPause();
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(mBcrTPTest);
		} catch (IllegalArgumentException e) {
			Log.e(ModuleTestApplication.TAG,"TPTestActivity======In onDestroy():"+e+"======");
		}

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

	@Override
	public void onBackPressed() {
		application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.tp_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		if (mAutomatic) mTimeout = -1;

		super.onBackPressed();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.tp_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.tp_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
		if (mAutomatic) mTimeout = -1;
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
				if (ModuleTestApplication.getInstance().getTestState(getString(R.string.tp_test))
						== ModuleTestApplication.TestState.TEST_STATE_NONE) {
					ModuleTestApplication.getInstance().setTestState(getString(R.string.tp_test),
							ModuleTestApplication.TestState.TEST_STATE_TIME_OUT);
					TPTestActivity.this.finish();
				}
			}
		}
	}
}
