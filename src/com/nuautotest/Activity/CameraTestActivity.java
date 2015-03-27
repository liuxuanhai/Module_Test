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
import android.widget.Button;
import android.widget.LinearLayout;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.NativeLib.RootCommand;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;
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
	private SurfaceHolder mSurfaceHolder;
	private Camera camera;
	private String Flag = "";
	DisplayMetrics outMetrics;
	private FileWriter mLogWriter;
	private SurfaceCallback mSurfaceCallback;
	private CameraDisplayListener mDisplayListener;
	private int mTimeout;
	private TimerHandler mTimerHandler;
	public static int mRotationFront, mRotationBack;

	private Button mBtTakePicture;
	private static final int STATE_IDLE = 0;
	private static final int STATE_TAKING = 1;
	private static final int STATE_TAKEN = 2;
	private int state = STATE_IDLE;

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
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_camera.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Camera Test---");

		setContentView(R.layout.camera_test);
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		Intent intent = this.getIntent();
		Flag = intent.getStringExtra("Flag");
		if (Flag.equals("Front")) {
			this.setTitle(R.string.front_camera_test);
			checkExistance();
		} else
			this.setTitle(R.string.back_camera_test);

		mBtTakePicture = (Button) this.findViewById(R.id.btTakePicture);
		surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
		mSurfaceHolder = surfaceView.getHolder();
		if (mSurfaceHolder == null) {
			Log.e(ModuleTestApplication.TAG, "SurfaceHolder is null");
			this.finish();
		}
//		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHolder.setKeepScreenOn(true);
		mSurfaceCallback = new SurfaceCallback();

		DisplayManager dm = (DisplayManager)getSystemService(DISPLAY_SERVICE);
		if (dm == null) postError("In onCreate: get DISPLAY_SERVICE failed");
		mDisplayListener = new CameraDisplayListener();
		dm.registerDisplayListener(mDisplayListener, null);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSurfaceHolder.addCallback(mSurfaceCallback);
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
		mSurfaceHolder.removeCallback(mSurfaceCallback);

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

				List<Camera.Size> pSizes = parameters.getSupportedPreviewSizes();
				if (pSizes == null) {
					Log.e(ModuleTestApplication.TAG, "getSupportedPreviewSizes returned null");
					return;
				}
				Camera.Size pSize;
				Log.d(ModuleTestApplication.TAG, "Supported Preview sizes:");
				for (Camera.Size pSize1 : pSizes) {
					pSize = pSize1;
					Log.d(ModuleTestApplication.TAG, "(" + pSize.width + "x" + pSize.height + ")");
				}
				pSize = pSizes.get(0);

				parameters.setPreviewSize(pSize.width, pSize.height);

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

				boolean swap_wh = false;

				if (Flag.equals("Front")) {
					camera.setDisplayOrientation((display.getRotation() * 90 + mRotationFront) % 360);
					if ((display.getRotation() * 90 + mRotationFront) % 360 == 90 ||
						(display.getRotation() * 90 + mRotationFront) % 360 == 270) swap_wh = true;
				} else {
					camera.setDisplayOrientation((display.getRotation() * 90 + mRotationBack) % 360);
					if ((display.getRotation() * 90 + mRotationBack) % 360 == 90 ||
							(display.getRotation() * 90 + mRotationBack) % 360 == 270) swap_wh = true;
				}

				LinearLayout parent = (LinearLayout)surfaceView.getParent();
				if (parent != null) {
					if (swap_wh) {
						if (pSize.height / pSize.width < parent.getWidth() / parent.getHeight())
							mSurfaceHolder.setFixedSize(parent.getHeight() * pSize.height / pSize.width,
									parent.getHeight());
						else
							mSurfaceHolder.setFixedSize(parent.getWidth(),
									parent.getWidth() * pSize.width / pSize.height);
					} else {
						if (pSize.width / pSize.height < parent.getWidth() / parent.getHeight())
							mSurfaceHolder.setFixedSize(parent.getHeight() * pSize.width / pSize.height,
									parent.getHeight());
						else
							mSurfaceHolder.setFixedSize(parent.getWidth(),
									parent.getWidth() * pSize.height / pSize.width);
					}
				}

				try {
					camera.startPreview();
				} catch (Exception ignored) {}
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		}

	}

	public class CameraFocusCallback implements Camera.AutoFocusCallback {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			camera.takePicture(null, null, null);
			mBtTakePicture.setText("继续预览");
			mBtTakePicture.setEnabled(true);
			state = STATE_TAKEN;
		}
	}

	public void onClickTakePicture(View view) {
		if (camera == null) {
			Log.e(ModuleTestApplication.TAG, "onClickTakePicture: camera = null");
			return;
		}
		switch (state) {
			case STATE_IDLE:
				mBtTakePicture.setText("正在拍照...");
				mBtTakePicture.setEnabled(false);
				state = STATE_TAKING;
				camera.autoFocus(new CameraFocusCallback());
				break;
			case STATE_TAKING:
				break;
			case STATE_TAKEN:
				mBtTakePicture.setText("拍照");
				state = STATE_IDLE;
				camera.startPreview();
				break;
		}
	}

	protected void checkExistance() {
		RootCommand rootcmd = ModuleTestApplication.getRootcmd();
		if (!rootcmd.isEnabled()) return;

		boolean exist0 = false, exist1 = false;
		File f;
		f = new File("/dev/video0");
		if (f.exists()) exist0 = true;
		f = new File("/dev/video1");
		if (f.exists()) exist1 = true;
		if (exist0 && !exist1) rootcmd.Write("ln -s /dev/video0 /dev/video1\n");
	}

	// 成功失败按钮
	public void onbackbtn(View view) {

		switch (view.getId()) {
			case R.id.fail:
				if (Flag.equals("Front")) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.front_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				} else {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.back_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				}
				this.finish();
				break;
			case R.id.success:
				if (Flag.equals("Front")) {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.front_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				} else {
					NuAutoTestAdapter.getInstance().setTestState(getString(R.string.back_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				}
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
		Log.e(ModuleTestApplication.TAG, "CameraTestActivity"+"======"+error+"======");
		if (Flag.equals("Front")) {
			NuAutoTestAdapter.getInstance().setTestState(getString(R.string.front_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		} else {
			NuAutoTestAdapter.getInstance().setTestState(getString(R.string.back_camera_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
					if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.front_camera_test))
							== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
						NuAutoTestAdapter.getInstance().setTestState(getString(R.string.front_camera_test),
								NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
						CameraTestActivity.this.finish();
					}
				} else {
					if (NuAutoTestAdapter.getInstance().getTestState(getString(R.string.back_camera_test))
							== NuAutoTestAdapter.TestState.TEST_STATE_NONE) {
						NuAutoTestAdapter.getInstance().setTestState(getString(R.string.back_camera_test),
								NuAutoTestAdapter.TestState.TEST_STATE_TIME_OUT);
						CameraTestActivity.this.finish();
					}
				}
			}
		}
	}
}