package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GPS测试
 *
 * @author xie-hang
 *
 */

public class GPSTestActivity extends Activity
		implements LocationListener, GpsStatus.Listener {

	TextView text, record;
	private int[] mPRN = new int[10];
	private float[] mSNR = new float[10];
	private int mSatelliteCount = 0;
	private boolean mRegisteredListener, mProviderEnable;
	private ModuleTestApplication application;

	private LocationManager mLocationManager;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
//	private Looper mLooper;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.gps_test);
		text = (TextView)findViewById(R.id.gsensor);
		record = (TextView)findViewById(R.id.grecord);
		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_gps.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---GPS Test---");

		mRegisteredListener = false;
		mLocationManager = (LocationManager)mContext.getSystemService(LOCATION_SERVICE);
		if (mLocationManager == null)
			postError("In initCreate():Get LOCATION_SERVICE failed");
	}

	@Override
	public void onResume() {
		super.onResume();

		initResume();
		printRecord();
	}

	protected void initResume() {
		mProviderEnable = Settings.Secure.isLocationProviderEnabled(
				mContext.getContentResolver(), LocationManager.GPS_PROVIDER);

		try {
			Settings.Secure.setLocationProviderEnabled(
					mContext.getContentResolver(), LocationManager.GPS_PROVIDER, true);
		} catch (Exception ignored) {}

		try {
			mRegisteredListener
					= mLocationManager.addGpsStatusListener(this);
			if (!mRegisteredListener)
				postError("In initResume():addGpsStatusListener failed");
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					0, 0, this);
		} catch (SecurityException e) {
			postError("In initResume():"+e);
		} catch (IllegalArgumentException e) {
			postError("In initResume():"+e);
		} catch (RuntimeException e) {
			postError("In initResume():"+e);
		}
	}

	@Override
	public void onPause() {
		releasePause();
		record.setText("");

		super.onPause();
	}

	protected void releasePause() {
		try {
			if (mRegisteredListener) {
				mLocationManager.removeUpdates(this);
				mLocationManager.removeGpsStatusListener(this);
				mRegisteredListener = false;
			}
		} catch (IllegalArgumentException e) {
			postError("In releasePause():"+e);
		}

		try {
			if (!mProviderEnable) Settings.Secure.setLocationProviderEnabled(
					mContext.getContentResolver(), LocationManager.GPS_PROVIDER, false);
		} catch (Exception ignored) {}

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onGpsStatusChanged(int event) {
		GpsStatus status;
		Iterator<GpsSatellite> iterator;
		GpsSatellite satellite;

		switch(event) {
			case GpsStatus.GPS_EVENT_STARTED:
				if (!isAutomatic) text.setText("GPS打开");
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				if (!isAutomatic) text.setText("GPS关闭");
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				if (!isAutomatic) text.setText("首次定位");
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				status = mLocationManager.getGpsStatus(null);
				if (status == null)
					Log.e(ModuleTestApplication.TAG, "In onGpsStatusChanged():getGpsStatus failed");
				int satelliteCount = 0;
				if (status != null) {
					iterator = status.getSatellites().iterator();
					while (iterator.hasNext()) {
						satelliteCount++;
						satellite = iterator.next();
						if (indexOfSatellite(satellite.getPrn()) != -1)
							mSNR[indexOfSatellite(satellite.getPrn())] = satellite.getSnr();
						else if (mSatelliteCount < 10) {
							mPRN[mSatelliteCount] = satellite.getPrn();
							mSNR[mSatelliteCount] = satellite.getSnr();
							mSatelliteCount++;
						}
					}
				}
				if (isAutomatic) {
					if (satelliteCount>0) stopAutoTest(true);
				} else {
					printRecord();
					text.setText("卫星数量:"+satelliteCount);
					break;
				}
		}
	}

	private void printRecord() {
		if (mSatelliteCount == 0) record.setText("暂无数据");
		else {
			record.setText("卫星:\r\n");
			for (int i=0; i<mSatelliteCount; i++) {
				record.append("编号:"+mPRN[i]+"\t信号强度:"+mSNR[i]+"\r\n");
			}
		}
	}

	private int indexOfSatellite(int prn) {
		for (int i=0; i<mSatelliteCount; i++)
			if (prn == mPRN[i]) return i;
		return -1;
	}

	public void onLocationChanged(Location location) {
	}

	public void onProviderDisabled(String arg0) {
	}

	public void onProviderEnabled(String arg0) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {

	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "GPSTestActivity"+"======"+error+"======");
		if (!isAutomatic)
			application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
//		Looper.prepare();
//		mLooper = Looper.myLooper();
//		if (mLooper == null) postError("In startAutoTest():myLooper==null");
		initResume();
		application.setTestState(mContext.getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		Timer timer = new Timer();
		TimerTask task = new EndLoopTask();
		try {
			timer.schedule(task, 10000);
		} catch(IllegalStateException e) {
			postError("In startAutoTest():"+e);
		}
//		Looper.loop();
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.setTestState(mContext.getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
			application.getTooltip()[application.getIndex(mContext.getString(R.string.gps_test))] = "卫星数量："+mSatelliteCount;
		} else {
			application.setTestState(mContext.getString(R.string.gps_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		}
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releasePause();
//		if (mLooper != null) {
//			mLooper.quit();
//			mLooper = null;
//		}
		this.finish();
	}

	public class EndLoopTask extends TimerTask {
		@Override
		public void run() {
//			if (mLooper != null) {
//				mLooper.quit();
//				mLooper = null;
//			}
		}

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
			while ( (!isFinished) && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				Log.e(ModuleTestApplication.TAG, "======GPS Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
