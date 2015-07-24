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
import android.view.View;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 光传感器
 *
 * @author xie-hang
 *
 */

public class LightSensorTestActivity extends Activity implements SensorEventListener {
	private SensorManager sm;
	private Sensor lightSensor;
	private TextView tvLight;
	private boolean mRegisteredSensor;
	private float dataLux;
	private float autoRecLux = -1;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.light_sensor_test);
		tvLight = (TextView) findViewById(R.id.lightsensor);
		initCreate();
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_lightsensor.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Light Sensor Test---");

		dataLux = 0;

		sm = (SensorManager)mContext.getSystemService(SENSOR_SERVICE);
		if (sm == null) postError("In initCreate():Get SENSOR_SERVICE failed");
		lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
	}

	@Override
	public void onResume() {
		super.onResume();

		initResume();
	}

	void initResume() {
		if (!mRegisteredSensor) {
			mRegisteredSensor = sm.registerListener(this, lightSensor,
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

	void releasePause() {
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
		dataLux = event.values[0];

		if (isAutomatic) {
			if (autoRecLux != -1 && autoRecLux != dataLux)
				stopAutoTest(true);
			else
				autoRecLux = dataLux;
		} else
			tvLight.setText(String.valueOf(dataLux));
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
		Log.e(ModuleTestApplication.TAG, "LightSensorTestAcitivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		initResume();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.lightsensor_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
			while (!isFinished && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				Log.e(ModuleTestApplication.TAG, "======Light Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
