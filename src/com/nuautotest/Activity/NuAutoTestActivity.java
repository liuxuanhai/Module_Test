package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;

/**
 * 测试界面
 *
 * @author xie-hang
 *
 */

public class NuAutoTestActivity extends Activity {
	static final String IS_FROM_BOOTRECEIVER = "com.nuautotest.IS_FROM_BOOTRECEIVER";

	private NuAutoTestAdapter adapter;
	private boolean mAutoTested;
	private AutoTestThread mAutoTestThread;
	public Handler mRefreshHandler;
	static final int MSG_REFRESH = 0x101;
	static final int MSG_RUNNEXT = 0x102;
	static final int MSG_FINISH = 0x103;

	public class RefreshHandler extends Handler {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_REFRESH) {
				adapter.setItems(ModuleTestApplication.getInstance().getItem());
				adapter.setStates(ModuleTestApplication.getInstance().getListViewState());
				adapter.setTooltip(ModuleTestApplication.getInstance().getTooltip());
				adapter.setMap(ModuleTestApplication.getInstance().getMap());
				adapter.setCount(ModuleTestApplication.getInstance().getCount());
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ModuleTestApplication.getInstance().initLog(this);
		setContentView(R.layout.nu_autotest_activity);
		// 初始化配置
		initConfig();
		// 初始化界面
		initView();
		mRefreshHandler = new RefreshHandler();

		Boolean isBootFromReceiver = this.getIntent().getBooleanExtra(IS_FROM_BOOTRECEIVER, false);
		if (isBootFromReceiver) {
			mAutoTested = true;
			char[] buffer = new char[1024];
			String szBuffer;
			int i=0, p=0, pLast = 0;
			try {
				FileReader fReader = new FileReader(FactoryResetTestActivity.AUTO_BOOT_FLAG);
				fReader.read(buffer);
				szBuffer = new String(buffer);
				while (p < szBuffer.length()) {
					if (szBuffer.charAt(p) == '\n') {
						ModuleTestApplication.getInstance().getListViewState()[i] = szBuffer.substring(pLast, p);
						pLast = p+1;
						p++;
						i++;
					}
					p++;
				}
				fReader.close();
				mRefreshHandler.sendEmptyMessage(MSG_REFRESH);
				File file = new File(FactoryResetTestActivity.AUTO_BOOT_FLAG);
				file.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			mAutoTested = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		mAutoTested = false;
		ModuleTestApplication.getInstance().resetListViewState();
		ModuleTestApplication.getInstance().resetTooltip();
		Log.i(ModuleTestApplication.TAG, "------Module Test Stopped------");
		ModuleTestApplication.getInstance().recordLog(null);
		ModuleTestApplication.getInstance().finishLog();

		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		mAutoTestThread.sendEmptyMessage(MSG_RUNNEXT);
	}

	public class AutoTestThread extends Handler implements Runnable {
		private int mIndex, mLastIndex;
		private Looper mLooper;
		private int [] mMap;
		private int mCount;

		public AutoTestThread(int index) {
			super();
			mIndex = index;
			mLastIndex = mIndex-1;
		}

		public void run() {
			Looper.prepare();
			mLooper = Looper.myLooper();
			if (mLooper == null) {
				Log.e(ModuleTestApplication.TAG, "In startAutoTest():myLooper==null");
				return;
			}

			mMap = ModuleTestApplication.getInstance().getMap();
			mCount = ModuleTestApplication.getInstance().getCount();
			while (mIndex < mCount) {
				while (mLastIndex == mIndex) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				mLastIndex = mIndex;
				if (mIndex < mCount) runNext();
			}
		}

		public void runNext() {
			if (!ModuleTestApplication.getInstance().getSelected()[mMap[mIndex]]) return;

			if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.audio_test))) {
//				Intent intentAudio = new Intent(NuAutoTestActivity.this, AudioTestActivity.class);
//				intentAudio.putExtra("Auto", true);
//				startActivityForResult(intentAudio, 5);
//				AudioTestActivity activityAudio = new AudioTestActivity();
//				Thread threadRecord = new Thread(activityAudio.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadRecord.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.gsensor_test))) {
				GSensorTestActivity activityGSensor = new GSensorTestActivity();
				Thread threadSpeed = new Thread(activityGSensor.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadSpeed.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.lightsensor_test))) {
				LightSensorTestActivity activityLight = new LightSensorTestActivity();
				Thread threadLight = new Thread(activityLight.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadLight.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.compass_test))) {
				CompassTestActivity activityCompass = new CompassTestActivity();
				Thread threadCompass = new Thread(activityCompass.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadCompass.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.battery_test))) {
				BatteryTestActivity activityBattery = new BatteryTestActivity();
				Thread threadBattery = new Thread(activityBattery.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadBattery.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.backlight_test))) {
//				Intent intentBackLight = new Intent(NuAutoTestActivity.this, BackLightTestActivity.class);
//				intentBackLight.putExtra("Auto", true);
//				startActivityForResult(intentBackLight, 5);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.button_test))) {
//				Intent intentButton = new Intent(NuAutoTestActivity.this, ButtonTestActivity.class);
//				intentButton.putExtra("Auto", true);
//				startActivityForResult(intentButton, 6);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.front_camera_test))) {
//				Intent intentFrontCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
//				intentFrontCamera.putExtra("Flag", "Front");
//				intentFrontCamera.putExtra("Auto", true);
//				startActivityForResult(intentFrontCamera, 7);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.backlight_test))) {
//				Intent intentBackCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
//				intentBackCamera.putExtra("Flag", "Back");
//				intentBackCamera.putExtra("Auto", true);
//				startActivityForResult(intentBackCamera, 8);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.usb_test))) {
//				USBTestActivity activityUSB = new USBTestActivity();
//				Thread threadUSB = new Thread(activityUSB.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadUSB.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.hdmi_test))) {
//				Intent intentHDMI = new Intent(NuAutoTestActivity.this, HDMITestActivity.class);
//				intentHDMI.putExtra("Auto", true);
//				startActivityForResult(intentHDMI, 10);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.wifi_test))) {
				WifiTestActivity activityWifi = new WifiTestActivity();
				Thread threadWifi = new Thread(activityWifi.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadWifi.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.bluetooth_test))) {
				BlueToothTestActivity activityBT = new BlueToothTestActivity();
				Thread threadBT = new Thread(activityBT.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadBT.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.gps_test))) {
				GPSTestActivity activityGPS = new GPSTestActivity();
				Thread threadGPS = new Thread(activityGPS.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadGPS.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.sd_test))) {
//				SDTestActivity activitySD = new SDTestActivity();
//				Thread threadSD = new Thread(activitySD.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadSD.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.charger_test))) {
//				ChargerTestActivity activityCharger = new ChargerTestActivity();
//				Thread threadCharger = new Thread(activityCharger.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadCharger.run();
//				Intent intentCharger = new Intent(NuAutoTestActivity.this, ChargerTestActivity.class);
//				intentCharger.putExtra("Auto", true);
//				startActivityForResult(intentCharger, 15);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.tp_test))) {
//				Intent intentTP = new Intent(NuAutoTestActivity.this, TPTestActivity.class);
//				intentTP.putExtra("Auto", true);
//				startActivityForResult(intentTP, 16);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.vibrator_test))) {
//				Intent intentVibrator = new Intent(NuAutoTestActivity.this, VibratorTestActivity.class);
//				intentVibrator.putExtra("Auto", true);
//				startActivityForResult(intentVibrator, 17);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.lcd_test))) {
//				Intent intentLCD = new Intent(NuAutoTestActivity.this, LCDTestActivity.class);
//				intentLCD.putExtra("Auto", true);
//				startActivityForResult(intentLCD, 18);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.headset_test))) {
//				HeadsetTestActivity activityHeadset = new HeadsetTestActivity();
//				Thread threadHeadset = new Thread(activityHeadset.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadHeadset.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.flashlight_test))) {
//				Intent intentFlashlight = new Intent(NuAutoTestActivity.this, FlashlightTestActivity.class);
//				intentFlashlight.putExtra("Auto", true);
//				startActivityForResult(intentFlashlight, 20);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.suspendresume_test))) {
//				SuspendResumeTestActivity activitySuspendResume = new SuspendResumeTestActivity();
//				Thread threadSuspendResume = new Thread(activitySuspendResume.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadSuspendResume.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.factoryreset_test))) {
//				FactoryResetTestActivity activityFactoryReset = new FactoryResetTestActivity();
//				Thread threadFactoryReset = new Thread(activityFactoryReset.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadFactoryReset.run();
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.phone_test))) {
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.fm_test))) {
//				Intent intentFM = new Intent(NuAutoTestActivity.this, FMTestActivity.class);
//				intentFM.putExtra("Auto", true);
//				startActivityForResult(intentFM, 24);
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.led_test))) {
			} else if (ModuleTestApplication.items[mMap[mIndex]].equals(getString(R.string.proximitysensor_test))) {
				ProximitySensorTestActivity activityProx = new ProximitySensorTestActivity();
				Thread threadProx = new Thread(activityProx.
						new AutoTestThread(NuAutoTestActivity.this,
						ModuleTestApplication.getInstance(), mRefreshHandler));
				threadProx.run();
			}
			this.sendEmptyMessage(MSG_RUNNEXT);
		}

		public void handleMessage(Message msg) {
			if (msg.what == MSG_RUNNEXT) {
				mIndex++;
			} else if (msg.what == MSG_FINISH) {
				mIndex = mCount;
			}
		}
	}

	public void initConfig() {
		boolean[] select = ModuleTestApplication.getInstance().getSelected();
		int[] map = ModuleTestApplication.getInstance().getMap();
		int current = 0, i, j;

		for (i=0; i<select.length; i++) select[i] = false;

		try {
			AssetManager config = getAssets();
			InputStream iStream = config.open("config.ini");
			byte[] buffer = new byte[1024];
			String string, section;
			int secIndex=-1;
			String[] lines;
//			Pattern patternSection, patternValue;
//			Matcher matcher;

//			patternSection = Pattern.compile("\\[\\S*\\]");
//			patternValue = Pattern.compile("\\S+=\\S*");
			iStream.read(buffer, 0, buffer.length);
			string = new String(buffer);
			string = string.trim();
			lines = string.split("\\n");
			for (i=0; i<lines.length; i++) {
				if (lines[i].split(";").length == 0) continue;
				lines[i] = lines[i].split(";")[0];
				lines[i] = lines[i].trim();
				if (lines[i].matches("\\[[\\S\\s]+\\]")) {
					secIndex = -1;
					if (lines[i].split("\\[").length > 1) {
						section = lines[i].split("\\[")[1];
						section = section.trim();
					} else {
						secIndex = -1;
						continue;
					}
					section = section.split("\\]")[0];
					section = section.trim();
					for (j=0; j< ModuleTestApplication.items.length; j++)
						if (ModuleTestApplication.items[j].equals(section)) {
							secIndex = j;
							break;
						}
					if (j == ModuleTestApplication.items.length) secIndex = -1;
				} else if (lines[i].matches("\\S+=\\S*")) {
					if (secIndex == -1) continue;
					String[] strs = lines[i].split("=");
					if (strs.length > 1) {
						strs[0] = strs[0].trim();
						if (strs[0].equals("use")) {
							strs[1] = strs[1].trim();
							if (strs[1].equals("1")) {
								select[secIndex] = true;
								map[current] = secIndex;
								current++;
							}
							else
								select[secIndex] = false;
						} else if (strs[0].equals("rotation") &&
								ModuleTestApplication.items[secIndex].equals("前摄头")) {
							strs[1] = strs[1].trim();
							CameraTestActivity.mRotationFront = Integer.parseInt(strs[1]);
						} else if (strs[0].equals("rotation") &&
								ModuleTestApplication.items[secIndex].equals("后摄头")) {
							strs[1] = strs[1].trim();
							CameraTestActivity.mRotationBack = Integer.parseInt(strs[1]);
						}
					}
				}
			}

//			for (i=0; i<map.length; i++)
//				if (select[i]) {
//					map[current] = i;
//					current++;
//				}
			ModuleTestApplication.getInstance().setSelected(select);
			ModuleTestApplication.getInstance().setMap(map);
			ModuleTestApplication.getInstance().setCount(current);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void initView() {
		adapter = new NuAutoTestAdapter(NuAutoTestActivity.this);
		adapter.setItems(ModuleTestApplication.getInstance().getItem());
		adapter.setStates(ModuleTestApplication.getInstance().getListViewState());
		adapter.setTooltip(ModuleTestApplication.getInstance().getTooltip());
		adapter.setMap(ModuleTestApplication.getInstance().getMap());
		adapter.setCount(ModuleTestApplication.getInstance().getCount());
		adapter.notifyDataSetChanged();

		Button btAutoTest = (Button) this.findViewById(R.id.btAutotest);
		btAutoTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mAutoTested) {
					Thread thread;
					mAutoTestThread = new AutoTestThread(0);
					thread = new Thread(mAutoTestThread);
					thread.start();
					mAutoTested = true;
				}
			}
		});

		Button btDeviceInfo = (Button)this.findViewById(R.id.btDeviceInfo);
		btDeviceInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(NuAutoTestActivity.this, DeviceInfoActivity.class));
			}
		});

		GridView gridview = (GridView) this.findViewById(R.id.gridView);
		gridview.setNumColumns(5);
		gridview.setAdapter(adapter);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				int index = ModuleTestApplication.getInstance().getMap()[arg2];
				if (ModuleTestApplication.items[index].equals(getString(R.string.audio_test))) {
					// 音频测试
					startActivity(new Intent(NuAutoTestActivity.this, AudioTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.gsensor_test))) {
					// 加速传感器测试
					startActivity(new Intent(NuAutoTestActivity.this, GSensorTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.lightsensor_test))) {
					// 光强度传感器测试
					startActivity(new Intent(NuAutoTestActivity.this, LightSensorTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.compass_test))) {
					// 指南针测试
					startActivity(new Intent(NuAutoTestActivity.this, CompassTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.battery_test))) {
					// 电池测试
					startActivity(new Intent(NuAutoTestActivity.this, BatteryTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.backlight_test))) {
					// 背光测试
					startActivity(new Intent(NuAutoTestActivity.this, BackLightTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.button_test))) {
					// 按键测试
					startActivity(new Intent(NuAutoTestActivity.this, ButtonTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.front_camera_test))) {
					// 前置摄像头测试
					Intent intentFrontCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
					intentFrontCamera.putExtra("Flag", "Front");
					startActivity(intentFrontCamera);
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.back_camera_test))) {
					// 后置摄像头测试
					Intent intentBackCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
					intentBackCamera.putExtra("Flag", "Back");
					startActivity(intentBackCamera);
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.usb_test))) {
					// USB测试
					startActivity(new Intent(NuAutoTestActivity.this, USBTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.hdmi_test))) {
					// HDMI测试
					startActivity(new Intent(NuAutoTestActivity.this, HDMITestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.wifi_test))) {
					// WIFI测试
					startActivity(new Intent(NuAutoTestActivity.this, WifiTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.bluetooth_test))) {
					// 蓝牙测试
					startActivity(new Intent(NuAutoTestActivity.this, BlueToothTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.gps_test))) {
					// GPS测试
					startActivity(new Intent(NuAutoTestActivity.this, GPSTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.sd_test))) {
					// SD卡测试
					startActivity(new Intent(NuAutoTestActivity.this, SDTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.charger_test))) {
					// 充电器测试
					startActivity(new Intent(NuAutoTestActivity.this, ChargerTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.tp_test))) {
					// TP测试
					startActivity(new Intent(NuAutoTestActivity.this, TPTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.vibrator_test))) {
					// Vibrator测试
					startActivity(new Intent(NuAutoTestActivity.this, VibratorTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.lcd_test))) {
					// LCD测试
					startActivity(new Intent(NuAutoTestActivity.this, LCDTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.headset_test))) {
					// 耳机测试
					startActivity(new Intent(NuAutoTestActivity.this, HeadsetTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.flashlight_test))) {
					// 闪光灯测试
					startActivity(new Intent(NuAutoTestActivity.this, FlashlightTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.suspendresume_test))) {
					// 休眠唤醒测试
					startActivity(new Intent(NuAutoTestActivity.this, SuspendResumeTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.factoryreset_test))) {
					// 恢复出厂设置
					startActivity(new Intent(NuAutoTestActivity.this, FactoryResetTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.phone_test))) {
					// 电话录音测试
					startActivity(new Intent(NuAutoTestActivity.this, PhoneTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.fm_test))) {
					// 收音机测试
					startActivity(new Intent(NuAutoTestActivity.this, FMTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.led_test))) {
					// LED测试
					startActivity(new Intent(NuAutoTestActivity.this, LEDTestActivity.class));
				} else if (ModuleTestApplication.items[index].equals(getString(R.string.proximitysensor_test))) {
					// 距离传感器测试
					startActivity(new Intent(NuAutoTestActivity.this, ProximitySensorTestActivity.class));
				}
			}
		});
	}

	@Override
	public void onBackPressed() {
		if (mAutoTestThread != null) mAutoTestThread.sendEmptyMessage(MSG_FINISH);

		super.onBackPressed();
	}
}