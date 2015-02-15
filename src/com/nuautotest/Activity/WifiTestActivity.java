package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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
	private TextView tvWifiStatus, tvWifiConnStatus;

	private ModuleTestApplication application;
	private WifiManager mWifiManager;
	private BroadcastReceiver mBroadcastRcv;
	private List<ScanResult> mWifiList;
	private boolean mWifiEnable = false;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	public class WifiBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				int mWifiState = mWifiManager.getWifiState();

				if (isAutomatic) {
					if (mWifiState == WifiManager.WIFI_STATE_ENABLED)
						stopAutoTest(true);
				} else {
					switch(mWifiState) {
						case WifiManager.WIFI_STATE_DISABLING:
							tvWifiStatus.setText("Wifi状态:关闭中...");
							break;
						case WifiManager.WIFI_STATE_DISABLED:
							tvWifiStatus.setText("Wifi状态:关闭");
							break;
						case WifiManager.WIFI_STATE_ENABLING:
							tvWifiStatus.setText("Wifi状态:打开中...");
							break;
						case WifiManager.WIFI_STATE_ENABLED:
							tvWifiStatus.setText("Wifi状态:打开");
							break;
						case WifiManager.WIFI_STATE_UNKNOWN:
							tvWifiStatus.setText("Wifi状态:不可用");
							break;
					}
				}
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				if (isAutomatic) {
					mWifiManager.startScan();
					mWifiList = mWifiManager.getScanResults();
					stopAutoTest(true);
				} else {
					WifiInfo wifiInfo;
					wifiInfo = mWifiManager.getConnectionInfo();
					if (wifiInfo.getNetworkId() != -1) {
						tvWifiConnStatus.setText("当前网络:\r\n");
						tvWifiConnStatus.append("编号:"+wifiInfo.getNetworkId()+"\r\n");
						tvWifiConnStatus.append("名称:"+wifiInfo.getSSID()+"\r\n");
						tvWifiConnStatus.append("IP:"+ipIntToString(wifiInfo.getIpAddress())+"\r\n");
						tvWifiConnStatus.append("服务器状态:"+wifiInfo.getSupplicantState().name()+"\r\n");
						tvWifiConnStatus.append("Pinging服务器...\t");
						boolean ping = mWifiManager.pingSupplicant();
						if (ping)
							tvWifiConnStatus.append("成功\r\n");
						else
							tvWifiConnStatus.append("失败\r\n");
					} else {
						tvWifiConnStatus.setText("无网络连接");
					}
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
		tvWifiStatus = (TextView)findViewById(R.id.tvwifistatus);
		tvWifiConnStatus = (TextView)findViewById(R.id.tvwificonnstatus);
		initCreate();
	}

	protected void initCreate() {
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

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mBroadcastRcv = new WifiBroadcastReceiver();
		mContext.registerReceiver(mBroadcastRcv, intentFilter);
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
//		TextView tvPingBaidu = (TextView)findViewById(R.id.tvPingBaidu);
//
//		tvPingBaidu.setText("Pinging www.baidu.com...\t");
//		Runtime run = Runtime.getRuntime();
//		Process proc = null;
//		String strPing = "ping -c 1 -i 0.2 -w 100 www.baidu.com";
//		int result;
//
//		try {
//			proc = run.exec(strPing);
//			result = proc.waitFor();
//			if (result == 0)
//				tvPingBaidu.append(""+result);
//			else
//				tvPingBaidu.append(""+result);
//		} catch (IOException e) {
//			e.printStackTrace();
//			tvPingBaidu.append("Ping Error");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			tvPingBaidu.append("Ping Error");
//		}
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW");
		Uri uri = Uri.parse("http://www.nufront.com");
		intent.setData(uri);
		startActivity(intent);
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "WifiTestActivity"+"======"+error+"======");
		if (!isAutomatic)
			application = ModuleTestApplication.getInstance();
		application.setTestState(getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		application.setTestState(mContext.getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		if (mWifiManager.getWifiState()==WifiManager.WIFI_STATE_ENABLED) {
			stopAutoTest(true);
		}
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.setTestState(mContext.getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
			if (mWifiList!=null && mWifiList.size()>0)
				application.getTooltip()[application.getIndex(mContext.getString(R.string.wifi_test))] =
						"名称："+mWifiList.get(0).SSID+" 信号："+mWifiList.get(0).level+"dBm";
		} else
			application.setTestState(mContext.getString(R.string.wifi_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
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
				Log.e(ModuleTestApplication.TAG, "======Wifi Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
