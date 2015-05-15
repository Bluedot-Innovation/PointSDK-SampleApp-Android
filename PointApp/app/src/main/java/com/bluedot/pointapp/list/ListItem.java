package com.bluedot.pointapp.list;

import au.com.bluedot.application.model.geo.Fence;

public class ListItem {
	private final Fence mFence;
	private boolean mIsCheckedIn;

	public ListItem(Fence fence, boolean isCheckedIn) {
		this.mFence = fence;
		this.mIsCheckedIn = isCheckedIn;
	}

	@Override
	public String toString() {
		return mFence != null ? mFence.getName() : "";
	}

	public boolean isCheckedIn() {
		return mIsCheckedIn;
	}

}