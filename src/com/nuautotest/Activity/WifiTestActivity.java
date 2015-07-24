package com.nuautotest.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Wifi测试
 *
 * @author xie-hang
 *
 */

public class WifiTestActivity extends Activity {
	private TextView tvWifiStatus, tvWifiNumber;
	private TextView tvWifiID, tvWifiName, tvWifiIP, tvWifiServerStatus, tvWifiPing;

	private WifiManager mWifiManager;
	private BroadcastReceiver mBroadcastRcv;
	private WifiScanThread mScanThread;
	private WifiUpdateHandler mUpdateHandler;
	private List<ScanResult> mScanResult;
	private boolean mWifiEnable = false;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	private class WifiBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				int mWifiState = mWifiManager.getWifiState();
				switch(mWifiState) {
					case WifiManager.WIFI_STATE_DISABLING:
						tvWifiStatus.setText(mContext.getString(R.string.status_disabling));
						break;
					case WifiManager.WIFI_STATE_DISABLED:
						tvWifiStatus.setText(mContext.getString(R.string.status_disabled));
						break;
					case WifiManager.WIFI_STATE_ENABLING:
						tvWifiStatus.setText(mContext.getString(R.string.status_enabling));
						break;
					case WifiManager.WIFI_STATE_ENABLED:
						tvWifiStatus.setText(mContext.getString(R.string.status_enabled));
						break;
					case WifiManager.WIFI_STATE_UNKNOWN:
						tvWifiStatus.setText(mContext.getString(R.string.status_unknown));
						break;
				}
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				WifiInfo wifiInfo;
				wifiInfo = mWifiManager.getConnectionInfo();
				if (wifiInfo.getNetworkId() != -1) {
					tvWifiID.setText(String.valueOf(wifiInfo.getNetworkId()));
					tvWifiName.setText(wifiInfo.getSSID());
					tvWifiIP.setText(ipIntToString(wifiInfo.getIpAddress()));
					tvWifiServerStatus.setText(wifiInfo.getSupplicantState().name());
					boolean ping = mWifiManager.pingSupplicant();
					if (ping)
						tvWifiPing.setText(mContext.getString(R.string.success));
					else
						tvWifiPing.setText(mContext.getString(R.string.fail));
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (device != null)
			Log.i(ModuleTestApplication.TAG, "======"+device.getDeviceName()+"======");
		mContext = this;
		setContentView(R.layout.wifi_test);
		tvWifiStatus = (TextView)findViewById(R.id.tvWifiStatus);
		tvWifiNumber = (TextView)findViewById(R.id.tvWifiNumber);
		tvWifiID = (TextView)findViewById(R.id.tvWifiID);
		tvWifiName = (TextView)findViewById(R.id.tvWifiName);
		tvWifiIP = (TextView)findViewById(R.id.tvWifiIp);
		tvWifiServerStatus = (TextView)findViewById(R.id.tvWifiServerStatus);
		tvWifiPing = (TextView)findViewById(R.id.tvWifiPing);
		mUpdateHandler = new WifiUpdateHandler();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mBroadcastRcv = new WifiBroadcastReceiver();
		mContext.registerReceiver(mBroadcastRcv, intentFilter);

		initCreate();
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_wifi.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Wifi Test---");

		mWifiManager = (WifiManager)mContext.getSystemService(WIFI_SERVICE);
		if (mWifiManager == null) postError("In initCreate():Get WIFI_SERVICE failed");
		mWifiManager.setWifiEnabled(true);
		if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) mWifiEnable = true;

		mScanThread = new WifiScanThread();
		Thread thread = new Thread(mScanThread);
		thread.start();
	}

	@Override
	public void onDestroy() {
		releaseDestroy();
		super.onDestroy();
	}

	void releaseDestroy() {
		mScanThread.shouldStop = true;
		try {
			mContext.unregisterReceiver(mBroadcastRcv);
		} catch (IllegalArgumentException e) {
			Log.i(ModuleTestApplication.TAG, "======Wifi Bcv Not Registered======");
		}
		if (!mWifiEnable) mWifiManager.setWifiEnabled(false);

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String ipIntToString(int ip) {
		byte[] bytes = new byte[4];

		try {
			bytes[0] = (byte)(0xff & ip);
			bytes[1] = (byte)((0xff00 & ip)>>8);
			bytes[2] = (byte)((0xff0000 & ip)>>16);
			bytes[3] = (byte)((0xff000000 & ip)>>24);
			return Inet4Address.getByAddress(bytes).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return "";
		}
	}

	public void onEnableWifi(View view) {
//		switch(mWifiState) {
//		case WifiManager.WIFI_STATE_DISABLED:
//			mWifiManager.setWifiEnabled(true);
//			break;
//		case WifiManager.WIFI_STATE_ENABLED:
//			mWifiManager.setWifiEnabled(false);
//			break;
//		case WifiManager.WIFI_STATE_DISABLING:
//		case WifiManager.WIFI_STATE_ENABLING:
//		case WifiManager.WIFI_STATE_UNKNOWN:
//			break;
//		}
		startActivity(new Intent(
				android.provider.Settings.ACTION_WIFI_SETTINGS));
	}

	public void onOpenWeb(View view) {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW");
		Uri uri = Uri.parse("http://www.nufront.com");
		intent.setData(uri);
		startActivity(intent);
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
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
		Log.e(ModuleTestApplication.TAG, "WifiTestActivity" + "======" + error + "======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	private class WifiUpdateHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			tvWifiNumber.setText(String.valueOf(mScanResult.size()));
		}
	}

	class WifiScanThread extends Handler implements Runnable {
		public boolean shouldStop = false;

		@Override
		public void handleMessage(Message msg) {
			Log.d(ModuleTestApplication.TAG, "ScanThread handleMessage:"+msg.what);
			shouldStop = true;
		}

		@Override
		public void run() {
			while (!shouldStop) {
				mWifiManager.startScan();
				mScanResult = mWifiManager.getScanResults();
				if (isAutomatic) {
					if (!mScanResult.isEmpty()) stopAutoTest(true);
				} else
					mUpdateHandler.sendEmptyMessage(0);
				try {
					Thread.sleep(10);
				} catch (InterruptedException ignored) {}
			}
		}
	}

	void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.wifi_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		releaseDestroy();
		isFinished = true;
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
				Log.e(ModuleTestApplication.TAG, "======Wifi Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
