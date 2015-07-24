package com.nuautotest.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.nuautotest.application.ModuleTestApplication;

class BootCompleteReceiver extends BroadcastReceiver {
//	static final String ACTION = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(ModuleTestApplication.TAG, "====================BOOT_COMPLETED======================");
	}
}
