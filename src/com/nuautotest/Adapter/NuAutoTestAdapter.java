package com.nuautotest.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.nuautotest.Activity.R;
import com.nuautotest.application.ModuleTestApplication;

/**
 * 测试界面Adapter
 *
 * @author xie-hang
 *
 */
public class NuAutoTestAdapter extends BaseAdapter {

	public String[] items;
	private String[] states;
	private int[] map;
	private int count;
	private LayoutInflater inflater;
	//	private TextView mTextTooltip;

	public NuAutoTestAdapter(Context context) {
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return count;
	}

	public Object getItem(int position) {
		return map[position];
	}

	public long getItemId(int position) {
		return map[position];
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = inflater.inflate(R.layout.nu_autotest_listview_item, null);
		} else {
			view = convertView;
		}

		TextView mTextView = (TextView) view.findViewById(R.id.item);
		TextView mTextViewState = (TextView) view.findViewById(R.id.state);
//		mTextTooltip = (TextView)view.findViewById(R.id.tooltip);
		mTextView.setText(items[map[position]]);
		if (map[position] >= ModuleTestApplication.numberOfAutoTest)
			mTextView.setTextColor(Color.YELLOW);
		else
			mTextView.setTextColor(Color.CYAN);
		mTextViewState.setText(states[map[position]]);
		if (states[map[position]].equals("失败")) {
			mTextViewState.setTextColor(Color.RED);
		} else if (states[map[position]].equals("成功")) {
			mTextViewState.setTextColor(Color.GREEN);
		} else if (states[map[position]].equals("操作超时")) {
			mTextViewState.setTextColor(Color.YELLOW);
		} else {
			mTextViewState.setTextColor(Color.GRAY);
		}
//		mTextTooltip.setText(tooltip[map[position]]);
		return view;
	}

	public void setItems(String[] items) {
		this.items = items;
	}

	public void setStates(String[] states) {
		this.states = states;
	}

	public void setTooltip(String[] tooltip) {
		String[] tooltip1 = tooltip;
	}

	public void setMap(int[] map) {
		this.map = map;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
