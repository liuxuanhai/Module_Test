package com.nuautotest.Adapter;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nuautotest.Activity.CameraTestActivity;
import com.nuautotest.Activity.NuAutoTestActivity;
import com.nuautotest.Activity.R;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 测试界面Adapter
 *
 * @author xie-hang
 *
 */

public class NuAutoTestAdapter extends BaseAdapter {
	/*
	 * Define the number of tests and how many
	 * auto tests are there in the head of item[]
	 *
	 * These should be as same as
	 *  #res/values/strings.xml
	 *  #assets/config.ini
	 */
	public static final int numberOfTest = 28;

	private String[] items = {
			"加速传感", "光传感", "距离传感", "电池",
			"指南针", "WIFI", "蓝牙", "GPS",
			"充电器", "USB", "SD卡", "耳机",
			"音频", "背光", "按键", "前摄头",
			"后摄头", "闪光灯", "HDMI", "触屏",
			"震动", "LCD", "休眠唤醒", "电话",
			"出厂设置", "收音机", "LED", "设备状态" };

	public enum TestState {
		TEST_STATE_NONE,
		TEST_STATE_ON_GOING,
		TEST_STATE_SUCCESS,
		TEST_STATE_FAIL,
		TEST_STATE_TIME_OUT,
	}

	private String[] states = new String[numberOfTest];
	private int[] map = new int[numberOfTest];
	private int count = 0;
	private int needSuccessCount = 0;
	private int successCount = 0;

	private LayoutInflater inflater;
	private View[] mView = new View[numberOfTest];
	private Context mContext;
	private static NuAutoTestAdapter instance;
	private boolean isPCBA = false;

	public NuAutoTestAdapter(Context context, boolean pcba) {
		instance = this;
		mContext = context;
		isPCBA = pcba;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		resetListViewState();
		initConfig();
	}

	public static NuAutoTestAdapter getInstance() {
		return instance;
	}

	protected void initConfig() {
		int i, j;

		try {
			AssetManager config = mContext.getAssets();
			InputStream iStream = config.open("config.ini");
			byte[] buffer = new byte[1024];
			String string, section = "";
			int secIndex = -1;
			String[] lines;

			iStream.read(buffer, 0, buffer.length);
			string = new String(buffer);
			string = string.trim();
			lines = string.split("\\n");
			for (i=0; i<lines.length; i++) {
				if (lines[i].split(";").length == 0) continue;
				lines[i] = lines[i].split(";")[0];
				lines[i] = lines[i].trim();
				if (lines[i].matches("\\[[\\S\\s]+\\]")) {
					secIndex = -1;
					if (lines[i].split("\\[").length > 1) {
						section = lines[i].split("\\[")[1];
						section = section.trim();
					} else {
						secIndex = -1;
						continue;
					}
					section = section.split("\\]")[0];
					section = section.trim();
					for (j=0; j<numberOfTest; j++)
						if (items[j].equals(section)) {
							secIndex = j;
							break;
						}
				} else if (lines[i].matches("\\S+=\\S*")) {
					if (secIndex == -1) continue;
					String[] strs = lines[i].split("=");
					if (strs.length > 1) {
						strs[0] = strs[0].trim();
						if (strs[0].equals("use")) {
							strs[1] = strs[1].trim();
							if (strs[1].equals("1")) {
								if (isPCBA && (section.equals(mContext.getString(R.string.back_camera_test)) ||
										section.equals(mContext.getString(R.string.tp_test)) || section.equals(mContext.getString(R.string.lcd_test))))
									continue;
								map[count] = secIndex;
								count++;
								if (section.equals(mContext.getString(R.string.test_status)) || section.equals(mContext.getString(R.string.factoryreset_test)))
									needSuccessCount--;
							}
						} else if (strs[0].equals("rotation") && items[secIndex].equals(mContext.getString(R.string.front_camera_test))) {
							strs[1] = strs[1].trim();
							CameraTestActivity.mRotationFront = Integer.parseInt(strs[1]);
						} else if (strs[0].equals("rotation") && items[secIndex].equals(mContext.getString(R.string.back_camera_test))) {
							strs[1] = strs[1].trim();
							CameraTestActivity.mRotationBack = Integer.parseInt(strs[1]);
						}
					}
				}
			}
			needSuccessCount += count;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public int getCount() {
		return count;
	}

	public String getItem(int index) {
		return items[index];
	}

	@Override
	public long getItemId(int position) {
		return map[position];
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			mView[position] = inflater.inflate(R.layout.nu_autotest_listview_item, null);
			Button btTest = (Button)mView[position].findViewById(R.id.btTest);
			btTest.setOnClickListener(NuAutoTestActivity.getInstance().new TestItemOnClickListener());
		} else {
			mView[position] = convertView;
		}
		if (mView[position] == null) {
			Log.e(ModuleTestApplication.TAG, "getView failed");
			return null;
		}

		Button btTest = (Button)mView[position].findViewById(R.id.btTest);
		TextView tvBuAlpha = (TextView)mView[position].findViewById(R.id.tvBtAlpha);

		int height = (int)(parent.getHeight()*0.98)/((count-1)/mContext.getResources().getInteger(R.integer.gridColNumber)+1);
		tvBuAlpha.setHeight((int)(height*0.92));
		btTest.setHeight(height);
		btTest.setText(items[map[position]]);
//		btTest.setTypeface(null, Typeface.NORMAL);
		if (states[map[position]].equals("失败"))
			tvBuAlpha.setBackgroundColor(mContext.getResources().getColor(R.color.fail));
//			btTest.setTextColor(mContext.getResources().getColor(R.color.fail));
		else if (states[map[position]].equals("成功"))
			tvBuAlpha.setBackgroundColor(mContext.getResources().getColor(R.color.green));
//			btTest.setTextColor(mContext.getResources().getColor(R.color.green));
		else if (states[map[position]].equals("操作超时"))
			tvBuAlpha.setBackgroundColor(mContext.getResources().getColor(R.color.timeout));
//			btTest.setTextColor(mContext.getResources().getColor(R.color.timeout));
		else if (states[map[position]].equals("测试中")) {
			tvBuAlpha.setBackgroundColor(mContext.getResources().getColor(R.color.timeout));
//			btTest.setTextColor(mContext.getResources().getColor(R.color.timeout));
//			btTest.setTypeface(null, Typeface.BOLD);
		} else
			tvBuAlpha.setBackgroundColor(Color.TRANSPARENT);
//			btTest.setTextColor(mContext.getResources().getColor(R.color.black));
//		TextView mTextView = (TextView) mView.findViewById(R.id.item);
//		TextView mTextViewState = (TextView) mView.findViewById(R.id.state);
//		mTextTooltip = (TextView)mView.findViewById(R.id.tooltip);
//		mTextView.setText(items[map[position]]);
//		if (map[position] >= ModuleTestApplication.numberOfAutoTest)
//			mTextView.setTextColor(Color.YELLOW);
//		else
//			mTextView.setTextColor(Color.CYAN);
//		mTextViewState.setText(states[map[position]]);
//		if (states[map[position]].equals("失败")) {
//			mTextViewState.setTextColor(Color.RED);
//		} else if (states[map[position]].equals("成功")) {
//			mTextViewState.setTextColor(Color.GREEN);
//		} else if (states[map[position]].equals("操作超时")) {
//			mTextViewState.setTextColor(Color.YELLOW);
//		} else {
//			mTextViewState.setTextColor(Color.GRAY);
//		}
//		mTextTooltip.setText(tooltip[map[position]]);
		return mView[position];
	}

	protected void setSuccessCount(String name, int inc) {
		if (!name.equals(mContext.getString(R.string.test_status)) && !name.equals(mContext.getString(R.string.factoryreset_test)))
			successCount += inc;
	}

	public TestState getTestState(String name) {
		if (states[getIndex(name)].equals("未测试"))
			return TestState.TEST_STATE_NONE;
		else if (states[getIndex(name)].equals("测试中"))
			return TestState.TEST_STATE_ON_GOING;
		else if (states[getIndex(name)].equals("成功"))
			return TestState.TEST_STATE_SUCCESS;
		else if (states[getIndex(name)].equals("失败"))
			return TestState.TEST_STATE_FAIL;
		else if (states[getIndex(name)].equals("操作超时"))
			return TestState.TEST_STATE_TIME_OUT;
		return TestState.TEST_STATE_NONE;
	}

	public void setTestState(String name, TestState state) {
		int index = getIndex(name);
		switch (state) {
			case TEST_STATE_NONE:
				if (states[index].equals("成功"))
					setSuccessCount(name, -1);
				states[index] = "未测试";
				break;
			case TEST_STATE_ON_GOING:
				if (states[index].equals("成功"))
					setSuccessCount(name, -1);
				states[index] = "测试中";
				mView[index].postInvalidate();
				break;
			case TEST_STATE_SUCCESS:
				if (!states[index].equals("成功")) {
					setSuccessCount(name, 1);
					Log.d(ModuleTestApplication.TAG, name + " test succeeded");
					if (successCount >= needSuccessCount) setMiscFlag();
				}
				states[index] = "成功";
				break;
			case TEST_STATE_FAIL:
				if (states[index].equals("成功"))
					setSuccessCount(name, -1);
				states[index] = "失败";
				break;
			case TEST_STATE_TIME_OUT:
				if (states[index].equals("成功"))
					setSuccessCount(name, -1);
				states[index] = "操作超时";
				break;
		}
	}

	public int getIndex(String name) {
		for (int i=0; i<numberOfTest; i++)
			if (items[i].equals(name)) return i;
		return -1;
	}

	public void resetListViewState() {
		for (int i=0; i<numberOfTest; i++) {
			states[i] = "未测试";
		}
	}

	protected void copyFile(File f) {
		String name = f.getName();
		int length = (int) f.length();
		char[] buf = new char[length];
		try {
			FileReader fr = new FileReader(f);
			fr.read(buf, 0, length);
			fr.close();
		} catch (Exception ignored) {}

		try {
			f.delete();
			f.createNewFile();
			f.setReadable(true, false);
			f.setWritable(true, false);
			FileWriter fw = new FileWriter(f);
			fw.write(buf, 0, length);
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setMiscFlag() {
		File f = new File("/misc/pcba_apk_test");
		File fprodmark = new File("/misc/prodmark");
		boolean flag = false, pflag = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;

			while ((line = br.readLine()) != null) {
				if (!isPCBA && line.equals("apk_ok") || isPCBA && line.equals("pcba_ok")) {
					flag = true;
					break;
				}
			}
		} catch (Exception ignored) {}

		try {
			BufferedReader br = new BufferedReader(new FileReader(fprodmark));
			String line;

			while ((line = br.readLine()) != null) {
				if (!isPCBA && line.equals("APKTEST=1") || isPCBA && line.equals("PCBATEST=1")) {
					pflag = true;
					break;
				}
			}
		} catch (Exception ignored) {}

		try {
			if (!flag) {
				if (!f.canWrite()) copyFile(f);
				FileWriter fw = new FileWriter(f, true);
				if (isPCBA) {
					fw.write("pcba_ok\n");
				} else {
					fw.write("apk_ok\n");
				}
				fw.flush();
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (!pflag) {
				if (!fprodmark.canWrite()) copyFile(fprodmark);
				FileWriter fwprodmark = new FileWriter(fprodmark, true);
				if (isPCBA) {
					fwprodmark.write("PCBATEST=1\n");
				} else {
					fwprodmark.write("APKTEST=1\n");
				}
				fwprodmark.flush();
				fwprodmark.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
