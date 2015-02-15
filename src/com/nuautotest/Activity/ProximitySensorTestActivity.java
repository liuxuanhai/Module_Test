package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 距离传感器
 *
 * @author xie-hang
 *
 */

public class ProximitySensorTestActivity extends Activity implements SensorEventListener {
	private SensorManager sm;
	private Sensor proxSensor;
	private TextView tvProx;
	private ModuleTestApplication application;
	private boolean mRegisteredSensor;
	private float dataProx;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.proximity_sensor_test);
		tvProx = (TextView) findViewById(R.id.proxsensor);
		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_proxsensor.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Proximity Sensor Test---");

		dataProx = 0;

		sm = (SensorManager)mContext.getSystemService(SENSOR_SERVICE);
		if (sm == null) postError("In initCreate():Get SENSOR_SERVICE failed");
		proxSensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	}

	@Override
	public void onResume() {
		super.onResume();

		initResume();
	}

	protected void initResume() {
		if (!mRegisteredSensor) {
			mRegisteredSensor = sm.registerListener(this, proxSensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			if (!mRegisteredSensor)
				postError("In initResume():registerListener failed");
		}
	}

	@Override
	public void onPause() {
		releasePause();

		super.onPause();
	}

	protected void releasePause() {
		if (mRegisteredSensor) {
			sm.unregisterListener(this);
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

	@Override
	public void onSensorChanged(SensorEvent event) {
		dataProx = event.values[0];

		if (isAutomatic)
			stopAutoTest(true);
		else
			tvProx.setText("距离:" + dataProx);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application= ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "ProximitySensorTestAcitivity"+"======"+error+"======");
		if (!isAutomatic)
			application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		initResume();
		application.setTestState(mContext.getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.setTestState(mContext.getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
			application.getTooltip()[application.getIndex(mContext.getString(R.string.proximitysensor_test))] = "距离:" + dataProx;
		} else {
			application.setTestState(mContext.getString(R.string.proximitysensor_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		}
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releasePause();
		this.finish();
	}

	public class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Application app, Handler handler) {
			super();
			mContext = context;
			application = (ModuleTestApplication) app;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
			while (!isFinished && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				Log.e(ModuleTestApplication.TAG, "======Proximity Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}