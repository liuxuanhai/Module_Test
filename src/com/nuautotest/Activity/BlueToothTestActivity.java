package com.nuautotest.Activity;

import android.app.Activity;
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
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * 蓝牙测试
 *
 * @author xie-hang
 *
 */

public class BlueToothTestActivity extends Activity {
	private TextView tvBTStatus, tvBTConnStatus, tvBTNumber;
	Vector<BluetoothDevice> mDevice = new Vector<BluetoothDevice>();

	private BluetoothAdapter mBTAdapter;
	private BroadcastReceiver mBroadcastRcv;

	private boolean mBtEnable = false;
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
					&& mBTAdapter.getState()==BluetoothAdapter.STATE_ON)))
					mBTAdapter.startDiscovery();
				else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
					mDevice.add((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
					Log.d(ModuleTestApplication.TAG, "Found device: "+
							mDevice.get(mDevice.size()-1).getName()+" "+
							mDevice.get(mDevice.size()-1).getAddress());
					stopAutoTest(true);
				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
					mBTAdapter.startDiscovery();
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
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (device != null) {
						if (indexOfDevice(device) == -1)
							mDevice.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
						else
							mDevice.set(indexOfDevice(device), device);
					}
					printRecord();
				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
					mBTAdapter.startDiscovery();
				}
			}
		}
	}

	protected void printRecord() {
		int i;

		tvBTNumber.setText("设备数量: " + mDevice.size());
		tvBTConnStatus.setText("附近设备:\r\n");
		for (BluetoothDevice device : mDevice) {
			tvBTConnStatus.append("名称:" + device.getName() + "\t地址:"
					+ device.getAddress() + "\r\n");
		}
	}

	protected int indexOfDevice(BluetoothDevice device) {
		int i;
		if (mDevice == null) return -1;
		for (i=0; i<mDevice.size(); i++) {
			if (mDevice.get(i).getAddress().equals(device.getAddress()))
				return i;
		}
		return -1;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		setContentView(R.layout.bluetooth_test);
		tvBTStatus = (TextView)findViewById(R.id.tvbtstatus);
		tvBTConnStatus = (TextView)findViewById(R.id.tvbtconnstatus);
		tvBTNumber = (TextView)findViewById(R.id.tvbtNumber);
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
			if (mBTAdapter.getState() == BluetoothAdapter.STATE_ON) mBtEnable = true;

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
			intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			mBroadcastRcv = new BTBroadcastReceiver();
			mContext.registerReceiver(mBroadcastRcv, intentFilter);

			if (mBTAdapter.isEnabled())
				mBTAdapter.startDiscovery();
			else
				mBTAdapter.enable();
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
				mBTAdapter.startDiscovery();
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
				tvBTStatus.setText("蓝牙状态:关闭中...");
				break;
		}
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
		if (mBTAdapter != null && !mBtEnable) mBTAdapter.disable();

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
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
		Log.e(ModuleTestApplication.TAG, "BlueToothTestActivity" + "======" + error + "======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	public void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.bluetooth_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
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
