package com.bluedot.pointapp.model;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by Jichen on 1/04/15.
 */
public abstract class MapItem implements ClusterItem{

    private MarkerOptions markerOptions;

    private String id;

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
}
