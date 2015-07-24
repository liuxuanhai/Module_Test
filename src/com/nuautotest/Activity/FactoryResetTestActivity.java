package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.nuautotest.Adapter.NuAutoTestAdapter;
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
	private static final String BOOT_CONFIG = "/misc/boot_config";
	private PowerManager mPowerManager;

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

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_reboot.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Reboot Test---");

		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
	}

	private void wipeDirectory(String name) {
		File directory = new File(name);
		File[] files = directory.listFiles();

		if (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isDirectory()) wipeDirectory(file.toString());
				file.delete();
			}
		} else {
			directory.delete();
		}
	}

	void wipeSDCard() {
		File sdcard = new File(Environment.getExternalStorageDirectory().toString());
		try {
			File[] files = sdcard.listFiles();
			if (files != null && files.length > 0) {
				for (File file : files) {
					if (file.isDirectory()) wipeDirectory(file.toString());
					file.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	void StartTest() {
		File bootconfigFlag = new File(BOOT_CONFIG);
		try {
			if (bootconfigFlag.exists()) {
				bootconfigFlag.delete();
				bootconfigFlag.createNewFile();
			}
		} catch(IOException e) {
			Log.e(ModuleTestApplication.TAG, "Create autobootflag ERROR!");
			return;
		}

		FileWriter fWriter;

		try {
			/* wipe SD card */
			wipeSDCard();

			/* set boot_config */
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

	private class StartButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			StartTest();
		}
	}

	void startAutoTest() {
		initCreate();
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.factoryreset_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);

		StartTest();
	}

	private class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Handler handler) {
			super();
			mContext = context;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
		}
	}
}