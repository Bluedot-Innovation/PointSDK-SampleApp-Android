package com.bluedot.pointapp.list;

import android.view.LayoutInflater;
import android.view.View;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 */
public interface Item {
	public int getViewType();

	public View getView(LayoutInflater inflater, View convertView);
}
