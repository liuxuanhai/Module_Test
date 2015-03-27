package com.nuautotest.Activity;

import android.app.ActionBar;
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
 * 加速传感器
 *
 * @author xie-hang
 *
 */

public class GSensorTestActivity extends Activity implements
		SensorEventListener
{
	TextView text;
	ImageView image;
	private boolean mRegisteredSensor;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;
	private SensorManager mSensorManager;
	private float autoRecX=0, autoRecY=0, autoRecZ=0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.gsensor_test);
		text = (TextView) findViewById(R.id.tvSpeedSensor);
		image = (ImageView)findViewById(R.id.ivSpeedSensor);
		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_gsensor.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---G Sensor Test---");

		mRegisteredSensor = false;
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

	protected void initResume() {
		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mRegisteredSensor = mSensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		if (!mRegisteredSensor)
			postError("In initResume():registerListener failed");
	}

	@Override
	protected void onPause() {
		releasePause();

		super.onPause();
	}

	protected void releasePause() {
		if (mRegisteredSensor) {
			mSensorManager.unregisterListener(this);
			mRegisteredSensor = false;
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
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				float dataX = event.values[SensorManager.DATA_X];
				float dataY = event.values[SensorManager.DATA_Y];
				float dataZ = event.values[SensorManager.DATA_Z];
				if (isAutomatic) {
					if (autoRecX + autoRecY + autoRecZ != 0 &&
						autoRecX != dataX && autoRecY != dataY && autoRecZ != dataZ)
						stopAutoTest(true);
					autoRecX = dataX;
					autoRecY = dataY;
					autoRecZ = dataZ;
				} else {
					text.setText("数据：\nX: " + dataX);
					text.append("\nY: " + dataY);
					text.append("\nZ: " + dataZ);
					if (dataY != 0) {
						float rotation = (float) Math.toDegrees(Math.atan(dataX / dataY));
						if (dataY <0) rotation += 180;
						Display display = this.getWindowManager().getDefaultDisplay();
						switch (display.getRotation()) {
							case Surface.ROTATION_0:
								break;
							case Surface.ROTATION_90:
								rotation += 270;
								break;
							case Surface.ROTATION_180:
								rotation += 180;
								break;
							case Surface.ROTATION_270:
								rotation += 90;
								break;
						}
						image.setRotation(rotation);
					}
				}
			}
		}
	}

	// 成功失败按钮
	public void onbackbtn(View view) {

		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "GSensorTestActivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		initResume();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
				Log.e(ModuleTestApplication.TAG, "======SpeedSensor Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}