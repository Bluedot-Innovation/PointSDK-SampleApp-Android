package com.bluedot.pointapp;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.bluedotinnovation.android.pointapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import au.com.bluedot.application.model.Proximity;
import au.com.bluedot.point.ApplicationNotificationListener;
import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.ServiceStatusListener;
import au.com.bluedot.point.net.engine.BeaconInfo;
import au.com.bluedot.point.net.engine.FenceInfo;
import au.com.bluedot.point.net.engine.LocationInfo;
import au.com.bluedot.point.net.engine.ZoneInfo;
import au.com.bluedot.point.net.engine.ServiceManager;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 */
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

    // storage for credentials update which may happen while app is paused
    private Bundle scheduledCredentialsUpdate = null;

    // To determine whether current activity is visible
    private boolean isActivityPaused = false;

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

        // Android O handling - Set the foreground Service Notification which will fire only if running on Android O and above
        Intent actionIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT );
        mServiceManager.setForegroundServiceNotification(R.drawable.ic_launcher, getString(R.string.foreground_notification_title), getString(R.string.foreground_notification_text), pendingIntent, false);

        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(false);

        mTabHost.getTabWidget().getChildTabViewAt(TAB_CHECKLIST).setClickable(false);
        mTabHost.getTabWidget().getChildTabViewAt(TAB_MAP).setClickable(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            // get new credentials from resuming intent
            scheduledCredentialsUpdate = new Bundle();
            Uri customURI = intent.getData();
            scheduledCredentialsUpdate.putString("uri_data", customURI.toString());
        }
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
        isActivityPaused = false;
        mServiceManager.addBlueDotPointServiceStatusListener(this);
        serviceStarted = mServiceManager.isBlueDotPointServiceRunning();
        if (scheduledCredentialsUpdate == null) {
            refreshCurrentFragment(mTabHost.getCurrentTab());
        } else {
            mTabHost.setCurrentTab(TAB_AUTH);
            // pass new credentials to authentication fragment
            AuthenticationFragment authFragment = (AuthenticationFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.title_section1));
            if (authFragment != null) {
                authFragment.refresh(scheduledCredentialsUpdate);
            }
            scheduledCredentialsUpdate = null;
        }
    }
    @Override
    public void onCheckIntoFence(FenceInfo fenceInfo, ZoneInfo zoneInfo, LocationInfo location, Map<String, String> customData, boolean isCheckOut) {
        String messageText = "";
// uncomment to display custom data
//        if (customData!=null && !(customData.isEmpty())) {
//            for (Map.Entry<String, String> entry : customData.entrySet()) {
//                messageText += entry.getKey() + ": " + entry.getValue() + "\n";
//            }
//        }
        String messageTitle = "CheckIn " + zoneInfo.getZoneName();
        if (!isActivityPaused) {
            fireDialog(messageTitle, messageText);
        } else {
            fireNotification(messageTitle, messageText);
        }
    }

    @Override
    public void onCheckedOutFromFence(FenceInfo fenceInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        String messageText = "dwellTime,min=" + dwellTime + "\n";
// uncomment to display custom data
//        if (customData!=null && !(customData.isEmpty())) {
//            for (Map.Entry<String, String> entry : customData.entrySet()) {
//                messageText += entry.getKey() + ": " + entry.getValue() + "\n";
//            }
//        }
        String messageTitle = "CheckOut " + zoneInfo.getZoneName();
        if (!isActivityPaused) {
            fireDialog(messageTitle, messageText);
        } else {
            fireNotification(messageTitle, messageText);
        }
    }

    @Override
    public void onCheckIntoBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, LocationInfo location, Proximity proximity, Map<String, String> customData, boolean isCheckOut) {
        String messageText = "";
// uncomment to display custom data
//        if (customData!=null && !(customData.isEmpty())) {
//            for (Map.Entry<String, String> entry : customData.entrySet()) {
//                messageText += entry.getKey() + ": " + entry.getValue() + "\n";
//            }
//        }
        String messageTitle = "CheckIn " + zoneInfo.getZoneName();
        if (!isActivityPaused) {
            fireDialog(messageTitle, messageText);
        } else {
            fireNotification(messageTitle, messageText);
        }
    }

    @Override
    public void onCheckedOutFromBeacon(BeaconInfo beaconInfo, ZoneInfo zoneInfo, int dwellTime, Map<String, String> customData) {
        String messageText = "dwellTime,min=" + dwellTime + "\n";
// uncomment to display custom data
//        if (customData!=null && !(customData.isEmpty())) {
//            for (Map.Entry<String, String> entry : customData.entrySet()) {
//                messageText += entry.getKey() + ": " + entry.getValue() + "\n";
//            }
//        }
        String messageTitle = "CheckOut " + zoneInfo.getZoneName();
        if (!isActivityPaused) {
            fireDialog(messageTitle, messageText);
        } else {
            fireNotification(messageTitle, messageText);
        }
    }

    private void fireDialog(final String title, final String message) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle(title);
                    alertDialog.setMessage(message);
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
        } catch (Exception e) {

        }
    }

    public ArrayList<ZoneInfo> getZones() {
        return mServiceManager.getZonesAndFences();
    }

    public void startAuthentication(String email, String apiKey,
                                    String packageName, boolean restartMode, String url) {
        mProgress.setMessage(getString(R.string.please_wait_authenticating));
        mProgress.show();

        if (url == null) {
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

        isActivityPaused = true;
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }

        if (quit && mServiceManager != null) {
            mServiceManager.stopPointService();
        }

        mServiceManager.removeBlueDotPointServiceStatusListener(this);

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

        mTabHost.setCurrentTab(TAB_MAP);
        mTabHost.getTabWidget().getChildTabViewAt(TAB_CHECKLIST).setClickable(true);
        mTabHost.getTabWidget().getChildTabViewAt(TAB_MAP).setClickable(true);

        mServiceManager.subscribeForApplicationNotification(this);
    }

    //This is called when Bluedot Point Service stopped. Your app could clear and release resources 
    @Override
    public void onBlueDotPointServiceStop() {
        if (mProgress != null && mProgress.isShowing())
            mProgress.dismiss();

        serviceStarted = false;

        mServiceManager.unsubscribeForApplicationNotification(this);

        refreshCurrentFragment(mTabHost.getCurrentTab());
        mTabHost.getTabWidget().getChildTabViewAt(TAB_CHECKLIST).setClickable(false);
        mTabHost.getTabWidget().getChildTabViewAt(TAB_MAP).setClickable(false);
    }

    //This is invoked when Bluedot Point Service got error. You can call isFatal() method to check if the error is fatal.
    //The Bluedot Point Service will stop itself if the error is fatal, then the onBlueDotPointServiceStop() is called
    @Override
    public void onBlueDotPointServiceError(final BDError bdError) {
        // if bdError is not fatal - service is still and authentication in progress. No need to shut mProgress.
        if (mProgress != null && mProgress.isShowing() && bdError.isFatal())
            mProgress.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Builder(MainActivity.this).setTitle(bdError.isFatal() ? "Error" : "Notice").setMessage(bdError.getReason()).setPositiveButton("OK", null).create().show();
            }
        });
    }

    @Override
    public void onRuleUpdate(final List<ZoneInfo> zoneInfos) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (zoneInfos == null || zoneInfos.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("Information")
                            .setMessage(Html.fromHtml(getResources().getString(R.string.empty_ruleset)))
                            .setPositiveButton("OK", null).create();
                    alertDialog.show();
                    TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());

                }
                if (mTabHost.getCurrentTab() == TAB_MAP) {
                    refreshCurrentFragment(TAB_MAP);
                }
            }
        });

    }

    private void fireNotification(String notificationTitle, String notificiationMessage) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContentTitle("CustAction: " + notificationTitle);
        mBuilder.setContentText(notificiationMessage);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(new Random().nextInt(), mBuilder.build());
    }

}
