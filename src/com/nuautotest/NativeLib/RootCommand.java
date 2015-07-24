package com.nuautotest.NativeLib;

import android.util.Log;
import com.nuautotest.application.ModuleTestApplication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootCommand {
	private DataOutputStream mWriter;
	private BufferedReader mReader;
	private static boolean mCanBeEnable = true;

	public RootCommand() {
		Log.d(ModuleTestApplication.TAG, "RootCommand()");
		try {
			Process pSu = Runtime.getRuntime().exec("su");
			mWriter = new DataOutputStream(pSu.getOutputStream());
			mReader = new BufferedReader(new InputStreamReader(pSu.getInputStream()));
		} catch (Exception e) {
			mCanBeEnable = false;
		}
	}

	public boolean isEnabled() {
		return mCanBeEnable;
	}

	public void Write(String cmd) throws Exception {
		if (!mCanBeEnable) return;
		try {
			mWriter.writeBytes(cmd);
			mWriter.flush();
		} catch (Exception e) {
			mCanBeEnable = false;
			throw e;
		}
	}

	public String ReadLine() throws Exception {
		String ret = "";
		if (!mCanBeEnable) return ret;
		try {
			ret = mReader.readLine();
		} catch (Exception e) {
			mCanBeEnable = false;
			throw e;
		}
		return ret;
	}
}
