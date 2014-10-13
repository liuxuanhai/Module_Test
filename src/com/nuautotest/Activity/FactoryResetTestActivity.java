package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.nuautotest.application.ModuleTestApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 恢复出厂设置
 *
 * @author xie-hang
 *
 */

public class FactoryResetTestActivity extends Activity {
	public static final String AUTO_BOOT_FLAG = "/sdcard/Module_Test_autobootflag";
	public static final String BOOT_CONFIG = "/misc/boot_config";
	private PowerManager mPowerManager;
	private ModuleTestApplication application;

	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factory_reset_test);
		mContext = this;

		Button btStart = (Button) this.findViewById(R.id.btRebootStart);

		btStart.setOnClickListener(new StartButtonListener());

		initCreate();
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_reboot.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Reboot Test---");

		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.factoryreset_test))]="失败";
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.factoryreset_test))]="成功";
				this.finish();
				break;
		}
	}

	public void StartTest() {
//		File autobootFlag = new File(AUTO_BOOT_FLAG);
		File bootconfigFlag = new File(BOOT_CONFIG);
		try {
//			autobootFlag.createNewFile();
			if (bootconfigFlag.exists()) {
				bootconfigFlag.delete();
				bootconfigFlag.createNewFile();
			}
		} catch(IOException e) {
			Log.e(ModuleTestApplication.TAG, "Create autobootflag ERROR!");
			return;
		}

		FileWriter fWriter;
		application = ModuleTestApplication.getInstance();

		try {
//			fWriter = new FileWriter(autobootFlag);
//			for (int i=0; i<ModuleTestApplication.numberOfTest; i++) {
//				if (application.getIndex(getString(R.string.factoryreset_test)) == i)
//					fWriter.write("成功\n");
//				else
//					fWriter.write(application.getListViewState()[i]+"\n");
//			}
//			fWriter.close();

			fWriter = new FileWriter(bootconfigFlag);
			fWriter.write("Boot system = android;\n");
			fWriter.write("Boot device = emmc;\n");
			fWriter.write("Clear data = yes;\n");
			fWriter.write("Clear cache = yes;\n");
			fWriter.write("OTA decide = no;\n");
			fWriter.write("Update from SDCard = no;\n");
			fWriter.write("bp calibration = no;\n");
			fWriter.write("shutdown = yes;\n");
			fWriter.close();
		} catch (IOException e) {
			Log.e(ModuleTestApplication.TAG, "Write autobootflag ERROR!");
			return;
		}

//		final PackageManager pm = getPackageManager();
//        List<ApplicationInfo> packages = pm.getInstalledApplications(
//                PackageManager.GET_META_DATA);
//        for (ApplicationInfo packageInfo : packages)
//           Log.i(ModuleTestApplication.TAG, "pachageName: "+packageInfo.packageName + "   UID: " + packageInfo.uid);
//
//		NativeLib nativeLib = new NativeLib();
//		File f = new File("/init.rc");
//		Log.i(ModuleTestApplication.TAG, "rtc0 canRead: "+f.canRead());
//		Log.i(ModuleTestApplication.TAG, "rtc0 canWrite: "+f.canWrite());
//		int ret = nativeLib.power_on_off();
//		Log.i(ModuleTestApplication.TAG, "power_on_off returned: "+ret);

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		mPowerManager.reboot(null);
	}

	public class StartButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			StartTest();
		}
	}

	@Override
	public void onBackPressed() {
	}

	public void startAutoTest() {
		initCreate();
		application.getListViewState()[application.getIndex(mContext.getString(R.string.factoryreset_test))]="测试中";
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		StartTest();
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
		}
	}
}