package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by Jichen on 1/04/15.
 */
public class CircleItem extends MapItem{

    private CircleOptions circle;

    @Override
    public LatLng getPosition() {
        return circle.getCenter();
    }

    @Override
    public void setGeometry(Object geometry) {
        this.circle = (CircleOptions)geometry;
    }

    @Override
    public Object getGeometry() {
        return circle;
    }
}
