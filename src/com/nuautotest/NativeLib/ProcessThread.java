package com.nuautotest.NativeLib;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import java.util.List;

public class ProcessThread extends Thread {

	public static final int MSG_KILLTHREAD = 0x101;
	private ActivityManager am;
	private ContextWrapper cw;
	private boolean flag = true;
	public  PTHandler handler;

	public ProcessThread(ActivityManager pam, ContextWrapper pcw) {
		super();
		handler = new PTHandler();
		am = pam;
		cw = pcw;
	}

	@Override
	public void run() {
		while (flag) {
			List<RunningAppProcessInfo> pl = am.getRunningAppProcesses();
			for (RunningAppProcessInfo info : pl) {
				if (info.processName.equals("com.android.systemui")) {
					Process.killProcess(info.pid);
					break;
				}
			}
			try {
				sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Intent intent = new Intent();
		intent.setComponent(
				new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService"));
		cw.startService(intent);
	}

	public class PTHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_KILLTHREAD) flag = false;
		}
	}
}
