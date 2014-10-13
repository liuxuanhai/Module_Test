package com.nuautotest.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
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

	public class TPTestBroadcastReceiver extends BroadcastReceiver {
		private View mBottomView;
		private LayoutParams mParam;
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_TPSUCCESS.equals(intent.getAction())) {
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.tp_test))] = "成功";
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

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_tp.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---TP Test---");

		self = this;

		mProcessThread = new ProcessThread((ActivityManager)this.getSystemService(ACTIVITY_SERVICE), this);
		mProcessThread.start();

		setContentView(R.layout.tp_test);
		mLayout = (LinearLayout)this.findViewById(R.id.tpView);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_TPSUCCESS);
		intentFilter.addAction(ACTION_TPSHOWBUTTON);
		mBcrTPTest = new TPTestBroadcastReceiver();
		registerReceiver(mBcrTPTest, intentFilter);
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
		if (mProcessThread == null)
			mProcessThread = new ProcessThread((ActivityManager)this.getSystemService(ACTIVITY_SERVICE), this);
		if (!mProcessThread.isAlive())
			mProcessThread.start();
		mLayout.postInvalidate();
	}

	@Override
	public void onPause() {
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
		application.getListViewState()[application.getIndex(getString(R.string.tp_test))] = "失败";
		if (mAutomatic) mTimeout = -1;

		super.onBackPressed();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.tp_test))] = "失败";
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.tp_test))] = "成功";
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
				if (ModuleTestApplication.getInstance().getListViewState()
						[ModuleTestApplication.getInstance().getIndex(getString(R.string.tp_test))].equals("未测试")) {
					ModuleTestApplication.getInstance().getListViewState()
							[ModuleTestApplication.getInstance().getIndex(getString(R.string.tp_test))] = "操作超时";
					TPTestActivity.this.finish();
				}
			}
		}
	}
}
