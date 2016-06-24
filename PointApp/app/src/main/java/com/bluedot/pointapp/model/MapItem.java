package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 */
public abstract class MapItem implements ClusterItem{

    private MarkerOptions markerOptions;

    private String id;

    private float icon = BitmapDescriptorFactory.HUE_RED;

    public abstract void setGeometry(Object geometry);

    public abstract Object getGeometry();

    public MarkerOptions getMarkerOptions(){
        return markerOptions;
    }

    public void setMarkerOption(MarkerOptions mo){
        this.markerOptions = mo;
    }

    public String getID(){
        return id;
    }

    public void setID(String id){
        this.id = id;
    }

    public float getIcon() {
        return icon;
    }

    public void setIcon(float icon) {
        this.icon = icon;
    }
}
