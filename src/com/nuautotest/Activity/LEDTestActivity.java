package com.nuautotest.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.nuautotest.application.ModuleTestApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * LED测试
 *
 * @author xie-hang
 *
 */

public class LEDTestActivity extends Activity {
	private final String LED_RED_FILE = "/sys/class/leds/red/brightness";
	private final String LED_GREEN_FILE = "/sys/class/leds/green/brightness";
	private final String LED_BLUE_FILE = "/sys/class/leds/blue/brightness";
	private Button mBtRed, mBtGreen, mBtBlue;
	private boolean mRedOn = false, mGreenOn = false, mBlueOn = false;
	private boolean mPrevRed = false, mPrevGreen = false, mPrevBlue = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.led_test);
		mBtRed = (Button)findViewById(R.id.btLedRed);
		mBtGreen = (Button)findViewById(R.id.btLedGreen);
		mBtBlue = (Button)findViewById(R.id.btLedBlue);

		try {
			Scanner sred = new Scanner(new File(LED_RED_FILE));
			Scanner sgreen = new Scanner(new File(LED_GREEN_FILE));
			Scanner sblue = new Scanner(new File(LED_BLUE_FILE));

			if (!sred.next().equals("0")) {
				mPrevRed = mRedOn = true;
				mBtRed.setText("红色：关闭");
			}
			if (!sgreen.next().equals("0")) {
				mPrevGreen = mGreenOn = true;
				mBtGreen.setText("绿色：关闭");
			}
			if (!sblue.next().equals("0")) {
				mPrevBlue = mBlueOn = true;
				mBtBlue.setText("蓝色：关闭");
			}
		} catch (FileNotFoundException e) {
			ModuleTestApplication application = ModuleTestApplication.getInstance();
			application.setTestState(getString(R.string.led_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		}
	}

	@Override
	public void onPause() {
		mRedOn = mPrevRed;
		mGreenOn = mPrevGreen;
		mBlueOn = mPrevBlue;
		updateLight("Red");
		updateLight("Green");
		updateLight("Blue");

		super.onPause();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		ModuleTestApplication application;
		switch (view.getId()) {
			case R.id.fail:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.led_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.led_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	protected void updateLight(String color) {
		FileWriter fw;
		try {
			if (color.equals("Red")) {
				fw = new FileWriter(LED_RED_FILE);
				if (mRedOn) {
					fw.write("1");
					mBtRed.setText("红色：关闭");
				} else {
					fw.write("0");
					mBtRed.setText("红色：打开");
				}
				fw.close();
			} else if (color.equals("Green")) {
				fw = new FileWriter(LED_GREEN_FILE);
				if (mGreenOn) {
					fw.write("1");
					mBtGreen.setText("绿色：关闭");
				} else {
					fw.write("0");
					mBtGreen.setText("绿色：打开");
				}
				fw.close();
			} else if (color.equals("Blue")) {
				fw = new FileWriter(LED_BLUE_FILE);
				if (mBlueOn) {
					fw.write("1");
					mBtBlue.setText("蓝色：关闭");
				} else {
					fw.write("0");
					mBtBlue.setText("蓝色：打开");
				}
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClickLedRed(View view) {
		mRedOn = !mRedOn;
		updateLight("Red");
	}

	public void onClickLedGreen(View view) {
		mGreenOn = !mGreenOn;
		updateLight("Green");
	}

	public void onClickLedBlue(View view) {
		mBlueOn = !mBlueOn;
		updateLight("Blue");
	}
}