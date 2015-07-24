package com.nuautotest.Activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;

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
	private TextView mTvLed;

	private final int LED_RED = 0x1;
	private final int LED_GREEN = 0x2;
	private final int LED_BLUE = 0x4;
	private int mCurrent = 0;
	private int mPrev = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.led_test);
		mBtRed = (Button)findViewById(R.id.btLedRed);
		mBtGreen = (Button)findViewById(R.id.btLedGreen);
		mBtBlue = (Button)findViewById(R.id.btLedBlue);
		mTvLed = (TextView)findViewById(R.id.tvLED);

		try {
			Scanner sred = new Scanner(new File(LED_RED_FILE));
			Scanner sgreen = new Scanner(new File(LED_GREEN_FILE));
			Scanner sblue = new Scanner(new File(LED_BLUE_FILE));

			if (!sred.next().equals("0")) {
				mPrev |= LED_RED;
				mBtRed.setText(getString(R.string.led_red_close));
			}
			if (!sgreen.next().equals("0")) {
				mPrev |= LED_GREEN;
				mBtGreen.setText(getString(R.string.led_green_close));
			}
			if (!sblue.next().equals("0")) {
				mPrev |= LED_BLUE;
				mBtBlue.setText(getString(R.string.led_blue_close));
			}
			mCurrent = mPrev;
			updateLight();
		} catch (FileNotFoundException e) {
			NuAutoTestAdapter.getInstance().setTestState(getString(R.string.led_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		}
	}

	@Override
	public void onPause() {
		mCurrent = mPrev;
		updateLight();

		super.onPause();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.led_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.led_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	void updateLight() {
		FileWriter fw;
		try {
				fw = new FileWriter(LED_RED_FILE);
				if ((mCurrent & LED_RED) != 0) {
					fw.write("1");
					mBtRed.setText(getString(R.string.led_red_close));
				} else {
					fw.write("0");
					mBtRed.setText(getString(R.string.led_red_open));
				}
				fw.close();

				fw = new FileWriter(LED_GREEN_FILE);
				if ((mCurrent & LED_GREEN) != 0) {
					fw.write("1");
					mBtGreen.setText(getString(R.string.led_green_close));
				} else {
					fw.write("0");
					mBtGreen.setText(getString(R.string.led_green_open));
				}
				fw.close();

				fw = new FileWriter(LED_BLUE_FILE);
				if ((mCurrent & LED_BLUE) != 0) {
					fw.write("1");
					mBtBlue.setText(getString(R.string.led_blue_close));
				} else {
					fw.write("0");
					mBtBlue.setText(getString(R.string.led_blue_open));
				}
				fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		switch (mCurrent) {
			case 0:
				mTvLed.setBackgroundColor(Color.BLACK);
				break;
			case LED_RED:
				mTvLed.setBackgroundColor(Color.RED);
				break;
			case LED_GREEN:
				mTvLed.setBackgroundColor(Color.GREEN);
				break;
			case LED_BLUE:
				mTvLed.setBackgroundColor(Color.BLUE);
				break;
			case LED_RED | LED_GREEN:
				mTvLed.setBackgroundColor(Color.YELLOW);
				break;
			case LED_RED | LED_BLUE:
				mTvLed.setBackgroundColor(Color.MAGENTA);
				break;
			case LED_GREEN | LED_BLUE:
				mTvLed.setBackgroundColor(Color.CYAN);
				break;
			case LED_RED | LED_GREEN | LED_BLUE:
				mTvLed.setBackgroundColor(Color.WHITE);
				break;
		}
	}

	public void onClickLedRed(View view) {
		mCurrent ^= LED_RED;
		updateLight();
	}

	public void onClickLedGreen(View view) {
		mCurrent ^= LED_GREEN;
		updateLight();
	}

	public void onClickLedBlue(View view) {
		mCurrent ^= LED_BLUE;
		updateLight();
	}
}