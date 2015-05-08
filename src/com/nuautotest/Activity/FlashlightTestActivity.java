package com.nuautotest.Activity;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.NativeLib.RootCommand;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;
import java.util.List;

/**
 * 闪光灯测试
 *
 * @author xie-hang
 *
 */

public class FlashlightTestActivity extends Activity {
	private Button mbtFlashlight;
	private Camera mCamera;
	private boolean isCaptureFinished;
	private FileWriter mLogWriter;
	private static final int MSG_TIMEOUT = 0x101;
	private boolean mAutomatic;
	private int mTimeout;
	private TimerHandler mTimerHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			if (ModuleTestApplication.LOG_ENABLE) {
				try {
					mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_flashlight.txt");
				} catch (IOException e) {
					e.printStackTrace();
				}
				ModuleTestApplication.getInstance().recordLog(null);
			}
			Log.i(ModuleTestApplication.TAG, "---Flashlight Test---");

			setContentView(R.layout.flashlight_test);
			mbtFlashlight = (Button)findViewById(R.id.btFlashlight);

			isCaptureFinished = true;
			mCamera = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
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
	public void onPause() {
		turnFlashlight(true);

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
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
		super.onDestroy();
	}

	protected void turnFlashlight(boolean off) {
		RootCommand rootcmd = ModuleTestApplication.getRootcmd();
		if (rootcmd.isEnabled()) {
			try {
				rootcmd.Write("echo 46 > /sys/class/gpio/export\n");
				/* Read current state */
				rootcmd.Write("cat /sys/class/gpio/gpio46/value\n");
				if (off || rootcmd.ReadLine().contains("1")) {
					rootcmd.Write("echo 0 > /sys/class/gpio/gpio46/value\n");
					rootcmd.Write("echo 46 > /sys/class/gpio/unexport\n");
					rootcmd.Write("echo 47 > /sys/class/gpio/unexport\n");
					mbtFlashlight.setText("打开");
				} else {
					rootcmd.Write("echo 47 > /sys/class/gpio/export\n");
					rootcmd.Write("echo out > /sys/class/gpio/gpio47/direction\n");
					rootcmd.Write("echo 0 > /sys/class/gpio/gpio47/value\n");
					rootcmd.Write("echo out > /sys/class/gpio/gpio46/direction\n");
					rootcmd.Write("echo 1 > /sys/class/gpio/gpio46/value\n");
					mbtFlashlight.setText("关闭");
				}
			} catch (Exception e) {
				Log.w(ModuleTestApplication.TAG, "Flashlight Test: get root failed");
			}
		}
		if (!rootcmd.isEnabled()) {
			if (mCamera == null) return;
			if (!isCaptureFinished) return;

			Camera.Parameters parameters = mCamera.getParameters();
			List<String> flashModes = parameters.getSupportedFlashModes();
			if (flashModes != null) {
				for (String flashMode1 : flashModes) Log.i(ModuleTestApplication.TAG, flashMode1);
			}
			// Check if camera flash exists
			String flashMode = parameters.getFlashMode();
			if (off || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				mCamera.setParameters(parameters);
				mbtFlashlight.setText("打开");
			} else {
				// Turn on the flash
				if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					mCamera.setParameters(parameters);
					mbtFlashlight.setText("关闭");
				}
			}
		}
	}

	public void onFlashlight(View view) {
		turnFlashlight(false);
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.flashlight_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.flashlight_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "FlashlightTestActivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.flashlight_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
				if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.flashlight_test))
						== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.flashlight_test),
							NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
					FlashlightTestActivity.this.finish();
				}
			}
		}
	}
}
