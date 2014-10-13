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

public class NuSelectAdapter extends BaseAdapter {

	private LayoutInflater inflater;

	private String[] states;
	private boolean[] selected;

	public NuSelectAdapter(Context context) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return ModuleTestApplication.items.length;
	}

	public Object getItem(int arg0) {
		return arg0;
	}

	public long getItemId(int arg0) {
		return arg0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null)
			view = inflater.inflate(R.layout.nu_select_listview_item, null);
		else
			view = convertView;

		TextView item = (TextView)view.findViewById(R.id.tvItem);
		item.setText(ModuleTestApplication.items[position]);
		TextView state = (TextView)view.findViewById(R.id.tvState);
		state.setText(states[position]);
		state.setTextColor(Color.GRAY);
		if(states[position].equals("失败")){
			state.setTextColor(Color.RED);
		}else if(states[position].equals("成功")){
			state.setTextColor(Color.GREEN);
		}
		if(selected[position]){
			view.setBackgroundColor(0xff00cc00);
			item.setTextColor(Color.BLACK);
			state.setTextColor(Color.BLACK);
		} else {
			view.setBackgroundColor(Color.BLACK);
			item.setTextColor(Color.GRAY);
			state.setTextColor(Color.GRAY);
		}

		return view;
	}

	public void setStates(String[] states) {
		this.states = states;
	}

	public void setSelected(boolean[] selected) {
		this.selected = selected;
	}
}
