package com.nuautotest.NativeLib;

import android.util.Log;
import com.nuautotest.application.ModuleTestApplication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by hangxie on 15-3-27.
 */
public class RootCommand {
	Process pSu;
	DataOutputStream mWriter;
	BufferedReader mReader;
	static boolean mCanBeEnable = true;

	public RootCommand() {
		Log.d(ModuleTestApplication.TAG, "RootCommand()");
		try {
			pSu = Runtime.getRuntime().exec("su");
			mWriter = new DataOutputStream(pSu.getOutputStream());
			mReader = new BufferedReader(new InputStreamReader(pSu.getInputStream()));
			mWriter.writeBytes("\n");
			mWriter.flush();
		} catch (Exception e) {
			e.printStackTrace();
			mCanBeEnable = false;
		}
	}

	public boolean isEnabled() {
		return mCanBeEnable;
	}

	public void Write(String cmd) {
		Log.d(ModuleTestApplication.TAG, "Write: CanBeEnable = " + mCanBeEnable);
		if (!mCanBeEnable) return;
		try {
			mWriter.writeBytes(cmd);
			mWriter.flush();
		} catch (Exception e) {
			e.printStackTrace();
			mCanBeEnable = false;
		}
	}

	public String ReadLine() {
		String ret = "";
		Log.d(ModuleTestApplication.TAG, "ReadLine: CanBeEnable = " + mCanBeEnable);
		if (!mCanBeEnable) return ret;
		try {
			ret = mReader.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			mCanBeEnable = false;
		}
		return ret;
	}
}
