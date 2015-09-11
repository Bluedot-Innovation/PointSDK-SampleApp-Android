package com.bluedot.pointapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ExpandableListView;

import com.bluedot.pointapp.list.CheckListAdapter;
import com.bluedot.pointapp.list.HeaderItem;
import com.bluedot.pointapp.list.ListItem;
import com.bluedotinnovation.android.pointapp.R;

import java.util.ArrayList;

import au.com.bluedot.application.model.geo.Fence;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.ZoneInfo;

public class ChecklistFragment extends Fragment {

    private ExpandableListView mListView;

    private static MainActivity mActivity;
    private CheckListAdapter mAdapter;
    private ArrayList<HeaderItem> mHeaderItems;
    private ArrayList<ArrayList<ListItem>> mListItems;
    private int mWidth;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = (WindowManager) mActivity
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            mWidth = display.getWidth(); // deprecated
        } else {
            display.getSize(size);
            mWidth = size.x;
        }
    }

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_checklist, null);
        mListView = (ExpandableListView) v.findViewById(R.id.list);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mListView.setIndicatorBounds(mWidth - 100, mWidth - 20);
        } else {
            mListView.setIndicatorBoundsRelative(mWidth - 100, mWidth - 20);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        updateListView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getActivity() == null) {
            mActivity = (MainActivity) activity;
        } else {
            mActivity = (MainActivity) getActivity();
        }
    }

    private void updateListView() {
        if (mActivity != null && mListView != null) {
            ArrayList<ZoneInfo> zones = mActivity.getZones();
            mHeaderItems = new ArrayList<HeaderItem>();
            mListItems = new ArrayList<ArrayList<ListItem>>();
            for (ZoneInfo zoneInfo : zones) {
                mHeaderItems.add(new HeaderItem(zoneInfo.getZoneName(), zoneInfo.getZoneId()));
                ArrayList<ListItem> mZoneItems = new ArrayList<ListItem>();
                for (Fence fence : zoneInfo.getFences()) {
                    boolean isCheckedIn = false;
                    mZoneItems.add(new ListItem(fence, isCheckedIn));
                }
                for (BeaconInfo beacon : zoneInfo.getBeacons()) {
                    boolean isCheckedIn = false;
                    mZoneItems.add(new ListItem(beacon, isCheckedIn));
                }
                mListItems.add(mZoneItems);
            }

            mAdapter = new CheckListAdapter(mActivity, mHeaderItems, mListItems);
            mListView.setAdapter(mAdapter);
        }
    }

    public void refresh() {
        updateListView();
    }

}
