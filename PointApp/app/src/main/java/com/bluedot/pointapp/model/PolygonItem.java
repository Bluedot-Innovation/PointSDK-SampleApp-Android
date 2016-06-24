package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.clustering.ClusterItem;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 */
public class PolygonItem extends MapItem{

    private PolygonOptions polygon;

    @Override
    public LatLng getPosition() {
        return polygon.getPoints().get(0);
    }

    @Override
    public void setGeometry(Object geometry) {
        this.polygon = (PolygonOptions)geometry;
    }

    @Override
    public Object getGeometry() {
        return polygon;
    }
}
