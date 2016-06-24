package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.clustering.ClusterItem;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 */
public class BoundingBoxItem extends MapItem{

    private PolygonOptions polygonOptions;

    @Override
    public LatLng getPosition() {
        return polygonOptions.getPoints().get(0);
    }

    @Override
    public void setGeometry(Object geometry) {
        this.polygonOptions = (PolygonOptions)geometry;
    }

    @Override
    public Object getGeometry() {
        return polygonOptions;
    }
}
