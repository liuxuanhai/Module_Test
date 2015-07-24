package com.nuautotest.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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
 * 充电测试
 *
 * @author xie-hang
 *
 */

public class ChargerTestActivity extends Activity
{
	private TextView text;
	private BroadcastReceiver broadcastRec;
	private Intent mRegisteredListener;
	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.charger_test);
		text = (TextView)findViewById(R.id.tvCharger);
		mContext = this;

		initCreate();
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_charger.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Charger Test---");

		mRegisteredListener = null;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

		Intent batteryStatus = mContext.registerReceiver(null, intentFilter);
		int chargePlug = 0;
		if (batteryStatus != null)
			chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		if (!isAutomatic) {
			if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB)
				text.setText(mContext.getString(R.string.charger_usb));
			else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC)
				text.setText(mContext.getString(R.string.charger_ac));
			else
				text.setText(mContext.getString(R.string.charger_none));
		} else {
			if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
					chargePlug == BatteryManager.BATTERY_PLUGGED_AC)
				stopAutoTest(true);
		}

		broadcastRec = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				System.out.println("==============" + intent.getAction());
				if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					if (isAutomatic) {
						if (intent.getIntExtra("plugged", 0) == BatteryManager.BATTERY_PLUGGED_AC ||
								intent.getIntExtra("plugged", 0) == BatteryManager.BATTERY_PLUGGED_USB)
							stopAutoTest(true);
					} else {
						if (intent.getIntExtra("plugged", 0) == BatteryManager.BATTERY_PLUGGED_AC) {
							Log.i(ModuleTestApplication.TAG, "AC in");
							text.setText(mContext.getString(R.string.charger_ac));
						} else if (intent.getIntExtra("plugged", 0) == BatteryManager.BATTERY_PLUGGED_USB) {
							Log.i(ModuleTestApplication.TAG, "USB in");
							text.setText(mContext.getString(R.string.charger_usb));
						} else {
							Log.i(ModuleTestApplication.TAG, "Unplugged");
							text.setText(mContext.getString(R.string.charger_none));
						}
					}
				}
			}
		};

		mRegisteredListener = mContext.registerReceiver(broadcastRec, intentFilter);
	}

	@Override
	protected void onDestroy() {
		releaseDestroy();
		super.onDestroy();
	}

	void releaseDestroy() {
		if (mRegisteredListener != null) {
			mContext.unregisterReceiver(broadcastRec);
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
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.charger_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.charger_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.charger_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.charger_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.charger_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
		this.finish();
	}

	private class AutoTestThread extends Handler implements Runnable {

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
				Log.e(ModuleTestApplication.TAG, "======Charging Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}