package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

/**
 * 测试界面
 *
 * @author xie-hang
 *
 */

public class NuAutoTestActivity extends Activity {
	private static NuAutoTestActivity instance;
	private NuAutoTestAdapter adapter;
	private boolean mAutoTested;
	private AutoTestThread mAutoTestThread;
	public Handler mRefreshHandler;
	static private boolean isPCBA = false, isAndroid = false;
	static final int MSG_REFRESH = 0x101;
	static final int MSG_RUNNEXT = 0x102;
	static final int MSG_FINISH = 0x103;

	static final String MODE_PCBA = "pcba";
	static final String Mode_ANDROID = "android";

	public NuAutoTestActivity() {
		super();
		instance = this;
	}

	public class RefreshHandler extends Handler {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_REFRESH) {
				adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isPCBA |= (this.getIntent().getIntExtra(MODE_PCBA, 0) == 1);
		isAndroid |= (this.getIntent().getIntExtra(Mode_ANDROID, 0) == 1);
		if (!isPCBA && !isAndroid) {
			startActivity(new Intent(this, ModeSelectActivity.class));
			this.finish();
		}

		ModuleTestApplication.getInstance().initLog(this);
		setContentView(R.layout.nu_autotest_activity);
		// 初始化界面
		initView();
		mRefreshHandler = new RefreshHandler();
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
		adapter.resetListViewState();
		Log.i(ModuleTestApplication.TAG, "------Module Test Stopped------");
		ModuleTestApplication.getInstance().recordLog(null);
		ModuleTestApplication.getInstance().finishLog();
		isPCBA = isAndroid = false;

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

			mCount = adapter.getCount();
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
			String item = adapter.getItem(mIndex);

//			if (item.equals(getString(R.string.audio_test))) {
//				Intent intentAudio = new Intent(NuAutoTestActivity.this, AudioTestActivity.class);
//				intentAudio.putExtra("Auto", true);
//				startActivityForResult(intentAudio, 5);
//				AudioTestActivity activityAudio = new AudioTestActivity();
//				Thread threadRecord = new Thread(activityAudio.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadRecord.run();
//			} else
			if (item.equals(getString(R.string.gsensor_test))) {
				GSensorTestActivity activityGSensor = new GSensorTestActivity();
				Thread threadSpeed = new Thread(activityGSensor.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadSpeed.run();
			} else if (item.equals(getString(R.string.lightsensor_test))) {
				LightSensorTestActivity activityLight = new LightSensorTestActivity();
				Thread threadLight = new Thread(activityLight.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadLight.run();
			} else if (item.equals(getString(R.string.compass_test))) {
				CompassTestActivity activityCompass = new CompassTestActivity();
				Thread threadCompass = new Thread(activityCompass.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadCompass.run();
			} else if (item.equals(getString(R.string.battery_test))) {
				BatteryTestActivity activityBattery = new BatteryTestActivity();
				Thread threadBattery = new Thread(activityBattery.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadBattery.run();
//			} else if (item.equals(getString(R.string.backlight_test))) {
//				Intent intentBackLight = new Intent(NuAutoTestActivity.this, BackLightTestActivity.class);
//				intentBackLight.putExtra("Auto", true);
//				startActivityForResult(intentBackLight, 5);
//			} else if (item.equals(getString(R.string.button_test))) {
//				Intent intentButton = new Intent(NuAutoTestActivity.this, ButtonTestActivity.class);
//				intentButton.putExtra("Auto", true);
//				startActivityForResult(intentButton, 6);
//			} else if (item.equals(getString(R.string.front_camera_test))) {
//				Intent intentFrontCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
//				intentFrontCamera.putExtra("Flag", "Front");
//				intentFrontCamera.putExtra("Auto", true);
//				startActivityForResult(intentFrontCamera, 7);
//			} else if (item.equals(getString(R.string.backlight_test))) {
//				Intent intentBackCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
//				intentBackCamera.putExtra("Flag", "Back");
//				intentBackCamera.putExtra("Auto", true);
//				startActivityForResult(intentBackCamera, 8);
//			} else if (item.equals(getString(R.string.usb_test))) {
//				USBTestActivity activityUSB = new USBTestActivity();
//				Thread threadUSB = new Thread(activityUSB.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadUSB.run();
//			} else if (item.equals(getString(R.string.hdmi_test))) {
//				Intent intentHDMI = new Intent(NuAutoTestActivity.this, HDMITestActivity.class);
//				intentHDMI.putExtra("Auto", true);
//				startActivityForResult(intentHDMI, 10);
			} else if (item.equals(getString(R.string.wifi_test))) {
				WifiTestActivity activityWifi = new WifiTestActivity();
				Thread threadWifi = new Thread(activityWifi.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadWifi.run();
			} else if (item.equals(getString(R.string.bluetooth_test))) {
				BlueToothTestActivity activityBT = new BlueToothTestActivity();
				Thread threadBT = new Thread(activityBT.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadBT.run();
			} else if (item.equals(getString(R.string.gps_test))) {
				GPSTestActivity activityGPS = new GPSTestActivity();
				Thread threadGPS = new Thread(activityGPS.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
				threadGPS.run();
//			} else if (item.equals(getString(R.string.sd_test))) {
//				SDTestActivity activitySD = new SDTestActivity();
//				Thread threadSD = new Thread(activitySD.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadSD.run();
//			} else if (item.equals(getString(R.string.charger_test))) {
//				ChargerTestActivity activityCharger = new ChargerTestActivity();
//				Thread threadCharger = new Thread(activityCharger.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadCharger.run();
//				Intent intentCharger = new Intent(NuAutoTestActivity.this, ChargerTestActivity.class);
//				intentCharger.putExtra("Auto", true);
//				startActivityForResult(intentCharger, 15);
//			} else if (item.equals(getString(R.string.tp_test))) {
//				Intent intentTP = new Intent(NuAutoTestActivity.this, TPTestActivity.class);
//				intentTP.putExtra("Auto", true);
//				startActivityForResult(intentTP, 16);
//			} else if (item.equals(getString(R.string.vibrator_test))) {
//				Intent intentVibrator = new Intent(NuAutoTestActivity.this, VibratorTestActivity.class);
//				intentVibrator.putExtra("Auto", true);
//				startActivityForResult(intentVibrator, 17);
//			} else if (item.equals(getString(R.string.lcd_test))) {
//				Intent intentLCD = new Intent(NuAutoTestActivity.this, LCDTestActivity.class);
//				intentLCD.putExtra("Auto", true);
//				startActivityForResult(intentLCD, 18);
//			} else if (item.equals(getString(R.string.headset_test))) {
//				HeadsetTestActivity activityHeadset = new HeadsetTestActivity();
//				Thread threadHeadset = new Thread(activityHeadset.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadHeadset.run();
//			} else if (item.equals(getString(R.string.flashlight_test))) {
//				Intent intentFlashlight = new Intent(NuAutoTestActivity.this, FlashlightTestActivity.class);
//				intentFlashlight.putExtra("Auto", true);
//				startActivityForResult(intentFlashlight, 20);
//			} else if (item.equals(getString(R.string.suspendresume_test))) {
//				SuspendResumeTestActivity activitySuspendResume = new SuspendResumeTestActivity();
//				Thread threadSuspendResume = new Thread(activitySuspendResume.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadSuspendResume.run();
//			} else if (item.equals(getString(R.string.factoryreset_test))) {
//				FactoryResetTestActivity activityFactoryReset = new FactoryResetTestActivity();
//				Thread threadFactoryReset = new Thread(activityFactoryReset.
//						new AutoTestThread(NuAutoTestActivity.this,
//						ModuleTestApplication.getInstance(), mRefreshHandler));
//				threadFactoryReset.run();
//			} else if (item.equals(getString(R.string.phone_test))) {
//			} else if (item.equals(getString(R.string.fm_test))) {
//				Intent intentFM = new Intent(NuAutoTestActivity.this, FMTestActivity.class);
//				intentFM.putExtra("Auto", true);
//				startActivityForResult(intentFM, 24);
//			} else if (item.equals(getString(R.string.led_test))) {
			} else if (item.equals(getString(R.string.proximitysensor_test))) {
				ProximitySensorTestActivity activityProx = new ProximitySensorTestActivity();
				Thread threadProx = new Thread(activityProx.
						new AutoTestThread(NuAutoTestActivity.this,
						mRefreshHandler));
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

	public void setMode(String mode) {
		if (mode.equals(MODE_PCBA)) isPCBA = true;
		else isAndroid = true;
	}

	public void initView() {
		if (isPCBA)
			this.setTitle(getString(R.string.app_name_pcba)+" "+getString(R.string.version));
		else if (isAndroid)
			this.setTitle(getString(R.string.app_name)+" "+getString(R.string.version));

		adapter = new NuAutoTestAdapter(NuAutoTestActivity.this, isPCBA);

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

		GridView gridview = (GridView)this.findViewById(R.id.gridView);
		gridview.setAdapter(adapter);
	}

	public class TestItemOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Button btTest = (Button)v;
			if (btTest.getText().equals(getString(R.string.audio_test))) {
				// 音频测试
				startActivity(new Intent(NuAutoTestActivity.this, AudioTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.gsensor_test))) {
				// 加速传感器测试
				startActivity(new Intent(NuAutoTestActivity.this, GSensorTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.lightsensor_test))) {
				// 光强度传感器测试
				startActivity(new Intent(NuAutoTestActivity.this, LightSensorTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.compass_test))) {
				// 指南针测试
				startActivity(new Intent(NuAutoTestActivity.this, CompassTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.battery_test))) {
				// 电池测试
				startActivity(new Intent(NuAutoTestActivity.this, BatteryTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.backlight_test))) {
				// 背光测试
				startActivity(new Intent(NuAutoTestActivity.this, BackLightTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.button_test))) {
				// 按键测试
				startActivity(new Intent(NuAutoTestActivity.this, ButtonTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.front_camera_test))) {
				// 前置摄像头测试
				Intent intentFrontCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
				intentFrontCamera.putExtra("Flag", "Front");
				startActivity(intentFrontCamera);
			} else if (btTest.getText().equals(getString(R.string.back_camera_test))) {
				// 后置摄像头测试
				Intent intentBackCamera = new Intent(NuAutoTestActivity.this, CameraTestActivity.class);
				intentBackCamera.putExtra("Flag", "Back");
				startActivity(intentBackCamera);
			} else if (btTest.getText().equals(getString(R.string.usb_test))) {
				// USB测试
				startActivity(new Intent(NuAutoTestActivity.this, USBTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.hdmi_test))) {
				// HDMI测试
				startActivity(new Intent(NuAutoTestActivity.this, HDMITestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.wifi_test))) {
				// WIFI测试
				startActivity(new Intent(NuAutoTestActivity.this, WifiTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.bluetooth_test))) {
				// 蓝牙测试
				startActivity(new Intent(NuAutoTestActivity.this, BlueToothTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.gps_test))) {
				// GPS测试
				startActivity(new Intent(NuAutoTestActivity.this, GPSTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.sd_test))) {
				// SD卡测试
				startActivity(new Intent(NuAutoTestActivity.this, SDTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.charger_test))) {
				// 充电器测试
				startActivity(new Intent(NuAutoTestActivity.this, ChargerTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.tp_test))) {
				// TP测试
				startActivity(new Intent(NuAutoTestActivity.this, TPTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.vibrator_test))) {
				// Vibrator测试
				startActivity(new Intent(NuAutoTestActivity.this, VibratorTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.lcd_test))) {
				// LCD测试
				startActivity(new Intent(NuAutoTestActivity.this, LCDTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.headset_test))) {
				// 耳机测试
				startActivity(new Intent(NuAutoTestActivity.this, HeadsetTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.flashlight_test))) {
				// 闪光灯测试
				startActivity(new Intent(NuAutoTestActivity.this, FlashlightTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.suspendresume_test))) {
				// 休眠唤醒测试
				startActivity(new Intent(NuAutoTestActivity.this, SuspendResumeTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.factoryreset_test))) {
				// 恢复出厂设置
				startActivity(new Intent(NuAutoTestActivity.this, FactoryResetTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.phone_test))) {
				// 电话录音测试
				startActivity(new Intent(NuAutoTestActivity.this, PhoneTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.fm_test))) {
				// 收音机测试
				startActivity(new Intent(NuAutoTestActivity.this, FMTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.led_test))) {
				// LED测试
				startActivity(new Intent(NuAutoTestActivity.this, LEDTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.proximitysensor_test))) {
				// 距离传感器测试
				startActivity(new Intent(NuAutoTestActivity.this, ProximitySensorTestActivity.class));
			} else if (btTest.getText().equals(getString(R.string.test_status))) {
				// 校准/测试状态
				startActivity(new Intent(NuAutoTestActivity.this, TestStatusActivity.class));
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (mAutoTestThread != null) mAutoTestThread.sendEmptyMessage(MSG_FINISH);

		super.onBackPressed();
	}

	public static NuAutoTestActivity getInstance() {
		if (instance == null)
			instance = new NuAutoTestActivity();
		return instance;
	}
}