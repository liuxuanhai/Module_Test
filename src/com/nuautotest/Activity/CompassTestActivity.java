package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 指南针测试
 *
 * @author xie-hang
 *
 */

public class CompassTestActivity extends Activity implements
		SensorEventListener {
	private TextView text;

	private boolean mRegisteredMagSensor, mRegisteredAccSensor;

	private SensorManager mSensorManager;
	private Sensor mMagSensor, mAccSensor;

	private final float[] magValue = new float[3];
	private final float[] accValue = new float[3];
	private final float[] rotation = new float[9];
	private final float[] orientation = new float[3];
	private float autoRecOrientation = 0;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.compass_test);
		text = (TextView) findViewById(R.id.tvCompass);
		initCreate();
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_compass.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Compass Test---");

		mRegisteredMagSensor = false;
		mRegisteredAccSensor = false;

		mSensorManager = (SensorManager)mContext.getSystemService(SENSOR_SERVICE);
		if (mSensorManager == null)
			postError("In initCreate():Get SENSOR_SERVICE failed");
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		initResume();
	}

	void initResume() {
		mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mRegisteredMagSensor = mSensorManager.registerListener(this, mMagSensor,
				SensorManager.SENSOR_DELAY_GAME);
		if (!mRegisteredMagSensor)
			postError("In initResume():Register Mag sensor failed");

		mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mRegisteredAccSensor = mSensorManager.registerListener(this, mAccSensor,
				SensorManager.SENSOR_DELAY_GAME);
		if (!mRegisteredAccSensor)
			postError("In initResume():Register Acc sensor failed");
	}

	@Override
	protected void onPause() {
		releasePause();

		super.onPause();
	}

	void releasePause() {
		if (mRegisteredMagSensor) {
			mSensorManager.unregisterListener(this, mMagSensor);
			mRegisteredMagSensor = false;
		}

		if (mRegisteredAccSensor) {
			mSensorManager.unregisterListener(this, mAccSensor);
			mRegisteredAccSensor = false;
		}

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event)
	{
		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				System.arraycopy(event.values, 0, magValue, 0, 3);
			else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
				System.arraycopy(event.values, 0, accValue, 0, 3);
		}

		SensorManager.getRotationMatrix(rotation, null, accValue, magValue);
		SensorManager.getOrientation(rotation, orientation);
		for (int i=0; i<3; i++)
			orientation[i] = (float)Math.toDegrees(orientation[i]);

		if (isAutomatic) {
			if (autoRecOrientation != 0 && autoRecOrientation != orientation[0])
				stopAutoTest(true);
			else
				autoRecOrientation = orientation[0];
		} else {
			text.setText(String.valueOf(orientation[0]));

			Display display = getWindowManager().getDefaultDisplay();
			int deviceRotation = display.getRotation();
			ImageView compassImage = (ImageView)findViewById(R.id.compassimage);
			switch(deviceRotation) {
				case Surface.ROTATION_0:
					compassImage.setRotation(-orientation[0]);
					break;
				case Surface.ROTATION_90:
					compassImage.setRotation(-orientation[0]+270);
					break;
				case Surface.ROTATION_180:
					compassImage.setRotation(-orientation[0]+180);
					break;
				case Surface.ROTATION_270:
					compassImage.setRotation(-orientation[0]+90);
					break;
			}
		}
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "CompassTestActivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		initResume();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.compass_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releasePause();
		this.finish();
	}

	public class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Handler handler) {
			super();
			mContext = context;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
			while ( (!isFinished) && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				Log.e(ModuleTestApplication.TAG, "======Compass Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}