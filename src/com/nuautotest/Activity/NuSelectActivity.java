package com.nuautotest.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.nuautotest.Adapter.NuSelectAdapter;
import com.nuautotest.application.ModuleTestApplication;

public class NuSelectActivity extends Activity {
	private ListView mListView;
	private NuSelectAdapter mAdapter;
	private ModuleTestApplication mApplication;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nu_select_activity);
		mApplication = ModuleTestApplication.getInstance();
//		mListView = (ListView)findViewById(R.id.lvSelect);
		mAdapter = new NuSelectAdapter(this);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
			                        long arg3) {
				boolean[] selected = mApplication.getSelected();
				selected[arg2] = !selected[arg2];
				mApplication.setSelected(selected);
				mAdapter.setSelected(selected);
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.setStates(mApplication.getListViewState());
		mAdapter.setSelected(mApplication.getSelected());
		mAdapter.notifyDataSetChanged();
	}

	public void onClickSelectAll(View view) {
		boolean[] select = mApplication.getSelected();
		for (int i=0; i<select.length; i++)
			select[i] = true;
		mApplication.setSelected(select);

		mAdapter.setSelected(select);
		mAdapter.notifyDataSetChanged();
	}

	public void onClickUnselectAll(View view) {
		boolean[] select = mApplication.getSelected();
		for (int i=0; i<select.length; i++)
			select[i] = false;
		mApplication.setSelected(select);

		mAdapter.setSelected(select);
		mAdapter.notifyDataSetChanged();
	}

	public void onClickStart(View view) {
		int[] map = mApplication.getMap();
		int current=0;
		for (int i=0; i<map.length; i++)
			if (mApplication.getSelected()[i]) {
				map[current] = i;
				current++;
			}
		mApplication.setMap(map);
		mApplication.setCount(current);

		Intent intent = new Intent(NuSelectActivity.this, NuAutoTestActivity.class);
		startActivity(intent);
	}
}
