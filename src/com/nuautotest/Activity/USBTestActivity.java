package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;
import java.util.HashMap;

/**
 * usb测试
 *
 * @author xie-hang
 *
 */

public class USBTestActivity extends Activity

{
	private TextView mtvUsbAccessory/*, mtvUsbHost*/;
	//	private BroadcastReceiver mBcrAccessory, mBcrHost;
	private ModuleTestApplication application;
	private boolean mListening, mIsAdb;
	private Handler mUsbHandler;

	static final int MSG_HOST_IN = 0x101;
	static final int MSG_HOST_OUT = 0x102;
	static final int MSG_ACCESSORY_IN = 0x103;
	static final int MSG_ACCESSORY_OUT = 0x104;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private boolean mHostOK, mAccessoryOK;

	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.usb_test);
		mtvUsbAccessory = (TextView)findViewById(R.id.tvUsbAccessory);
//		mtvUsbHost = (TextView)findViewById(R.id.tvUsbHost);

		application = ModuleTestApplication.getInstance();
		mContext = this;

		initCreate();
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_usb.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---USB Test---");

//		IntentFilter ifAccessory = new IntentFilter();
//		ifAccessory.addAction(Intent.ACTION_UMS_CONNECTED);
//		ifAccessory.addAction(Intent.ACTION_UMS_DISCONNECTED);
//
//		mBcrAccessory = new MyAccessoryReceiver();
//		mContext.registerReceiver(mBcrAccessory, ifAccessory);
//
//		int permission =
//				mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS");
//		if (permission == PackageManager.PERMISSION_GRANTED) {
//			mIsAdb = (Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0);
//			if (mIsAdb)
//				Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
//			else {
//				Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
//				Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
//			}
//		} else {
//			if (isAutomatic) {
//				if (Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0)
//					stopAutoTest(false);
//			} else {
//				if (Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0)
//					mtvUsbAccessory.setText("USB Accessory状态:请手动关闭USB调试");
//				else
//					mtvUsbAccessory.setText("USB Accessory状态:无响应");
//			}
//		}


		mUsbHandler = new UsbHostHandler();
		mListening = true;

		Thread threadHost = new Thread(new UsbHostThread());
		threadHost.start();

		Thread threadAccessory = new Thread(new UsbAccessoryThread());
		threadAccessory.start();
	}

	public class UsbHostHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_HOST_IN:
//				mtvUsbHost.setText("USB Host状态:插入");
					break;
				case MSG_HOST_OUT:
//				mtvUsbHost.setText("USB Host状态:未插入");
					break;
				case MSG_ACCESSORY_IN:
					mtvUsbAccessory.setText("状态:插入");
					break;
				case MSG_ACCESSORY_OUT:
					mtvUsbAccessory.setText("状态:未插入");
			}
		}
	}

	public class UsbHostThread extends Thread {
		@Override
		public void run() {
			while (mListening) {
				File fDeviceDir = new File("/sys/bus/usb/devices");
				File [] fDevices = fDeviceDir.listFiles();
				File fDeviceMaxChild;
				boolean hasDevice = false;
				int i;
				for (i=0; i< (fDevices != null ? fDevices.length : 0); i++) {
					fDeviceMaxChild =
							new File("/sys/bus/usb/devices/"+fDevices[i].getName()+"/maxchild");
					if ( (fDeviceMaxChild.isFile()) && (fDeviceMaxChild.canRead()) ) {
						FileReader fReader;
						try {
							fReader = new FileReader(fDeviceMaxChild);
							char [] chBuffer = new char[1024];
							int readCount = fReader.read(chBuffer);
							fReader.close();
							String strBuffer = String.valueOf(chBuffer, 0, readCount-1);
							if (strBuffer.equals("0"))
								hasDevice = true;
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				if (hasDevice) {
					if (isAutomatic)
						mHostOK = true;
					else
						mUsbHandler.sendEmptyMessage(MSG_HOST_IN);
				} else {
					if (!isAutomatic)
						mUsbHandler.sendEmptyMessage(MSG_HOST_OUT);
				}

				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				UsbManager manager = (UsbManager)mContext.getSystemService(USB_SERVICE);

				HashMap<String,UsbDevice> device = manager.getDeviceList();

				if ( (device != null) && (device.size() != 0) ) {
					if (isAutomatic)
						mHostOK = true;
					else
						mUsbHandler.sendEmptyMessage(MSG_HOST_IN);
				} else
					mUsbHandler.sendEmptyMessage(MSG_HOST_OUT);

				try {
					Thread.sleep(100);
				} catch(InterruptedException ignored) {}
			}
		}
	}

	public class UsbAccessoryThread extends Thread {
		@Override
		public void run() {
			while (mListening) {
				File fAccessory = new File("/sys/class/android_usb/android0/state");
				if ( (fAccessory.isFile()) && (fAccessory.canRead()) ) {
					try {
						FileReader fReader = new FileReader(fAccessory);
						char [] chBuffer = new char[1024];
						int readCount = fReader.read(chBuffer);
						fReader.close();
						String strBuffer = String.valueOf(chBuffer, 0, readCount-1);
						if ( (strBuffer.equals("CONNECTED")) || (strBuffer.equals("CONFIGURED")) ) {
							if (isAutomatic)
								mAccessoryOK = true;
							else
								mUsbHandler.sendEmptyMessage(MSG_ACCESSORY_IN);
						} else {
							if (!isAutomatic)
								mUsbHandler.sendEmptyMessage(MSG_ACCESSORY_OUT);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

//	public class MyAccessoryReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (isAutomatic) {
//				if (intent.getAction().
//						equals(Intent.ACTION_UMS_CONNECTED))
//					mAccessoryOK = true;
//			} else {
//				if (intent.getAction().
//						equals(Intent.ACTION_UMS_CONNECTED)) {
//					mtvUsbAccessory.setText("USB Accessory状态:插入");
//				} else if (intent.getAction().
//						equals(Intent.ACTION_UMS_DISCONNECTED)){
//					mtvUsbAccessory.setText("USB Accessory状态:移除");
//				}
//			}
//		}
//	};

//	public class MyHostReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (intent.getAction().
//					equals(Intent.ACTION_MEDIA_MOUNTED)) {
//				String path = intent.getData().toString()
//						.substring("file://".length());
//				if (path.equals("/mnt/usbdisk"))
//					mtvUsbHost.setText("USB Host状态:插入");
//			} else if (intent.getAction().
//					equals(Intent.ACTION_MEDIA_REMOVED) ||
//					intent.getAction().
//					equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
//				String path = intent.getData().toString()
//						.substring("file://".length());
//				if (path.equals("/mnt/usbdisk"))
//					mtvUsbHost.setText("USB Host状态:未插入");
//			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
//				Log.d("XieHang", "======BCR DEVICE ATTACHED======");
//			}
//		}
//	};

	@Override
	protected void onDestroy() {
		releaseDestroy();

		super.onDestroy();
	}

	public void releaseDestroy() {
		int permission =
				mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS");
		if (permission == PackageManager.PERMISSION_GRANTED) {
			if (mIsAdb)
				Settings.Secure.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
		}

//		try {
//			mContext.unregisterReceiver(mBcrAccessory);
//			mContext.unregisterReceiver(mBcrHost);
//		} catch (IllegalArgumentException e) {
//			Log.e(ModuleTestApplication.TAG, "USBTestActivity======In onDestroy():"+e+"======");
//		}
		mListening = false;

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
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.usb_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.usb_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
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
		application.getTooltip()[application.getIndex(mContext.getString(R.string.usb_test))] = "请向device和host接口插入设备";
		application.setTestState(mContext.getString(R.string.usb_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		initCreate();
	}

	public void stopAutoTest(boolean success) {
		application.getTooltip()[application.getIndex(mContext.getString(R.string.usb_test))] = "";
		if (success)
			application.setTestState(mContext.getString(R.string.usb_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
		else
			application.setTestState(mContext.getString(R.string.usb_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
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
				if (/*mHostOK &&*/ mAccessoryOK)
					stopAutoTest(true);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				stopAutoTest(false);
				Log.e(ModuleTestApplication.TAG, "======USB Test FAILED======");
			}
		}
	}
}