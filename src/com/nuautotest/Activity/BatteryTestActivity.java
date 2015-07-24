package com.nuautotest.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
 * 电池测试
 *
 * @author xie-hang
 *
 */

public class BatteryTestActivity extends Activity
{
	private TextView mtvCapacity, mtvMax, mtvTemperature, mtvVoltage;
	private BroadcastReceiver broadcastRec;
	private int dataLevel, dataVol, dataTemp;

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
		setContentView(R.layout.battery_test);
		mtvCapacity = (TextView)findViewById(R.id.tvBatteryCapacity);
		mtvMax = (TextView)findViewById(R.id.tvbatteryMax);
		mtvTemperature = (TextView)findViewById(R.id.tvBatteryTemperature);
		mtvVoltage = (TextView)findViewById(R.id.tvBatteryVoltage);
		initCreate();
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_battery.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Battery Test---");

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

		broadcastRec = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					dataLevel = intent.getIntExtra("level", 0);
					dataVol = intent.getIntExtra("voltage", 0);
					dataTemp = intent.getIntExtra("temperature", 0)/10;
					if (isAutomatic) {
						if (dataLevel>=0 && dataLevel <= intent.getIntExtra("scale", 0) &&
							dataVol >= 3000 && dataVol <= 5000)
							stopAutoTest(true);
						else
							stopAutoTest(false);
					} else {
						mtvCapacity.setText(dataLevel+"%");
						mtvMax.setText(intent.getIntExtra("scale", 0) + "%");
						mtvTemperature.setText(String.valueOf(dataTemp) + "℃");
						mtvVoltage.setText(dataVol+"mV");
					}
				}
			}
		};

		mContext.registerReceiver(broadcastRec, intentFilter);
	}

	@Override
	protected void onDestroy() {
		releaseDestroy();

		super.onDestroy();
	}

	void releaseDestroy() {
		try {
			mContext.unregisterReceiver(broadcastRec);
		} catch (IllegalArgumentException e) {
			Log.i(ModuleTestApplication.TAG, "======Battery Bcv Not Registered======");
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

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.battery_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.battery_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.battery_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.battery_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.battery_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
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
				Log.e(ModuleTestApplication.TAG, "======Battery Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}