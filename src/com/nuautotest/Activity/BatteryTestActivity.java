package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
	private TextView text;
	private BroadcastReceiver broadcastRec;
	private ModuleTestApplication application;
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
		text = (TextView) findViewById(R.id.bsensor);
		initCreate();
	}

	protected void initCreate() {
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
						stopAutoTest(true);
					} else {
						text.setText("电量："+dataLevel+"%"+"\n"
										+"最大容量："+intent.getIntExtra("scale", 0)+"%"	+"\n"
										+"电压："+dataVol+"mV"+"\n"
										+"温度："+dataTemp+"度"+"\n"
						);
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

	protected void releaseDestroy() {
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
				application= ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "BatteryTestActivity"+"======"+error+"======");
		if (!isAutomatic)
			application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		application.setTestState(mContext.getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.setTestState(mContext.getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
			application.getTooltip()[application.getIndex(mContext.getString(R.string.battery_test))] =
					"电量:"+dataLevel+" 电压:"+dataVol+" 温度:"+dataTemp;
		} else {
			application.setTestState(mContext.getString(R.string.battery_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
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
				Log.e(ModuleTestApplication.TAG, "======Battery Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}