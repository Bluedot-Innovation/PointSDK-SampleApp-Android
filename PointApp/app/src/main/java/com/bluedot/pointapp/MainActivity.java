package com.bluedot.pointapp;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.widget.Toast;

import com.bluedotinnovation.android.pointapp.R;

import java.util.ArrayList;
import java.util.List;

import au.com.bluedot.point.ApplicationNotification;
import au.com.bluedot.point.ApplicationNotificationListener;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.ServiceStatusListener;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.ZoneInfo;
import au.com.bluedot.point.net.engine.ServiceManager;

public class MainActivity extends FragmentActivity implements
        ServiceStatusListener,
        ApplicationNotificationListener {

    private ServiceManager mServiceManager;
    private ProgressDialog mProgress;
    private FragmentTabHost mTabHost;
    private boolean quit;

    private ArrayList<ZoneInfo> mZonesInfo = new ArrayList<ZoneInfo>();

    private boolean serviceStarted = false;

    // TAB indexes
    private final static int TAB_AUTH = 0;
    private final static int TAB_MAP = 1;
    private final static int TAB_CHECKLIST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(),
                android.R.id.tabcontent);

        //Checking if the app is started from the Url
        Bundle uriAuthData = new Bundle();
        if (getIntent() != null && getIntent().getData() != null) {
            Uri customURI = getIntent().getData();
            uriAuthData.putString("uri_data", customURI.toString());
        }

        //Setup UI
        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.title_section1))
                        .setIndicator(getString(R.string.title_section1)),
                AuthenticationFragment.class, uriAuthData);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.title_section2))
                        .setIndicator(getString(R.string.title_section2)),
                PointMapFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.title_section3))
                        .setIndicator(getString(R.string.title_section3)),
                ChecklistFragment.class, null);

        //Get an instance of ServiceManager
        mServiceManager = ServiceManager.getInstance(this);
       
        //Setup the notification icon to display when a notification action is triggered
        mServiceManager.setNotificationIDResourceID(R.drawable.ic_launcher);
        
        //Setup the notification activity to start when a fired notification is clicked 
        mServiceManager.setCustomMessageAction(MainActivity.class);

        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(false);
    }

    //stop the Bluedot Point Service
    public void stopService() {
        if (mServiceManager != null) {
        	//Call the method stopPointService in ServiceManager to stop Bluedot PointService
            mServiceManager.stopPointService();
            if (mTabHost != null) {
                refreshCurrentFragment(mTabHost.getCurrentTab());
            }
            mZonesInfo.clear();

        }
    }

    //Using ServiceManager to monitor Bluedot Point Service status
    @Override
    protected void onResume() {
        super.onResume();
        mServiceManager.addBlueDotPointServiceStatusListener(this);
        mServiceManager.subscribeForApplicationNotification(this);
        serviceStarted = mServiceManager.isBlueDotPointServiceRunning();
        refreshCurrentFragment(mTabHost.getCurrentTab());
    }

    @Override
    public void onCheckIntoFence(final ApplicationNotification applicationNotification) {

        if (applicationNotification != null
                && applicationNotification.getFence() != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getApplicationContext(),
                            "Application Notification Received !! You have Entered : "
                                    + applicationNotification.getFence().getName(),
                            Toast.LENGTH_LONG).show();
                }
            });

        }

    }

    @Override
    public void onCheckIntoBeacon(final ApplicationNotification applicationNotification) {

        if (applicationNotification != null
                && applicationNotification.getBeaconInfo() != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getApplicationContext(),
                            "Application Notification Received !! You have Entered : "
                                    + applicationNotification.getBeaconInfo().getName(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    public ArrayList<ZoneInfo> getZones() {
        return mServiceManager.getZonesAndFences();
    }

    public void startAuthentication(String email, String apiKey,
                                    String packageName, boolean restartMode, String url) {
        mProgress.setMessage(getString(R.string.please_wait_authenticating));
        mProgress.show();

        if (url == null){
            // no alternative url provided
            mServiceManager.sendAuthenticationRequest(packageName, apiKey, email, this, restartMode);
        } else {
            mServiceManager.sendAuthenticationRequest(packageName, apiKey, email, this, restartMode, url);
        }
    }

    public void refreshCurrentFragment(int tabIndex) {
        switch (tabIndex) {
//            case TAB_AUTH:
//                AuthenticationFragment authFragment = (AuthenticationFragment) getSupportFragmentManager()
//                        .findFragmentByTag(mTabHost.getCurrentTabTag());
//                if (authFragment != null) {
//                    authFragment.refresh();
//                }
//                break;
            case TAB_MAP:
                PointMapFragment pointMapFragment = (PointMapFragment) getSupportFragmentManager()
                        .findFragmentByTag(mTabHost.getCurrentTabTag());
                if (pointMapFragment != null) {
                    pointMapFragment.refresh();
                }
                break;
            case TAB_CHECKLIST:
                ChecklistFragment checklistFragment = (ChecklistFragment) getSupportFragmentManager()
                        .findFragmentByTag(mTabHost.getCurrentTabTag());
                if (checklistFragment != null) {
                    checklistFragment.refresh();
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }

        if (quit && mServiceManager != null) {
            mServiceManager.stopPointService();
        }

        mServiceManager.removeBlueDotPointServiceStatusListener(this);

        mServiceManager.unsubscribeForApplicationNotification(this);


    }

    @Override
    public void onBackPressed() {
        if (serviceStarted)
            new AlertDialog.Builder(this).setTitle("Quit").setMessage("Do you want to quit the app or put it in the background").setPositiveButton("QUIT", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    quit = true;
                    finish();
                }
            }).setNegativeButton("NO", null).setNeutralButton("Background", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();

                }
            }).create().show();

        else
            super.onBackPressed();
    }

    //This is called when Bluedot Point Service started successful, your app logic depending on the Bluedot PointService could be started from here
    @Override
    public void onBlueDotPointServiceStartedSuccess() {
        if (mProgress != null && mProgress.isShowing())
            mProgress.dismiss();

        serviceStarted = true;

        refreshCurrentFragment(mTabHost.getCurrentTab());
    }

    //This is called when Bluedot Point Service stopped. Your app could clear and release resources 
    @Override
    public void onBlueDotPointServiceStop() {
        if (mProgress != null && mProgress.isShowing())
            mProgress.dismiss();

        serviceStarted = false;

        refreshCurrentFragment(mTabHost.getCurrentTab());
    }

    //This is invoked when Bluedot Point Service got error. You can call isFatal() method to check if the error is fatal. 
    //The Bluedot Point Service will stop itself if the error is fatal, then the onBlueDotPointServiceStop() is called 
    @Override
    public void onBlueDotPointServiceError(final BDError bdError) {
        if (mProgress != null && mProgress.isShowing())
            mProgress.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Builder(MainActivity.this).setTitle("Error").setMessage(bdError.getReason()).setPositiveButton("OK", null).create().show();
            }
        });
    }

    @Override
    public void onRuleUpdate(List<ZoneInfo> zoneInfos) {
        if (mTabHost.getCurrentTab() == TAB_MAP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshCurrentFragment(TAB_MAP);
                }
            });
        }
    }

    @Override
    public void onBlueDotPointServiceStopWithError(final BDError bdError) {
        if (mProgress != null && mProgress.isShowing())
            mProgress.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Builder(MainActivity.this).setTitle("Error").setMessage(bdError.getReason()).setPositiveButton("OK", null).create().show();
            }
        });
    }

}
