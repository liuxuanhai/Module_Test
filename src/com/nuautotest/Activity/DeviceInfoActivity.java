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
import com.nuautotest.NativeLib.SystemPropertiesProxy;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;

/**
 * 设备信息
 *
 * @author xie-hang
 *
 */

public class DeviceInfoActivity extends Activity {
	private static final String CALIBRATION_PORT = "/dev/ttyN5";

	private TextView mtvIMEI, mtvPCBASN, mtvSignal, mtvCalibration, mtvPcbaTestStatus, mtvApkTestStatus;
	private TextView mtvBtName, mtvBtAddr, mtvBtScanmode, mtvBtState;
	private TextView mtvWifiBSSID, mtvWifiSSID, mtvWifiMac, mtvWifiIp, mtvWifiSpeed;
	private TextView mtvBatteryCapacity, mtvBatteryVoltage, mtvBatteryTemperature;
	private TextView mtvVersionAP, mtvVersionBP, mtvVersionKernel;

	private TelephonyManager mPhoneManager;
	private BluetoothAdapter mBluetoothAdapter;
	private WifiInfo mWifiInfo;
	private BroadcastReceiver mBatteryBcr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_info);

		mtvIMEI = (TextView)this.findViewById(R.id.tvDIIMEI);
		mtvPCBASN = (TextView)this.findViewById(R.id.tvDIPCBASN);
		mtvSignal = (TextView)this.findViewById(R.id.tvDISignal);
		mtvCalibration = (TextView)this.findViewById(R.id.tvDICalibration);
		mtvPcbaTestStatus = (TextView)this.findViewById(R.id.tvDIPcbaTestStatus);
		mtvApkTestStatus = (TextView)this.findViewById(R.id.tvDIApkTestStatus);
		mtvBtName = (TextView)this.findViewById(R.id.tvDIBtName);
		mtvBtAddr = (TextView)this.findViewById(R.id.tvDIBtAddr);
		mtvBtScanmode = (TextView)this.findViewById(R.id.tvDIBtScanmode);
		mtvBtState = (TextView)this.findViewById(R.id.tvDIBtState);
		mtvWifiBSSID = (TextView)this.findViewById(R.id.tvDIWifiBSSID);
		mtvWifiSSID = (TextView)this.findViewById(R.id.tvDIWifiSSID);
		mtvWifiMac = (TextView)this.findViewById(R.id.tvDIWifiMac);
		mtvWifiIp = (TextView)this.findViewById(R.id.tvDIWifiIp);
		mtvWifiSpeed = (TextView)this.findViewById(R.id.tvDIWifiSpeed);
		mtvBatteryCapacity = (TextView)this.findViewById(R.id.tvDIBatteryCapacity);
		mtvBatteryVoltage = (TextView)this.findViewById(R.id.tvDIBatteryVoltage);
		mtvBatteryTemperature = (TextView)this.findViewById(R.id.tvDIBatteryTemperature);
		mtvVersionAP = (TextView)this.findViewById(R.id.tvDIVersionAP);
		mtvVersionBP = (TextView)this.findViewById(R.id.tvDIVersionBP);
		mtvVersionKernel = (TextView)this.findViewById(R.id.tvDIVersionKernel);

		mPhoneManager = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
		SignalStrengthListener mSignalListener = new SignalStrengthListener();
		mPhoneManager.listen(mSignalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		WifiManager mWifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mBatteryBcr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					mtvBatteryCapacity.setText(getString(R.string.battery_capacity) + intent.getIntExtra("level", 0) + "%");
					mtvBatteryVoltage.setText(getString(R.string.battery_voltage) + intent.getIntExtra("voltage", 0) + "mV");
					mtvBatteryTemperature.setText(getString(R.string.battery_temperature) + intent.getIntExtra("temperature", 0)/10 + "℃");
				}
			}
		};
		this.registerReceiver(mBatteryBcr, intentFilter);
	}

	private void getIMEI() {
		if (mPhoneManager != null) mtvIMEI.setText(mPhoneManager.getDeviceId());
	}

	private void getPCBASN() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("/misc/pcbasn"));
			String line = br.readLine();
			if (line != null) mtvPCBASN.setText(line);
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
	}

	private void getCalibration() {
		try {
			final String CALIB_WRITE = "at+xprs?\r\n";
			final String CALIB_READ = "+XPRS:";
			final String CALIB_STATE = "CAL=";
			final String TEST_STATE = "FT=";
			FileWriter fw = new FileWriter(CALIBRATION_PORT);
			FileReader fr = new FileReader(CALIBRATION_PORT);
			char calib_state[] = new char[64];
			int count, time = 0;
			Integer iCalib_state = -1, iTest_state = -1;

			while (time < 10) {
				try {
					fw.write(CALIB_WRITE);
					fw.flush();

					count = fr.read(calib_state);
					String szCalib_state = String.valueOf(calib_state, 0, count);
					if (szCalib_state.contains(CALIB_READ)) {
						if (szCalib_state.contains(CALIB_STATE)) {
							int index = szCalib_state.lastIndexOf(CALIB_STATE);
							String substr = szCalib_state.substring(index + CALIB_STATE.length(), index + CALIB_STATE.length() + 1);
							iCalib_state = Integer.valueOf(substr, 16);
						}
						if (szCalib_state.contains(TEST_STATE)) {
							int index = szCalib_state.lastIndexOf(TEST_STATE);
							String substr = szCalib_state.substring(index + TEST_STATE.length(), index + TEST_STATE.length() + 1);
							iTest_state = Integer.valueOf(substr, 16);
						}

						switch (iCalib_state) {
							case -1:
								mtvCalibration.setText(getString(R.string.calibration_none));
								break;
							case 0:
								mtvCalibration.setText(getString(R.string.calibration_fail));
								break;
							case 1:
								mtvCalibration.setText(getString(R.string.calibration_pass));
								break;
							default:
								Log.e(ModuleTestApplication.TAG, "Unknown calibration state: " + iCalib_state);
								break;
						}
						switch (iTest_state) {
							case -1:
								mtvCalibration.append(", " + getString(R.string.bptest_none));
								break;
							case 0:
								mtvCalibration.append(", " + getString(R.string.bptest_fail));
								break;
							case 1:
								mtvCalibration.append(", " + getString(R.string.bptest_pass));
								break;
							default:
								Log.e(ModuleTestApplication.TAG, "Unknown test state: " + iTest_state);
								break;
						}
						break;
					} else {
						Log.w(ModuleTestApplication.TAG, CALIB_READ + " not found in " + szCalib_state);
						time++;
					}
				} catch (Exception ignored) {}
			}
			fw.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getTestStatus() {
		mtvPcbaTestStatus.setText("PCBA: " + getString(R.string.not_finish));
		mtvApkTestStatus.setText("APK: " + getString(R.string.not_finish));
		try {
			BufferedReader br = new BufferedReader(new FileReader("/misc/prodmark"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("PCBATEST=1")) {
					mtvPcbaTestStatus.setText("PCBA: " + getString(R.string.finish));
				} else if (line.equals("APKTEST=1")) {
					mtvApkTestStatus.setText("APK: " + getString(R.string.finish));
				}
			}
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
	}

	private void getBluetooth() {
		if (mBluetoothAdapter != null) {
			mtvBtName.setText("Name: " + mBluetoothAdapter.getName());
			mtvBtAddr.setText("Address: " + mBluetoothAdapter.getAddress());
			mtvBtScanmode.setText("Scan mode: ");
			switch (mBluetoothAdapter.getScanMode()) {
				case BluetoothAdapter.SCAN_MODE_NONE:
					mtvBtScanmode.append(getString(R.string.bt_scan_mode_none));
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
					mtvBtScanmode.append(getString(R.string.bt_scan_mode_connectable));
					break;
				case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
					mtvBtScanmode.append(getString(R.string.bt_scan_mode_connectable_discoverable));
					break;
			}
			mtvBtState.setText("State: ");
			switch (mBluetoothAdapter.getState()) {
				case BluetoothAdapter.STATE_OFF:
					mtvBtState.append(getString(R.string.status_disabled));
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					mtvBtState.append(getString(R.string.status_enabling));
					break;
				case BluetoothAdapter.STATE_ON:
					mtvBtState.append(getString(R.string.status_enabled));
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					mtvBtState.append(getString(R.string.status_disabling));
					break;
			}
		}
	}

	private void getWifi() {
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
	public void onResume() {
		super.onResume();

		getIMEI();
		getPCBASN();
		getCalibration();
		getTestStatus();
		getBluetooth();
		getWifi();
	}

	@Override
	public void onPause() {
		try {
			if (mBatteryBcr != null) this.unregisterReceiver(mBatteryBcr);
		} catch (Exception ignored) {}
		super.onPause();
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	/*public static String formatKernelVersion(String rawKernelVersion) {
		// Example (see tests for more):
		// Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
		//     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
		//     Thu Jun 28 11:02:39 PDT 2012

		final String PROC_VERSION_REGEX =
				"Linux version (\\S+) " +       *//* group 1: "3.0.31-g6fb96c9" *//*
				"\\((\\S+?)\\) " +              *//* group 2: "x@y.com" (kernel builder) *//*
				"(?:\\(gcc.+? \\)) " +          *//* ignore: GCC version information *//*
				"(#\\d+) " +                    *//* group 3: "#1" *//*
				"(?:.*?)?" +                    *//* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS *//*
				"((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; *//* group 4: "Thu Jun 28 11:02:39 PDT 2012" *//*

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
	}*/

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

			return (cdmaAsuLevel < ecioAsuLevel) ? cdmaAsuLevel : ecioAsuLevel;
		}
	}
}