package com.nuautotest.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.nuautotest.application.ModuleTestApplication;

import java.io.File;

public class BootCompleteReceiver extends BroadcastReceiver {
	static final String ACTION = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(ModuleTestApplication.TAG, "====================BOOT_COMPLETED======================");
		if (ACTION.equals(intent.getAction())) {
			File autobootFlag = new File(FactoryResetTestActivity.AUTO_BOOT_FLAG);
			if (autobootFlag.exists()) {
				Intent intentNuAutoTest = new Intent(context, NuAutoTestActivity.class);
				intentNuAutoTest.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intentNuAutoTest.putExtra(NuAutoTestActivity.IS_FROM_BOOTRECEIVER, true);
				context.startActivity(intentNuAutoTest);
			}
		}
	}
}
