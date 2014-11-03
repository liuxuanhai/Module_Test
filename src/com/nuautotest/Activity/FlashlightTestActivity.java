package com.nuautotest.Activity;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 闪光灯测试
 *
 * @author xie-hang
 *
 */

public class FlashlightTestActivity extends Activity {
	private Button mbtFlashlight;
	private ModuleTestApplication application;
	private Camera mCamera;
	private Camera.ShutterCallback mShutter;
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
					mLogWriter = new FileWriter("/sdcard/ModuleTest/log_vibrator.txt");
				} catch (IOException e) {
					e.printStackTrace();
				}
				ModuleTestApplication.getInstance().recordLog(null);
			}
			Log.i(ModuleTestApplication.TAG, "---Flashlight Test---");

			setContentView(R.layout.flashlight_test);
			mbtFlashlight = (Button)findViewById(R.id.btFlashlight);

			isCaptureFinished = true;
			mShutter = new Camera.ShutterCallback() {
				@Override
				public void onShutter() {
					isCaptureFinished = true;
					mbtFlashlight.setEnabled(true);
				}
			};
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
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(parameters);
		}
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

	protected void turnFlashlight() {
		if (mCamera == null) return;
		if (!isCaptureFinished) return;

//		isCaptureFinished = false;
//		mbtFlashlight.setEnabled(false);

		Camera.Parameters parameters = mCamera.getParameters();
		List<String> flashModes = parameters.getSupportedFlashModes();
		if (flashModes != null) {
			for (String flashMode1 : flashModes) Log.i(ModuleTestApplication.TAG, flashMode1);
		}
		// Check if camera flash exists
		String flashMode = parameters.getFlashMode();
		if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
			// Turn on the flash
			if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(parameters);
				mbtFlashlight.setText("关闭");
			}
		} else {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mCamera.setParameters(parameters);
			mbtFlashlight.setText("打开");
		}
//		mCamera.takePicture(mShutter, null, null);
	}

	public void onFlashlight(View view) {
		turnFlashlight();
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.flashlight_test))] = "失败";
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.flashlight_test))] = "成功";
				this.finish();
				break;
		}
		if (mAutomatic) mTimeout = -1;
	}

	@Override
	public void onBackPressed() {

	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "FlashlightTestActivity"+"======"+error+"======");
		application = ModuleTestApplication.getInstance();
		application.getListViewState()[application.getIndex(getString(R.string.flashlight_test))] = "失败";
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
				if (ModuleTestApplication.getInstance().getListViewState()
						[ModuleTestApplication.getInstance().getIndex(getString(R.string.flashlight_test))].equals("未测试")) {
					ModuleTestApplication.getInstance().getListViewState()
							[ModuleTestApplication.getInstance().getIndex(getString(R.string.flashlight_test))] = "操作超时";
					FlashlightTestActivity.this.finish();
				}
			}
		}
	}
}
