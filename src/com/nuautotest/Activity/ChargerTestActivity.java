package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
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
	private ModuleTestApplication application;
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
		text = (TextView) findViewById(R.id.sdsensor);
		mContext = this;

		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_charger.txt");
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
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		if (!isAutomatic) {
			if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB)
				text.setText("USB充电器已插入");
			else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC)
				text.setText("AC充电器已插入");
			else
				text.setText("充电器拔出");
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
							text.setText("AC充电器已插入");
						} else if (intent.getIntExtra("plugged", 0) == BatteryManager.BATTERY_PLUGGED_USB) {
							Log.i(ModuleTestApplication.TAG, "USB in");
							text.setText("USB充电器已插入");
						} else {
							Log.i(ModuleTestApplication.TAG, "Unplugged");
							text.setText("充电器拨出");
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

	protected void releaseDestroy() {
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
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.charger_test))]="失败";
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.charger_test))]="成功";
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		application.getTooltip()[application.getIndex(mContext.getString(R.string.charger_test))] = "***请插入充电器***";
		application.getListViewState()[application.getIndex(mContext.getString(R.string.charger_test))]="测试中";
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		application.getTooltip()[application.getIndex(mContext.getString(R.string.charger_test))] = "";
		if (success) {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.charger_test))]="成功";
		} else {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.charger_test))]="失败";
		}
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
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