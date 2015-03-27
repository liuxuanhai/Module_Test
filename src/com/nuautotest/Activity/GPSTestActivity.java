package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * GPS测试
 *
 * @author xie-hang
 *
 */

public class GPSTestActivity extends Activity
		implements LocationListener, GpsStatus.Listener {

	TextView text, record;
	private Vector<GpsSatellite> mData = new Vector<GpsSatellite>();
	private boolean mRegisteredListener, mProviderEnable;

	private LocationManager mLocationManager;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private Looper mLooper;
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

		if (!mProviderEnable) {
			try {
				Settings.Secure.setLocationProviderEnabled(
						mContext.getContentResolver(), LocationManager.GPS_PROVIDER, true);
			} catch (Exception ignored) {}
		}

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

		Log.d(ModuleTestApplication.TAG, "event= "+event);
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
				if (status != null) {
					iterator = status.getSatellites().iterator();
					while (iterator.hasNext()) {
						satellite = iterator.next();
						if (indexOfSatellite(satellite.getPrn()) != -1)
							mData.set(indexOfSatellite(satellite.getPrn()), satellite);
						else
							mData.add(satellite);
					}
				}
				if (isAutomatic) {
					if (mData.size()>0) stopAutoTest(true);
				} else {
					printRecord();
					break;
				}
		}
	}

	private void printRecord() {
		if (mData.size() == 0) record.setText("暂无数据");
		else {
			text.setText("卫星数量:"+mData.size());
			record.setText("卫星:\r\n");
			for (GpsSatellite data : mData) {
				record.append("编号: " + data.getPrn() +
						"\t\t信号强度: " + data.getSnr() +
						"\t\t海拔: " + data.getElevation() +
						"\t\t方位:" + data.getAzimuth() + "\r\n");
			}
		}
	}

	private int indexOfSatellite(int prn) {
		for (int i=0; i<mData.size(); i++)
			if (prn == mData.get(i).getPrn()) return i;
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
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
		Log.e(ModuleTestApplication.TAG, "GPSTestActivity" + "======" + error + "======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
//		Looper.prepare();
		mLooper = Looper.myLooper();
		if (mLooper == null) postError("In startAutoTest():myLooper==null");
		initResume();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		Timer timer = new Timer();
		TimerTask task = new EndLoopTask();
		try {
			timer.schedule(task, 10000);
		} catch(IllegalStateException e) {
			postError("In startAutoTest():"+e);
		}
		Looper.loop();
	}

	public void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.gps_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releasePause();
		if (mLooper != null) {
			mLooper.quit();
			mLooper = null;
		}
		this.finish();
	}

	public class EndLoopTask extends TimerTask {
		@Override
		public void run() {
			if (mLooper != null) stopAutoTest(false);
		}

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
					Thread.sleep(0);
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
