package com.nuautotest.Activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.*;

/**
 * 校准/测试状态
 *
 * @author xie-hang
 *
 */

public class TestStatusActivity extends Activity {
	static final String CALIBRATION_PORT = "/dev/ttyN5";

	private TextView mtvCalibration, mtvBPTest, mtvPCBA, mtvAPK;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_status);
		mtvCalibration = (TextView)this.findViewById(R.id.tvCalibTest);
		mtvBPTest = (TextView)this.findViewById(R.id.tvBPTest);
		mtvPCBA = (TextView)this.findViewById(R.id.tvPCBATest);
		mtvAPK = (TextView)this.findViewById(R.id.tvAPKTest);
	}

	@Override
	public void onResume() {
		super.onResume();

		/* Calibration */
		try {
			final String CALIB_WRITE = "at+xprs?\r\n";
			final String CALIB_READ = "+XPRS:";
			final String CALIB_STATE = "CAL=";
			final String TEST_STATE = "FT=";
			FileWriter fw = new FileWriter(CALIBRATION_PORT);
			FileReader fr = new FileReader(CALIBRATION_PORT);
			char calib_state[] = new char[64];
			int count, time = 0;
			Integer iCalib_state = -1, iTest_state = -1;

			while (time < 10) {
				try {
					fw.write(CALIB_WRITE);
					fw.flush();

					count = fr.read(calib_state);
					String szCalib_state = String.valueOf(calib_state, 0, count);
					if (szCalib_state.contains(CALIB_READ)) {
						if (szCalib_state.contains(CALIB_STATE)) {
							int index = szCalib_state.lastIndexOf(CALIB_STATE);
							String substr = szCalib_state.substring(index + CALIB_STATE.length(), index + CALIB_STATE.length() + 1);
							iCalib_state = Integer.valueOf(substr, 16);
						}
						if (szCalib_state.contains(TEST_STATE)) {
							int index = szCalib_state.lastIndexOf(TEST_STATE);
							String substr = szCalib_state.substring(index + TEST_STATE.length(), index + TEST_STATE.length() + 1);
							iTest_state = Integer.valueOf(substr, 16);
						}
						if (iCalib_state == 1) mtvCalibration.setBackgroundColor(Color.GREEN);
						if (iTest_state == 1) mtvBPTest.setBackgroundColor(Color.GREEN);
						break;
					} else {
						Log.w(ModuleTestApplication.TAG, CALIB_READ + " not found in " + szCalib_state);
						time++;
					}
				} catch (Exception ignored) {}
			}
			fw.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* Test status */
		try {
			BufferedReader br = new BufferedReader(new FileReader("/misc/pcba_apk_test"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("pcba_ok"))
					mtvPCBA.setBackgroundColor(Color.GREEN);
				else if (line.equals("apk_ok"))
					mtvAPK.setBackgroundColor(Color.GREEN);
			}
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
	}

	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.test_status), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.test_status), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}
	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}
}