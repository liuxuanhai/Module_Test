package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
 * 蓝牙测试
 *
 * @author xie-hang
 *
 */

public class BlueToothTestActivity extends Activity {
	private TextView tvBTStatus, tvBTConnStatus;
	private static CharSequence strConnInfo;
	BluetoothDevice mDevice;

	private ModuleTestApplication application;
	private BluetoothAdapter mBTAdapter;
	private BroadcastReceiver mBroadcastRcv;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	public class BTBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isAutomatic) {
				if (((BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())
						&& mBTAdapter.getState()==BluetoothAdapter.STATE_ON))
						|| (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) ) {
					mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					stopAutoTest(true);
				}
			} else {
				if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
					switch(mBTAdapter.getState()) {
						case BluetoothAdapter.STATE_OFF:
							tvBTStatus.setText("蓝牙状态:关闭");
							break;
						case BluetoothAdapter.STATE_TURNING_ON:
							tvBTStatus.setText("蓝牙状态:打开中...");
							break;
						case BluetoothAdapter.STATE_ON:
							tvBTStatus.setText("蓝牙状态:打开");
							mBTAdapter.startDiscovery();
							break;
						case BluetoothAdapter.STATE_TURNING_OFF:
							tvBTStatus.setText("蓝牙状态:关闭中...");
							break;
					}
				} else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
					mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (mDevice != null) {
						tvBTConnStatus.append("名称:"+mDevice.getName()+"\t地址:"
								+mDevice.getAddress()+"\r\n");
					}
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.bluetooth_test);
		tvBTStatus = (TextView)findViewById(R.id.tvbtstatus);
		tvBTConnStatus = (TextView)findViewById(R.id.tvbtconnstatus);
		initCreate();
	}

	protected void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_bluetooth.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Bluetooth Test---");

		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBTAdapter == null)
			postError("In initCreate():Bluetooth is not supported");
		else {
			mBTAdapter.enable();

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
			mBroadcastRcv = new BTBroadcastReceiver();
			mContext.registerReceiver(mBroadcastRcv, intentFilter);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		switch(mBTAdapter.getState()) {
			case BluetoothAdapter.STATE_OFF:
				tvBTStatus.setText("蓝牙状态:关闭");
				break;
			case BluetoothAdapter.STATE_TURNING_ON:
				tvBTStatus.setText("蓝牙状态:打开中...");
				break;
			case BluetoothAdapter.STATE_ON:
				tvBTStatus.setText("蓝牙状态:打开");
				tvBTConnStatus.setText(strConnInfo);
				mBTAdapter.startDiscovery();
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				tvBTStatus.setText("蓝牙状态:关闭中...");
				break;
		}
	}

	@Override
	public void onPause() {
		strConnInfo = tvBTConnStatus.getText();

		super.onPause();
	}

	@Override
	public void onDestroy() {
		releaseDestroy();

		super.onDestroy();
	}

	protected void releaseDestroy() {
		try {
			mContext.unregisterReceiver(mBroadcastRcv);
		} catch (IllegalArgumentException e) {
			Log.i(ModuleTestApplication.TAG, "======Bluetooth Bcv Not Registered======");
		}
		if (mBTAdapter!=null) mBTAdapter.disable();

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onEnableBT(View view) {
		startActivity(new Intent(
				android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {

	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "BlueToothTestActivity"+"======"+error+"======");
		if (!isAutomatic)
			application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		application.setTestState(mContext.getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		if ( (mBTAdapter!=null) && (mBTAdapter.getState()==BluetoothAdapter.STATE_ON) ) {
			stopAutoTest(true);
		}
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.setTestState(mContext.getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
			if (mDevice != null)
				application.getTooltip()[application.getIndex(mContext.getString(R.string.bluetooth_test))] =
						"名称："+mDevice.getName()+" 地址："+mDevice.getAddress();
		} else
			application.setTestState(mContext.getString(R.string.bluetooth_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
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
			while ( (!isFinished) && (time<2000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 2000) {
				stopAutoTest(false);
				Log.e(ModuleTestApplication.TAG, "======Bluetooth Test FAILED======");
			}
		}
	}
}
