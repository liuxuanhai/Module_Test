package com.nuautotest.application;

import android.app.Application;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;
import com.nuautotest.Activity.R;

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
	public static final boolean LOG_ENABLE = false;
	public static String LOG_DIR;

	public enum TestState {
		TEST_STATE_NONE,
		TEST_STATE_ON_GOING,
		TEST_STATE_SUCCESS,
		TEST_STATE_FAIL,
		TEST_STATE_TIME_OUT,
	}

	/* Static instance for this Application */
	public static ModuleTestApplication instance;

	/*
	 * Define the number of tests and how many
	 * auto tests are there in the head of item[]
	 *
	 * These should be as same as
	 *  #res/values/strings.xml
	 *  #assets/config.ini
	 */
	public static final int numberOfTest = 27;
	public static final int numberOfAutoTest = 8;

	static public String[] items = {
			"加速传感", "光传感", "距离传感", "电池",
			"指南针", "WIFI", "蓝牙", "GPS",
			"充电器", "USB", "SD卡", "耳机",
			"音频", "背光", "按键", "前摄头",
			"后摄头", "闪光灯", "HDMI", "触屏",
			"震动", "LCD", "休眠唤醒", "电话",
			"出厂设置", "收音机", "LED" };

	public String[] listViewState={
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试"};

	public String[] tooltip = {
			"", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", ""
	};

	public boolean[] selected = new boolean[numberOfTest];
	public int[] map = new int[numberOfTest];
	public int count = numberOfTest;
	public int successCount = 0;

	static FileWriter mLogWriter;
	ArrayList<String> mWriteLog;
	ArrayList<String> mClearLog;

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return this.count;
	}

	public void setMap(int[] map) {
		this.map = map;
	}

	public int[] getMap() {
		return this.map;
	}

	public boolean[] getSelected() {
		return selected;
	}

	public void setSelected(boolean[] selected) {
		this.selected = selected;
	}

	public String[] getItem() {
		return items;
	}

	public String[] getListViewState() {
		return listViewState;
	}

	public void setListViewState(String[] listViewState) {
		this.listViewState = listViewState;
	}

	public TestState getTestState(String name) {
		if (listViewState[getIndex(name)].equals("未测试"))
			return TestState.TEST_STATE_NONE;
		else if (listViewState[getIndex(name)].equals("测试中"))
			return TestState.TEST_STATE_ON_GOING;
		else if (listViewState[getIndex(name)].equals("成功"))
			return TestState.TEST_STATE_SUCCESS;
		else if (listViewState[getIndex(name)].equals("失败"))
			return TestState.TEST_STATE_FAIL;
		else if (listViewState[getIndex(name)].equals("操作超时"))
			return TestState.TEST_STATE_TIME_OUT;
		return TestState.TEST_STATE_NONE;
	}

	public void setTestState(String name, TestState state) {
		int index = getIndex(name);
		switch (state) {
			case TEST_STATE_NONE:
				if (listViewState[index].equals("成功"))
					successCount--;
				listViewState[index] = "未测试";
				break;
			case TEST_STATE_ON_GOING:
				if (listViewState[index].equals("成功"))
					successCount--;
				listViewState[index] = "测试中";
				break;
			case TEST_STATE_SUCCESS:
				if (!listViewState[index].equals("成功")) {
					successCount++;
					Log.d(TAG, name + " test succeeded");
					if (successCount == count-1) setMiscFlag();
				}
				listViewState[index] = "成功";
				break;
			case TEST_STATE_FAIL:
				if (listViewState[index].equals("成功"))
					successCount--;
				listViewState[index] = "失败";
				break;
			case TEST_STATE_TIME_OUT:
				if (listViewState[index].equals("成功"))
					successCount--;
				listViewState[index] = "操作超时";
				break;
		}
	}

	public String[] getTooltip() {
		return tooltip;
	}

	public void setTooltip(String[] tooltip) {
		this.tooltip = tooltip;
	}

	public int getIndex(String name) {
		for (int i=0; i<numberOfTest; i++)
			if (items[i].equals(name)) return i;
		return -1;
	}

	public void resetListViewState() {
		for (int i=0; i<numberOfTest; i++) {
			listViewState[i] = "未测试";
		}
	}

	public void resetTooltip() {
		for (int i=0; i<numberOfTest; i++) {
			tooltip[i] = "";
		}
	}

	protected void setMiscFlag() {
		File f = new File("/misc/pcba_apk_test");
		boolean flag = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;

			while ((line = br.readLine()) != null) {
				if (line.equals("apk_ok")) {
					flag = true;
					break;
				}
			}
		} catch (Exception ignored) {
		}

		try {
			if (!flag) {
				FileWriter fw = new FileWriter(f, true);
				fw.write("apk_ok\n");
				fw.flush();
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ModuleTestApplication getInstance() {
		if (instance == null)
			instance = new ModuleTestApplication();
		return instance;
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
