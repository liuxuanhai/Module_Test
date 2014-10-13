package com.nuautotest.Activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.nuautotest.application.ModuleTestApplication;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 休眠唤醒测试
 *
 * @author xie-hang
 *
 */

public class SuspendResumeTestActivity extends Activity {
	public final String ACTION_WAKEUP = "com.nuautotest.Activity.SleepResumeTestActivity.ACTION_WAKEUP";
	public final String ACTION_SLEEP = "com.nuautotest.Activity.SleepResumeTestActivity.ACTION_SLEEP";
	public final String TAG_WAKELOCK = "com.nuautotest.Activity.SleepResumeTestActivity.TAG_WAKELOCK";
	public static final int RTC_WAKEUP_NUFRONT = 4;

	private EditText tbTime, tbNumber, tbInterval;
	private TextView tvStatus;
	private AlarmManager mAlarmManager;
	private PowerManager mPowerManager;
	private WakeLock mWakeLock, mPartialWakeLock;
	//	private DevicePolicyManager mDevicePolicyManager;
	private ModuleTestApplication application;
	//	private ComponentName mDeviceAdminReceiver;
	private AlarmReceiver mAlarmReceiver;
	private int isTesting;
	private int mTime, mNumber, mInterval;
	private boolean isS3;

	private boolean isAutomatic, isFinished;
	private int time;
	private Context mContext;
	private Handler mHandler;
	private FileWriter mLogWriter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.suspend_resume_test);
		mContext = this;

		CheckBox cbS3 = (CheckBox) this.findViewById(R.id.cbSuspendResumeS3);
		TextView tvTime = (TextView) this.findViewById(R.id.tvSuspendResumeTime);
		tbTime = (EditText)this.findViewById(R.id.tbSuspendResumeTime);
		tbNumber = (EditText)this.findViewById(R.id.tbSuspendResumeNumber);
		tbInterval = (EditText)this.findViewById(R.id.tbSuspendResumeInterval);
		Button btStart = (Button) this.findViewById(R.id.btSuspendResumeStart);
		tvStatus = (TextView)this.findViewById(R.id.tvSuspendResumeStatus);

		btStart.setOnClickListener(new StartButtonListener());
		cbS3.setOnCheckedChangeListener(new S3Listener());

		isS3 = cbS3.isChecked();
		mWakeLock = null;
		mPartialWakeLock = null;

		initCreate();
	}

	public void initCreate() {
		if (ModuleTestApplication.LOG_ENABLE) {
			try {
				mLogWriter = new FileWriter("/sdcard/ModuleTest/log_suspendresume.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			ModuleTestApplication.getInstance().recordLog(null);
		}
		Log.i(ModuleTestApplication.TAG, "---Suspend Resume Test---");

		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//		mDevicePolicyManager = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
//		mDeviceAdminReceiver = new ComponentName(mContext, NSDeviceAdminReceiver.class);

		mAlarmReceiver = new AlarmReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_SLEEP);
		intentFilter.addAction(ACTION_WAKEUP);
		mContext.registerReceiver(mAlarmReceiver, intentFilter);

//		if (!mDevicePolicyManager.isAdminActive(mDeviceAdminReceiver)) {
//			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminReceiver);
//			startActivityForResult(intent, 0);
//		}
	}

	@Override
	public void onDestroy() {
		releaseDestroy();
		super.onDestroy();
	}

	public void releaseDestroy() {
//		if (mDevicePolicyManager.isAdminActive(mDeviceAdminReceiver))
//		mDevicePolicyManager.removeActiveAdmin(mDeviceAdminReceiver);
		try {
			mContext.unregisterReceiver(mAlarmReceiver);
		} catch (IllegalArgumentException e) {
			Log.i(ModuleTestApplication.TAG, "======AlarmReceiver Not Registered======");
		}
//		if (mWakeLock != null) {
//			mWakeLock.release();
//			mWakeLock = null;
//		}
		if (mPartialWakeLock != null) {
			if (mPartialWakeLock.isHeld()) mPartialWakeLock.release();
			mPartialWakeLock = null;
		}

		if (ModuleTestApplication.LOG_ENABLE) {
			ModuleTestApplication.getInstance().recordLog(mLogWriter);
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void getValues() {
		Editable str;
		int i;

		str = tbTime.getText();
		mTime = 0;
		for (i=0; i<str.length(); i++)
			mTime = mTime*10 + str.charAt(i)-'0';

		str = tbNumber.getText();
		mNumber = 0;
		for (i=0; i<str.length(); i++)
			mNumber = mNumber*10 + str.charAt(i)-'0';

		str = tbInterval.getText();
		mInterval = 0;
		for (i=0; i<str.length(); i++)
			mInterval = mInterval*10 + str.charAt(i)-'0';
	}

	public void startTest() {
		isTesting = 0;

		if (mPartialWakeLock != null) {
			if (mPartialWakeLock.isHeld()) mPartialWakeLock.release();
			mPartialWakeLock = null;
		}
		if (!isS3) {
			mPartialWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_WAKELOCK);
			mPartialWakeLock.acquire();
		}

//		if (!mDevicePolicyManager.isAdminActive(mDeviceAdminReceiver)) {
//			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminReceiver);
//			startActivityForResult(intent, 0);
//		} else {
		Intent intentSleep = new Intent();
		intentSleep.setAction(ACTION_SLEEP);
		mContext.sendBroadcast(intentSleep);
//		}
	}

	public class S3Listener implements android.widget.CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			isS3 = isChecked;
		}
	}

	public class StartButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (tbTime.length() == 0) {
				tvStatus.append("请设置休眠时间！\n");
				return;
			}
			if (tbNumber.length() == 0) {
				tvStatus.append("请设置休眠次数！\n");
				return;
			}
			if (tbInterval.length() == 0) {
				tvStatus.append("请设置休眠间隔！\n");
				return;
			}

			getValues();
			startTest();
		}
	}

	public class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_SLEEP.equals(intent.getAction())) {
				Log.d(ModuleTestApplication.TAG, "***********SLEEP************");
				if (mWakeLock != null) {
					if (mWakeLock.isHeld()) mWakeLock.release();
					mWakeLock = null;
				}
				Intent intentWakeup = new Intent();
				intentWakeup.setAction(ACTION_WAKEUP);
				long sleepTime = System.currentTimeMillis();
//				if (isS3)
//				{
//					if ( (mTime*60-mInterval>30) && (mTime*60-mInterval<=60) )
//						sleepTime += 30*1000;
//					else
//						sleepTime += mTime*60*1000-mInterval*1000;
//				} else
				sleepTime += mTime*1000;
				mAlarmManager.set(RTC_WAKEUP_NUFRONT, sleepTime,
						PendingIntent.getBroadcast(mContext, 0, intentWakeup, PendingIntent.FLAG_ONE_SHOT));
//				mDevicePolicyManager.lockNow();
				mPowerManager.goToSleep(SystemClock.uptimeMillis());
//				FileWriter fw;
//				try {
//					fw = new FileWriter("/sys/power/state");
//					fw.write("mem");
//					fw.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			} else if (ACTION_WAKEUP.equals(intent.getAction())) {
				Log.d(ModuleTestApplication.TAG, "***********WAKEUP***********");
				isTesting++;
				if (mWakeLock != null) {
					if (mWakeLock.isHeld()) mWakeLock.release();
					mWakeLock = null;
				}
				mWakeLock = mPowerManager.newWakeLock(
						PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.FULL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,
						TAG_WAKELOCK);
				if (isTesting < mNumber)
					mWakeLock.acquire();
				else
					mWakeLock.acquire(10*1000);
//				FileWriter fw;
//				try {
//					fw = new FileWriter("/sys/power/state");
//					fw.write("on");
//					fw.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
				if (isTesting < mNumber) {
					Intent intentSleep = new Intent();
					intentSleep.setAction(ACTION_SLEEP);
					mAlarmManager.set(RTC_WAKEUP_NUFRONT, System.currentTimeMillis()+mInterval*1000,
							PendingIntent.getBroadcast(mContext, 0, intentSleep, PendingIntent.FLAG_ONE_SHOT));
				} else if (isAutomatic) {
					stopAutoTest(true);
				}
			}
		}
	}

	// 成功失败按钮
	public void onbackbtn(View view) {
		switch (view.getId()) {
			case R.id.fail:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.suspendresume_test))]="失败";
				this.finish();
				break;
			case R.id.success:
				application= ModuleTestApplication.getInstance();
				application.getListViewState()[application.getIndex(getString(R.string.suspendresume_test))]="成功";
				this.finish();
				break;
		}
	}

	@Override
	public void onBackPressed() {
	}

	public void startAutoTest() {
		isAutomatic = true;
		isFinished = false;
		initCreate();
		isS3 = false;
		mTime = 2;
		mNumber = 3;
		mInterval = 2;
		application.getListViewState()[application.getIndex(mContext.getString(R.string.suspendresume_test))]="测试中";
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		startTest();
	}

	public void stopAutoTest(boolean success) {
		if (success) {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.suspendresume_test))]="成功";
		} else {
			application.getListViewState()[application.getIndex(mContext.getString(R.string.suspendresume_test))]="失败";
		}
		mHandler.sendEmptyMessage(NuAutoTestActivity.MSG_REFRESH);
		isFinished = true;
		releaseDestroy();
//		mWakeLock = mPowerManager.newWakeLock(
//				PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.FULL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE,
//				TAG_WAKELOCK);
//		mWakeLock.acquire(10*1000);
		this.finish();
	}

	public class AutoTestThread extends Handler implements Runnable {

		public AutoTestThread(Context context, Application app, Handler handler) {
			super();
			mContext = context;
			application = (ModuleTestApplication) app;
			mHandler = handler;
		}

		public void run() {
			startAutoTest();
			while ( (!isFinished) && (time<1000) ) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				time++;
			}
			if (time >= 1000) {
				Log.e(ModuleTestApplication.TAG, "======SuspendResume Test FAILED======");
				stopAutoTest(false);
			}
		}
	}
}
