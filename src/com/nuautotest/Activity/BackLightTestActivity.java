package com.nuautotest.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 背光测试
 *
 * @author xie-hang
 *
 */

public class BackLightTestActivity extends Activity
{
	public TextView t;
	public WindowManager.LayoutParams lp;
	public Thread thread;
	public float j=1;
	private ModuleTestApplication application;
	private FileWriter mLogWriter;

	protected static final int MESSAGE_TEST = 0X101;

	private static final int MSG_TIMEOUT = 0x102;
	private int mTimeout;
	private TimerHandler mTimerHandler;

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case BackLightTestActivity.MESSAGE_TEST:
					t.setText(j/10f+"");
					lp.screenBrightness = j/10f;
					getWindow().setAttributes(lp);
					break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backlight_test);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_backlight.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---BackLight Test---");

		t = (TextView) findViewById(R.id.sensor);
		lp = getWindow().getAttributes();
		lp.screenBrightness = 0.01f;
		getWindow().setAttributes(lp);

		thread = new Thread(new myThread());
		thread.start();
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean mAutomatic = BackLightTestActivity.this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 25;
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

	class myThread implements Runnable {

		public void run() {
			for (int i = 1; i <=10; i++) {
				Message message = new Message();
				message.what = BackLightTestActivity.MESSAGE_TEST;
				j=i;
				boolean msgResult
						= BackLightTestActivity.this.handler.sendMessage(message);
				if (!msgResult)
					postError("In myThread.run():Send MESSAGE_TEST failed");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					postError("In myThread.run():"+e);
				}
			}
		}
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.backlight_test))]="失败";
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.backlight_test))]="成功";
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "BackLightTestActivity"+"======"+error+"======");
		application = ModuleTestApplication.getInstance();
		application.getListViewState()[application.getIndex(getString(R.string.backlight_test))]="失败";
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
			mTimerHandler.sendEmptyMessage(MSG_TIMEOUT);
		}
	}

	protected class TimerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_TIMEOUT) {
				if (ModuleTestApplication.getInstance().getListViewState()
						[ModuleTestApplication.getInstance().getIndex(getString(R.string.backlight_test))].equals("未测试")) {
					ModuleTestApplication.getInstance().getListViewState()
							[ModuleTestApplication.getInstance().getIndex(getString(R.string.backlight_test))] = "操作超时";
					BackLightTestActivity.this.finish();
				}
			}
		}
	}
}