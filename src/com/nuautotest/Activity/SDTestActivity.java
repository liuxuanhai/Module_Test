package com.nuautotest.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.nuautotest.Adapter.NuAutoTestAdapter;
import com.nuautotest.application.ModuleTestApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * SD卡测试
 *
 * @author xie-hang
 *
 */

public class SDTestActivity extends Activity {
	private TextView mtvSDStatus;
	private BroadcastReceiver broadcastRec;

	private boolean isFinished;
	private Context mContext;
	private int time;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.sd_test);
		initCreate();

		mtvSDStatus = (TextView)findViewById(R.id.tvSDStatus);

		IntentFilter intentFilter = new IntentFilter(
				Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		intentFilter.addDataScheme("file");

		broadcastRec = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Uri uri = intent.getData();
				if (uri != null) {
					if ("android.intent.action.MEDIA_MOUNTED".equals(intent.getAction())) {
						String path = uri.toString().substring("file://".length());
						if (path.equals("/mnt/sdcardEx")) {
							mtvSDStatus.setText(mContext.getString(R.string.sd_insert));
						}
					} else if ("android.intent.action.MEDIA_REMOVED".equals(intent.getAction())
							|| "android.intent.action.MEDIA_UNMOUNTED".equals(intent.getAction())
							|| "android.intent.action.MEDIA_BAD_REMOVAL".equals(intent.getAction())) {
						String path = uri.toString().substring("file://".length());
						if (path.equals("/mnt/sdcardEx")) {
							mtvSDStatus.setText(mContext.getString(R.string.sd_remove));
						}
					}
				}
			}
		};

		registerReceiver(broadcastRec, intentFilter);

		if (statusTest())
			mtvSDStatus.setText(mContext.getString(R.string.sd_insert));
		else
			mtvSDStatus.setText(mContext.getString(R.string.sd_remove));
	}

	@Override
	protected void onDestroy() {
		try {
			unregisterReceiver(broadcastRec);
		} catch (IllegalArgumentException e) {
			postError("In onDestroy():"+e);
		}
		releaseDestroy();
		super.onDestroy();
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				NuAutoTestAdapter.getInstance().setTestState(getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public boolean onNavigateUp() {
		onBackPressed();
		return true;
	}

	void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter(ModuleTestApplication.LOG_DIR + "/ModuleTest/log_sd.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---SD Test---");
	}

	void releaseDestroy() {
		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "SDTestActivity"+"======"+error+"======");
		NuAutoTestAdapter.getInstance().setTestState(getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	boolean statusTest() {
//		String state = Environment.getExternalStorageState();
//		if ( (state.equals(Environment.MEDIA_MOUNTED)) ||
//			 (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) ) {
//			return true;
//		} else
//			return false;

		File mFile = new File("/mnt/sdcardEx/test.file");
		try {
			if (!mFile.createNewFile()) return false;
			if (mFile.isFile()) {
				mFile.delete();
				return true;
			} else
				return false;
		} catch (IOException e) {
			return false;
		}
	}

	void startAutoTest() {
		isFinished = false;
		NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		initCreate();
	}

	void stopAutoTest(boolean success) {
		if (success)
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_SUCCESS);
		else
			NuAutoTestAdapter.getInstance().setTestState(mContext.getString(R.string.sd_test), NuAutoTestAdapter.TestState.TEST_STATE_FAIL);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
		this.finish();
	}

	private class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Handler handler) {
			super();
			mContext = context;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
			while ( (!isFinished) && (time<1000) ) {
				try {
					if (statusTest()) stopAutoTest(true);
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				stopAutoTest(false);
				Log.e(ModuleTestApplication.TAG, "======SD Test FAILED======");
			}
		}
	}
}