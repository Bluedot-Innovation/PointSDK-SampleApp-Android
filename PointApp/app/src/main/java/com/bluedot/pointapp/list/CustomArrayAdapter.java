package com.bluedot.pointapp.list;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 */
public class CustomArrayAdapter extends ArrayAdapter<Item> {

	private LayoutInflater mInflater;

	public enum RowType {
		LIST_ITEM, HEADER_ITEM
	}

	public CustomArrayAdapter(Context context, List<Item> items) {
		super(context, 0, items);
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getViewTypeCount() {
		return RowType.values().length;

	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).getViewType();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getItem(position).getView(mInflater, convertView);
	}
}
