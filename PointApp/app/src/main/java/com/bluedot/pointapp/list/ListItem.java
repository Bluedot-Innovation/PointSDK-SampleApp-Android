package com.bluedot.pointapp.list;

import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.FenceInfo;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 */
public class ListItem {
	private FenceInfo mFence;
    private BeaconInfo mBeacon;
	private boolean mIsCheckedIn;

	public ListItem(FenceInfo fence, boolean isCheckedIn) {
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
