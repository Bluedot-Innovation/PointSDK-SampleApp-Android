package com.bluedot.pointapp.list;

import au.com.bluedot.application.model.geo.Fence;
import au.com.bluedot.point.net.engine.BeaconInfo;

public class ListItem {
	private Fence mFence;
    private BeaconInfo mBeacon;
	private boolean mIsCheckedIn;

	public ListItem(Fence fence, boolean isCheckedIn) {
		this.mFence = fence;
		this.mIsCheckedIn = isCheckedIn;
	}

    public ListItem(BeaconInfo beacon, boolean isCheckedIn) {
        this.mBeacon = beacon;
        this.mIsCheckedIn = isCheckedIn;
    }

	@Override
	public String toString() {
		return (mFence != null ? mFence.getName() : (mBeacon != null ? mBeacon.getName() : ""));
	}

	public boolean isCheckedIn() {
		return mIsCheckedIn;
	}

}