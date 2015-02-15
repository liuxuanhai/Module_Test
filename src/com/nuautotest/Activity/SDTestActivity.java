package com.nuautotest.Activity;

import android.app.Activity;
import android.app.Application;
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
	private ModuleTestApplication application;
	private TextView text, tvSDStatus;
	private BroadcastReceiver broadcastRec;
	private long mTotalSize, mAvailableSize;

	private boolean isFinished;
	private Context mContext;
	private int time;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sd_test);
		initCreate();

		text = (TextView) findViewById(R.id.sdsensor);
		tvSDStatus = (TextView) findViewById(R.id.tvSDStatus);

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
							text.setText("操作: 插入");
							tvSDStatus.setText("状态: SD卡已插入");
						}
					} else if ("android.intent.action.MEDIA_REMOVED".equals(intent.getAction())
							|| "android.intent.action.MEDIA_UNMOUNTED".equals(intent.getAction())
							|| "android.intent.action.MEDIA_BAD_REMOVAL".equals(intent.getAction())) {
						String path = uri.toString().substring("file://".length());
						if (path.equals("/mnt/sdcardEx")) {
							text.setText("操作: 移除");
							tvSDStatus.setText("状态: SD卡未插入");
						}
					}
				}
			}
		};

		registerReceiver(broadcastRec, intentFilter);

		if (statusTest())
			tvSDStatus.setText("状态: SD卡已插入");
		else
			tvSDStatus.setText("状态: SD卡未插入");
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
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
				this.finish();
				break;
			case R.id.success:
				application = ModuleTestApplication.getInstance();
				application.setTestState(getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
				this.finish();
				break;
		}

	}

	@Override
	public void onBackPressed() {
	}

	public void initCreate() {
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

	public void releaseDestroy() {
		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void postError(String error) {
		Log.e(ModuleTestApplication.TAG, "SDTestActivity"+"======"+error+"======");
		ModuleTestApplication.getInstance().setTestState(getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		this.finish();
	}

	public boolean statusTest() {
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
				mTotalSize = mFile.getTotalSpace();
				mAvailableSize = mFile.getUsableSpace();
				mFile.delete();
				return true;
			} else
				return false;
		} catch (IOException e) {
			return false;
		}
	}

	public void startAutoTest() {
		isFinished = false;
		ModuleTestApplication.getInstance().getTooltip()
				[ModuleTestApplication.getInstance().getIndex(mContext.getString(R.string.sd_test))]="***请插入SD卡***";
		ModuleTestApplication.getInstance().setTestState(mContext.getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_ON_GOING);
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		initCreate();
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.getTooltip()[application.getIndex(mContext.getString(R.string.sd_test))]=
					"SD卡总大小："+mTotalSize/1024/1024+"M\t\t可用空间："+mAvailableSize/1024/1024+"M";
			application.setTestState(mContext.getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_SUCCESS);
		} else {
			application.getTooltip()[application.getIndex(mContext.getString(R.string.sd_test))]="";
			application.setTestState(mContext.getString(R.string.sd_test), ModuleTestApplication.TestState.TEST_STATE_FAIL);
		}
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
		this.finish();
	}

	public class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Application app, Handler handler) {
			super();
			mContext = context;
			application = (ModuleTestApplication)app;
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