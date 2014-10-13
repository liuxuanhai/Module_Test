package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.SurfaceHolder.Callback;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 前後置拍照
 *
 * @author xie-hang
 *
 */

public class CameraTestActivity extends Activity {
	public static final int MSG_TIMEOUT = 0x101;
	private SurfaceView surfaceView;
	private Camera camera;
	private String Flag = "";
	private ModuleTestApplication application;
	DisplayMetrics outMetrics;
	private FileWriter mLogWriter;
	private SurfaceCallback mSurfaceCallback;
	private CameraDisplayListener mDisplayListener;
	private int mTimeout;
	private TimerHandler mTimerHandler;
	public static int mRotationFront, mRotationBack;

	private class CameraDisplayListener implements DisplayManager.DisplayListener {
		@Override
		public void onDisplayAdded(int displayId) {}

		@Override
		public void onDisplayChanged(int displayId) {
			surfaceView.setVisibility(View.INVISIBLE);
			surfaceView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onDisplayRemoved(int displayId) {}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_camera.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Camera Test---");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_test);
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		Intent intent = this.getIntent();
		Flag = intent.getStringExtra("Flag");
		surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.getHolder().setKeepScreenOn(true);
		mSurfaceCallback = new SurfaceCallback();

		DisplayManager dm = (DisplayManager)getSystemService(DISPLAY_SERVICE);
		if (dm == null) postError("In onCreate: get DISPLAY_SERVICE failed");
		mDisplayListener = new CameraDisplayListener();
		dm.registerDisplayListener(mDisplayListener, null);
	}

	@Override
	public void onResume() {
		super.onResume();
		surfaceView.getHolder().addCallback(mSurfaceCallback);
		boolean mAutomatic = this.getIntent().getBooleanExtra("Auto", false);
		if (mAutomatic) {
			mTimeout = 20;
			mTimerHandler = new TimerHandler();
			TimerThread mTimer = new TimerThread();
			mTimer.start();
		}
	}

	@Override
	public void onPause() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
		surfaceView.getHolder().removeCallback(mSurfaceCallback);

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
		DisplayManager dm = (DisplayManager)getSystemService(DISPLAY_SERVICE);
		if (dm != null) dm.unregisterDisplayListener(mDisplayListener);

		super.onDestroy();
	}

	private final class SurfaceCallback implements Callback {
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
					CameraInfo info = new CameraInfo();
					Camera.getCameraInfo(i, info);
					if (info.facing == CameraInfo.CAMERA_FACING_FRONT
							&& Flag.equals("Front")) {
						try {
							camera = Camera.open(i);
						} catch (RuntimeException e) {
							postError("In surfaceCreated():"+e);
						}
					}

					if (info.facing == CameraInfo.CAMERA_FACING_BACK
							&& Flag.equals("Back")) {
						try {
							camera = Camera.open(i);
						} catch (RuntimeException e) {
							postError("In surfaceCreated():"+e);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.i(ModuleTestApplication.TAG, "surfaceChanged");

			if (camera != null) {
				Camera.Parameters parameters = camera.getParameters();

				List<Camera.Size> pSizes=parameters.getSupportedPreviewSizes();
				Camera.Size pSize=pSizes.get((pSizes.size() -1)/2);

				parameters.setPreviewSize(pSize.width,pSize.height);

				try {
					camera.setParameters(parameters);
					camera.setPreviewDisplay(surfaceView.getHolder());
				} catch (RuntimeException e) {
					postError("In surfaceCreated():"+e);
				} catch (IOException e) {
					postError("In surfaceCreated():"+e);
				}

				WindowManager windowManager
						= (WindowManager)getSystemService(WINDOW_SERVICE);
				if (windowManager == null)
					postError("In surfaceCreated():Get WINDOW_SERVICE failed");
				Display display = windowManager.getDefaultDisplay();

				Log.d(ModuleTestApplication.TAG, "Current rotation: " + display.getRotation() +
						"config: " + mRotationFront + " " + mRotationBack);

				if (Flag.equals("Front"))
					camera.setDisplayOrientation((display.getRotation()*90 + mRotationFront)%360);
				else
					camera.setDisplayOrientation((display.getRotation()*90 + mRotationBack)%360);

				camera.startPreview();
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		}

	}

	// 成功失败按钮
	public void onbackbtn(View view) {

		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				if (Flag.equals("Front")) {
					application.getListViewState()[application.getIndex(getString(R.string.front_camera_test))] = "失败";
				} else {
					application.getListViewState()[application.getIndex(getString(R.string.back_camera_test))] = "失败";
				}
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				if (Flag.equals("Front")) {
					application.getListViewState()[application.getIndex(getString(R.string.front_camera_test))] = "成功";
				} else {
					application.getListViewState()[application.getIndex(getString(R.string.back_camera_test))] = "成功";
				}
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "CameraTestActivity"+"======"+error+"======");
		application = ModuleTestApplication.getInstance();
		if (Flag.equals("Front")) {
			application.getListViewState()[application.getIndex(getString(R.string.front_camera_test))] = "失败";
		} else {
			application.getListViewState()[application.getIndex(getString(R.string.back_camera_test))] = "失败";
		}
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
				if (Flag.equals("Front")) {
					if (ModuleTestApplication.getInstance().getListViewState()[ModuleTestApplication.getInstance().
							getIndex(getString(R.string.front_camera_test))].equals("未测试")) {
						ModuleTestApplication.getInstance().getListViewState()[ModuleTestApplication.getInstance().
								getIndex(getString(R.string.front_camera_test))] = "操作超时";
						CameraTestActivity.this.finish();
					}
				} else {
					if (ModuleTestApplication.getInstance().getListViewState()[ModuleTestApplication.getInstance().
							getIndex(getString(R.string.back_camera_test))].equals("未测试")) {
						ModuleTestApplication.getInstance().getListViewState()[ModuleTestApplication.getInstance().
								getIndex(getString(R.string.back_camera_test))] = "操作超时";
						CameraTestActivity.this.finish();
					}
				}
			}
		}
	}
}