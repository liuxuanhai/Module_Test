package com.nuautotest.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import com.nuautotest.NativeLib.ProcessThread;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LCD测试
 *
 * @author xie-hang
 *
 */

public class LCDTestActivity extends Activity {

	static final boolean MANUAL = true;

	private ModuleTestApplication application;
	private LinearLayout mllLcd;
	private Button mlcdBtSuccess, mlcdBtFail;
	private Timer mTimer;
	private TimerTask mTask;
	private int mCurrentColor;
	private Handler mHandler;
	private FileWriter mLogWriter;
	static final int MSG_CHANGEBG = 0x101;
	static final int MSG_TIMEOUT = 0x102;
	private int mTimeout;
	private TimerHandler mTimerHandler;
	private ProcessThread mProcessThread;
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_lcd.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---LCD Test---");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.lcd_test);
		mllLcd = (LinearLayout)findViewById(R.id.llLCD);
		mCurrentColor = Color.RED;
		mllLcd.setBackgroundColor(mCurrentColor);
		mlcdBtSuccess = (Button)this.findViewById(R.id.lcdBtSuccess);
		mlcdBtFail = (Button)this.findViewById(R.id.lcdBtFail);

		mContext = this;

		mHandler = new ChangeBGHandler();
		if (MANUAL) {
			mllLcd.setOnClickListener(new OnLcdClickListener());
		} else {
			mTimer = new Timer();
			mTask = new ChangeBGTask();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!MANUAL) {
			try {
				mTimer.schedule(mTask, 1000, 1000);
			} catch (IllegalStateException e) {
				postError("In onResume():"+e);
			}
		}
		boolean mAutomatic = this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 20;
			mTimerHandler = new TimerHandler();
			TimerThread mAutoTimer = new TimerThread();
			mAutoTimer.start();
		}
		mProcessThread = new ProcessThread((ActivityManager)this.getSystemService(ACTIVITY_SERVICE), this);
		mProcessThread.start();
	}

	@Override
	public void onPause() {
		if (!MANUAL) {
			mTimer.cancel();
			mTask.cancel();
		}
		mProcessThread.handler.sendEmptyMessage(ProcessThread.MSG_KILLTHREAD);
		super.onPause();
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

	public void onbackbtn(View view) {

		switch (view.getId()) {
			case R.id.lcdBtFail:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.lcd_test))]="失败";
				this.finish();
				break;
			case R.id.lcdBtSuccess:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.lcd_test))]="成功";
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG,"LCDTestActivity"+"======"+error+"======");
		application= ModuleTestApplication.getInstance();
		application.getListViewState()[application.getIndex(getString(R.string.lcd_test))]="失败";
		this.finish();
	}

	public void changeColor() {
		switch(mCurrentColor) {
			case Color.RED:
				mCurrentColor = Color.GREEN;
				break;
			case Color.GREEN:
				mCurrentColor = Color.BLUE;
				break;
			case Color.BLUE:
				mCurrentColor = Color.WHITE;
				break;
			case Color.WHITE:
				mCurrentColor = Color.BLACK;
				mlcdBtSuccess.setVisibility(View.VISIBLE);
				mlcdBtFail.setVisibility(View.VISIBLE);
				break;
			case Color.BLACK:
				mCurrentColor = Color.RED;
				break;
		}
	}

	public class ChangeBGHandler extends Handler {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_CHANGEBG)
				changeColor();
			mllLcd.setBackgroundColor(mCurrentColor);
		}
	}

	public class ChangeBGTask extends TimerTask {
		@Override
		public void run() {
			mHandler.sendEmptyMessage(MSG_CHANGEBG);
		}
	}

	public class OnLcdClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			mHandler.sendEmptyMessage(MSG_CHANGEBG);
			final String ACTION_XIEHANG_TEST = "com.nufront.xiehang.test";
			Intent intent = new Intent(ACTION_XIEHANG_TEST);
			mContext.sendBroadcast(intent);
		}
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
						[ModuleTestApplication.getInstance().getIndex(getString(R.string.lcd_test))].equals("未测试")) {
					ModuleTestApplication.getInstance().getListViewState()
							[ModuleTestApplication.getInstance().getIndex(getString(R.string.lcd_test))] = "操作超时";
					LCDTestActivity.this.finish();
				}
			}
		}
	}
}
