package com.nuautotest.application;

import android.app.Application;
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
	public static final int numberOfTest = 25;
	public static final int numberOfAutoTest = 7;

	static public String[] items = {
			"加速传感", "光传感", "电池", "指南针",
			"WIFI", "蓝牙", "GPS", "充电器",
			"USB", "SD卡", "耳机", "音频",
			"背光", "按键", "前摄头", "后摄头",
			"闪光灯", "HDMI", "触屏", "震动",
			"LCD", "休眠唤醒", "电话", "出厂设置",
			"收音机"};

	public String[] listViewState={
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试","未测试","未测试","未测试",
			"未测试"};

	public String[] tooltip = {
			"", "", "", "", "", "", "", "", "", "",
			"", "", "", "", "", "", "", "", "", "",
			"", "", "", "", ""
	};

	public boolean[] selected = new boolean[numberOfTest];
	public int[] map = new int[numberOfTest];
	public int count = numberOfTest;

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

	public static ModuleTestApplication getInstance() {
		if (instance == null)
			instance = new ModuleTestApplication();
		return instance;
	}

	public void initLog() {
		if (!LOG_ENABLE) return;
		try {
			File logout = new File("/sdcard/ModuleTest");
			if (!logout.exists()) {
				logout.mkdirs();
			}
			mLogWriter = new FileWriter("/sdcard/ModuleTest/log_all.txt");
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
