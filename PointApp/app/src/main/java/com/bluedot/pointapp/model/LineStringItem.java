package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 */
public class LineStringItem extends MapItem{

	private PolylineOptions polygon;

	@Override
	public LatLng getPosition() {
		return polygon.getPoints().get(0);
	}

	@Override
	public void setGeometry(Object geometry) {
		this.polygon = (PolylineOptions)geometry;
	}

	@Override
	public Object getGeometry() {
		return polygon;
	}
}
