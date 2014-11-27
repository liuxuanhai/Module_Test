package com.nuautotest.Activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;
import com.nuautotest.NativeLib.SystemPropertiesProxy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备信息
 *
 * @author xie-hang
 *
 */

public class DeviceInfoActivity extends Activity {
	static final String CALIBRATION_PORT = "/dev/ttyN5";

	private TextView mtvIMEI, mtvSignal, mtvCalibration;
	private TextView mtvBtName, mtvBtAddr, mtvBtScanmode, mtvBtState;
	private TextView mtvWifiBSSID, mtvWifiSSID, mtvWifiMac, mtvWifiIp, mtvWifiSpeed;
	private TextView mtvBatteryCapacity, mtvBatteryVoltage, mtvBatteryTemperature;
	private TextView mtvVersionAP, mtvVersionBP, mtvVersionKernel;

	private TelephonyManager mPhoneManager;
	private BluetoothAdapter mBluetoothAdapter;
	private WifiManager mWifiManager;
	private WifiInfo mWifiInfo;
	private SignalStrengthListener mSignalListener;
	private BroadcastReceiver mBatteryBcr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_info);

		mtvIMEI = (TextView)this.findViewById(R.id.tvIMEI);
		mtvSignal = (TextView)this.findViewById(R.id.tvSignal);
		mtvCalibration = (TextView)this.findViewById(R.id.tvCalibration);
		mtvBtName = (TextView)this.findViewById(R.id.tvBtName);
		mtvBtAddr = (TextView)this.findViewById(R.id.tvBtAddr);
		mtvBtScanmode = (TextView)this.findViewById(R.id.tvBtScanmode);
		mtvBtState = (TextView)this.findViewById(R.id.tvBtState);
		mtvWifiBSSID = (TextView)this.findViewById(R.id.tvWifiBSSID);
		mtvWifiSSID = (TextView)this.findViewById(R.id.tvWifiSSID);
		mtvWifiMac = (TextView)this.findViewById(R.id.tvWifiMac);
		mtvWifiIp = (TextView)this.findViewById(R.id.tvWifiIp);
		mtvWifiSpeed = (TextView)this.findViewById(R.id.tvWifiSpeed);
		mtvBatteryCapacity = (TextView)this.findViewById(R.id.tvBatteryCapacity);
		mtvBatteryVoltage = (TextView)this.findViewById(R.id.tvBatteryVoltage);
		mtvBatteryTemperature = (TextView)this.findViewById(R.id.tvBatteryTemperature);
		mtvVersionAP = (TextView)this.findViewById(R.id.tvVersionAP);
		mtvVersionBP = (TextView)this.findViewById(R.id.tvVersionBP);
		mtvVersionKernel = (TextView)this.findViewById(R.id.tvVersionKernel);

		mPhoneManager = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
		mSignalListener = new SignalStrengthListener();
		mPhoneManager.listen(mSignalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mWifiManager = (WifiManager)this.getSystemService(WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mBatteryBcr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					mtvBatteryCapacity.setText("电量: " + intent.getIntExtra("level", 0) + "%");
					mtvBatteryVoltage.setText("电压: " + intent.getIntExtra("voltage", 0) + "mV");
					mtvBatteryTemperature.setText("温度: " + intent.getIntExtra("temperature", 0)/10 + "度");
				}
			}
		};
		this.registerReceiver(mBatteryBcr, intentFilter);
	}

	@Override
	public void onResume() {
		super.onResume();

		/* IMEI */
		if (mPhoneManager != null) mtvIMEI.setText(mPhoneManager.getDeviceId());

		/* Calibration */
		try {
			final String CALIB_WRITE = "at+xprs?\r\n";
			final String CALIB_READ = "+XPRS:\"";
			FileWriter fw = new FileWriter(CALIBRATION_PORT);
			FileReader fr = new FileReader(CALIBRATION_PORT);
			char calib_state[] = new char[64];
			int count, time = 0;

			while (time < 10) {
				fw.write(CALIB_WRITE);
				fw.flush();

				count = fr.read(calib_state);
				String szCalib_state = String.valueOf(calib_state, 0, count);
				if (szCalib_state.contains(CALIB_READ)) {
					int index = szCalib_state.lastIndexOf(CALIB_READ);
					String substr = szCalib_state.substring(index + CALIB_READ.length(), index + CALIB_READ.length() + 4);
					Integer iCalib_state = Integer.valueOf(substr, 16);
					switch (iCalib_state) {
						case 0x0000:
							mtvCalibration.setText("未校准");
							break;
						case 0x8000:
							mtvCalibration.setText("已写号, 未校准");
							break;
						case 0xC000:
							mtvCalibration.setText("已校准");
							break;
						case 0xE000:
							mtvCalibration.setText("已校准, 已综测");
							break;
						default:
							Log.e(ModuleTestApplication.TAG, "Unknown calibration state: " + iCalib_state);
							break;
					}
					break;
				} else {
					Log.w(ModuleTestApplication.TAG, CALIB_READ + " not found in " + szCalib_state);
					time++;
				}
			}
			fw.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* Bluetooth */
		if (mBluetoothAdapter != null) {
			mtvBtName.setText("Name: " + mBluetoothAdapter.getName());
			mtvBtAddr.setText("Address: " + mBluetoothAdapter.getAddress());
			mtvBtScanmode.setText("Scan mode: ");
			switch (mBluetoothAdapter.getScanMode()) {
				case BluetoothAdapter.SCAN_MODE_NONE:
					mtvBtScanmode.append("不可用");
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
					mtvBtScanmode.append("可连接");
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
					mtvBtScanmode.append("可连接(可被发现)");
					break;
			}
			mtvBtState.setText("State: ");
			switch (mBluetoothAdapter.getState()) {
				case BluetoothAdapter.STATE_OFF:
					mtvBtState.append("关闭");
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					mtvBtState.append("打开中...");
					break;
				case BluetoothAdapter.STATE_ON:
					mtvBtState.append("打开");
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					mtvBtState.append("关闭中...");
					break;
			}
		}

		/* Wifi */
		mtvWifiBSSID.setText("BSSID: " + mWifiInfo.getBSSID());
		mtvWifiSSID.setText("SSID: " + mWifiInfo.getSSID());
		mtvWifiMac.setText("Mac: " + mWifiInfo.getMacAddress());
		mtvWifiIp.setText("IP: " + WifiTestActivity.ipIntToString(mWifiInfo.getIpAddress()));
		mtvWifiSpeed.setText("Speed: " + mWifiInfo.getLinkSpeed() + "Mbps");

		/* Version */
		mtvVersionAP.setText("AP: " + SystemPropertiesProxy.get(this, "ro.build.display.id"));
		mtvVersionBP.setText("BP: " + SystemPropertiesProxy.get(this, "gsm.version.baseband"));

		try {
			FileReader frKernel= new FileReader("/proc/version");

			char kernel_version[] = new char[1024];
			int count = frKernel.read(kernel_version);

			mtvVersionKernel.setText("Kernel: " + String.valueOf(kernel_version, 0, count));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPause() {
		try {
			if (mBatteryBcr != null) this.unregisterReceiver(mBatteryBcr);
		} catch (Exception e) {}
		super.onPause();
	}

	public static String formatKernelVersion(String rawKernelVersion) {
		// Example (see tests for more):
		// Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
		//     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
		//     Thu Jun 28 11:02:39 PDT 2012

		final String PROC_VERSION_REGEX =
				"Linux version (\\S+) " +       /* group 1: "3.0.31-g6fb96c9" */
				"\\((\\S+?)\\) " +              /* group 2: "x@y.com" (kernel builder) */
				"(?:\\(gcc.+? \\)) " +          /* ignore: GCC version information */
				"(#\\d+) " +                    /* group 3: "#1" */
				"(?:.*?)?" +                    /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
				"((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

		Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
		if (!m.matches()) {
			Log.e(ModuleTestApplication.TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
			return "Unavailable";
		} else if (m.groupCount() < 4) {
			Log.e(ModuleTestApplication.TAG, "Regex match on /proc/version only returned " + m.groupCount()
					+ " groups");
			return "Unavailable";
		}
		return m.group(1) + "\n" +                      // 3.0.31-g6fb96c9
				m.group(2) + " " + m.group(3) + "\n" +  // x@y.com #1
				m.group(4);                             // Thu Jun 28 11:02:39 PDT 2012
	}

	public class SignalStrengthListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			if (signalStrength.isGsm()) {
				mtvSignal.setText(getGsmDbm(signalStrength) + " dBm   ");
				mtvSignal.append(signalStrength.getGsmSignalStrength() + " asu");
			} else {
				mtvSignal.append(signalStrength.getCdmaDbm() + " dBm   ");
				mtvSignal.append(getCdmaAsuLevel(signalStrength) + " asu");
			}
		}

		public int getGsmDbm(SignalStrength signalStrength) {
			int dBm;

			int gsmSignalStrength = signalStrength.getGsmSignalStrength();
			int asu = (gsmSignalStrength == 99 ? -1 : gsmSignalStrength);
			if (asu != -1) {
				dBm = -113 + (2 * asu);
			} else {
				dBm = -1;
			}
			return dBm;
		}

		public int getCdmaAsuLevel(SignalStrength signalStrength) {
			final int cdmaDbm = signalStrength.getCdmaDbm();
			final int cdmaEcio = signalStrength.getCdmaEcio();
			int cdmaAsuLevel;
			int ecioAsuLevel;

			if (cdmaDbm >= -75) cdmaAsuLevel = 16;
			else if (cdmaDbm >= -82) cdmaAsuLevel = 8;
			else if (cdmaDbm >= -90) cdmaAsuLevel = 4;
			else if (cdmaDbm >= -95) cdmaAsuLevel = 2;
			else if (cdmaDbm >= -100) cdmaAsuLevel = 1;
			else cdmaAsuLevel = 99;

			// Ec/Io are in dB*10
			if (cdmaEcio >= -90) ecioAsuLevel = 16;
			else if (cdmaEcio >= -100) ecioAsuLevel = 8;
			else if (cdmaEcio >= -115) ecioAsuLevel = 4;
			else if (cdmaEcio >= -130) ecioAsuLevel = 2;
			else if (cdmaEcio >= -150) ecioAsuLevel = 1;
			else ecioAsuLevel = 99;

			int level = (cdmaAsuLevel < ecioAsuLevel) ? cdmaAsuLevel : ecioAsuLevel;
			return level;
		}
	}
}