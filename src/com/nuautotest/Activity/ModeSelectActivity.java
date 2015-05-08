package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * 测试模式选择
 *   PCBA测试 / 整机测试
 *
 * @author xie-hang
 *
 */

public class ModeSelectActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mode_select);
	}

	public void onClickPCBA(View v) {
		NuAutoTestActivity.getInstance().setMode(NuAutoTestActivity.MODE_PCBA);
		startActivity(new Intent(this, NuAutoTestActivity.class));
		this.finish();
	}

	public void onClickAndroid(View v) {
		NuAutoTestActivity.getInstance().setMode(NuAutoTestActivity.Mode_ANDROID);
		startActivity(new Intent(this, NuAutoTestActivity.class));
		this.finish();
	}
}