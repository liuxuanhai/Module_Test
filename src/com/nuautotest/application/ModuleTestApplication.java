package com.nuautotest.application;

import android.app.Application;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;
import com.nuautotest.NativeLib.RootCommand;

import java.io.*;
import java.util.ArrayList;

/**
 * 全局变量listViewState
 *
 * @author xu-liang & xie-hang
 *
 */

public class ModuleTestApplication extends Application {
	/* Log tag for the whole project */
	public static final String TAG = "ModuleTest";

	/* Control whether the log is enable for the whole project */
	public static boolean LOG_ENABLE = false;
	public static String LOG_DIR;
	private static FileWriter mLogWriter;
	private ArrayList<String> mWriteLog;
	private ArrayList<String> mClearLog;

	/* class RootCommand for read/write command as root */
	private static RootCommand rootcmd;

	public static RootCommand getRootcmd() {
		if (rootcmd == null)
			rootcmd = new RootCommand();
		return rootcmd;
	}

	/* Static instance for this Application */
	private static ModuleTestApplication instance;

	public static ModuleTestApplication getInstance() {
		if (instance == null)
			instance = new ModuleTestApplication();
		return instance;
	}

	ModuleTestApplication() {
		instance = this;
		rootcmd = new RootCommand();
	}

	public void initLog(ContextWrapper cw) {
		try {
			File logdir = cw.getFilesDir();
			if (logdir == null)
				logdir = Environment.getExternalStorageDirectory();
			LOG_DIR = logdir.getAbsolutePath();
			Log.d(TAG, "LOG_DIR = " + LOG_DIR);

			if (!LOG_ENABLE) return;

			File logout = new File(LOG_DIR + "/ModuleTest");
			if (!logout.exists()) {
				logout.mkdirs();
			}
			mLogWriter = new FileWriter(LOG_DIR + "/ModuleTest/log_all.txt");
			mWriteLog = new ArrayList<String>();
			mClearLog = new ArrayList<String>();
			mWriteLog.add("logcat");
			mWriteLog.add("-d");
			mClearLog.add("logcat");
			mClearLog.add("-c");

			Runtime.getRuntime().exec(mClearLog.toArray(new String[mClearLog.size()]));
			Log.i("ModuleTest", "------Module Test Started------");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void recordLog(FileWriter singleWriter) {
		if (!LOG_ENABLE) return;
		Process process;
		try {
			process = Runtime.getRuntime().exec(mWriteLog.toArray(new String[mWriteLog.size()]));
			Runtime.getRuntime().exec(mClearLog.toArray(new String[mClearLog.size()]));
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str;
			while ((str=bufferedReader.readLine()) != null) {
				if (singleWriter != null) {
					singleWriter.write(str);
					singleWriter.append('\n');
				}
				mLogWriter.write(str);
				mLogWriter.append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finishLog() {
		if (!LOG_ENABLE) return;
		try {
			if (mLogWriter != null) mLogWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
