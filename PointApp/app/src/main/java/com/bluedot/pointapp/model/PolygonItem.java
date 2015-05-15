package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by Jichen on 1/04/15.
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
