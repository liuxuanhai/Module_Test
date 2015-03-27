package com.nuautotest.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Color;
import android.os.Build;
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
import com.nuautotest.Adapter.NuAutoTestAdapter;
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

public class LCDTestActivity extends Activity implements View.OnSystemUiVisibilityChangeListener {

	static final boolean MANUAL = true;

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
	private int mSdkVersion = Build.VERSION.SDK_INT;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_lcd.txt");
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
		if (mSdkVersion < Build.VERSION_CODES.KITKAT) {
			mProcessThread = new ProcessThread((ActivityManager) this.getSystemService(ACTIVITY_SERVICE), this);
			mProcessThread.start();
		} else {
			setNavVisibility(false);
		}
	}

	@Override
	public void onPause() {
		if (!MANUAL) {
			mTimer.cancel();
			mTask.cancel();
		}
		if (mSdkVersion < Build.VERSION_CODES.KITKAT)
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
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lcd_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.lcdBtSuccess:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lcd_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG,"LCDTestActivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lcd_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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

	Runnable mNavHider = new Runnable() {
		@Override public void run() {
			setNavVisibility(false);
		}
	};

	void setNavVisibility(boolean visible) {
		int newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		if (!visible) {
			newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		// If we are now visible, schedule a timer for us to go invisible.
		if (visible) {
			Handler h = getWindow().getDecorView().getHandler();
			if (h != null) {
				h.removeCallbacks(mNavHider);
				h.postDelayed(mNavHider, 0);
			}
		}

		// Set the new desired visibility.
		getWindow().getDecorView().setSystemUiVisibility(newVis);
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {

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
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.lcd_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lcd_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					LCDTestActivity.this.finish();
				}
			}
		}
	}
}
