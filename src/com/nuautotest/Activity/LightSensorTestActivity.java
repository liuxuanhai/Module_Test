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
 * 光传感器
 *
 * @author xie-hang
 *
 */

public class LightSensorTestActivity extends Activity implements SensorEventListener {
	private SensorManager sm;
	private Sensor ligthSensor;
	private TextView tvValue;
	private ModuleTestApplication application;
	private boolean mRegisteredSensor;
	private float dataLux;

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
		tvValue = (TextView) findViewById(R.id.lightsensor);
		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_lightsensor.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Light Sensor Test---");

		dataLux = 0;

		sm = (SensorManager)mContext.getSystemService(SENSOR_SERVICE);
		if (sm == null) postError("In initCreate():Get SENSOR_SERVICE failed");
		ligthSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
	}

	@Override
	public void onResume() {
		super.onResume();

		initResume();
	}

	protected void initResume() {
		if (!mRegisteredSensor) {
			mRegisteredSensor = sm.registerListener(this, ligthSensor,
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

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		dataLux = event.values[0];

		if (isAutomatic)
			stopAutoTest(true);
		else {
			tvValue.setText("光线强度:" + dataLux);
		}
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.lightsensor_test))]="失败";
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.lightsensor_test))]="成功";
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "LightSensorTestAcitivity"+"======"+error+"======");
		if (!isAutomatic)
			application= ModuleTestApplication.getInstance();
		application.getListViewState()[application.getIndex(getString(R.string.lightsensor_test))]="失败";
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		initResume();
		application.getListViewState()[application.getIndex(mContext.getString(R.string.lightsensor_test))]="测试中";
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.lightsensor_test))]="成功";
			application.getTooltip()[application.getIndex(mContext.getString(R.string.lightsensor_test))] = "光线强度:" + dataLux;
		} else {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.lightsensor_test))]="失败";
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
				Log.e(ModuleTestApplication.TAG, "======Light Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
