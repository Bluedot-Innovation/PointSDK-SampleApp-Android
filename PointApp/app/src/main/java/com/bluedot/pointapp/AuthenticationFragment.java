package com.bluedot.pointapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import au.com.bluedot.point.net.engine.BDError;
import au.com.bluedot.point.ServiceStatusListener;

import au.com.bluedot.point.net.engine.ServiceManager;
import au.com.bluedot.point.net.engine.ZoneInfo;

import com.bluedotinnovation.android.pointapp.R;

import java.util.List;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 */
public class AuthenticationFragment extends Fragment implements OnClickListener, ServiceStatusListener{

    private static final int PERMISSION_REQUEST_CODE = 101;
    // Context Activity and UI elements members
	private MainActivity mActivity;
	private Button mBtnAuthenticate;

    private EditText mEdtEmail;
    private EditText mEdtApiKey;
    private EditText mEdtPackageName;
    private CheckBox mRestartMode;
    private TextView mTxtTitle;
	private boolean mIsAuthenticated;

    // Shared preferences - used to store Bluedot credentials
    private SharedPreferences mSharedPreferences;

    // Alternative back-end URL
    private String mAlternativeUrl = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_authenticate,
				container, false);
		mEdtEmail = (EditText) rootView.findViewById(R.id.edt_email);
		mEdtApiKey = (EditText) rootView.findViewById(R.id.edt_api_key);
		mEdtPackageName = (EditText) rootView.findViewById(R.id.edt_package_name);
        mBtnAuthenticate = (Button) rootView .findViewById(R.id.btn_authenticate);
        mRestartMode = (CheckBox) rootView .findViewById(R.id.chk_box_restart_mode);
        mTxtTitle = (TextView) rootView .findViewById(R.id.txt_title);
        mBtnAuthenticate.setOnClickListener(this);

        // get existing credentials from shared preferences
        if (getActivity() != null ) {
            mSharedPreferences = getActivity().getSharedPreferences(AppConstants.APP_PROFILE, Activity.MODE_PRIVATE);
            mEdtEmail.setText(mSharedPreferences.getString(AppConstants.KEY_USERNAME, null));
            mEdtApiKey.setText(mSharedPreferences.getString(AppConstants.KEY_API_KEY, null));
            mEdtPackageName.setText(mSharedPreferences.getString(AppConstants.KEY_PACKAGE_NAME, null));
        }

        // get credentials from lanching Uri
        if ( (getArguments() != null && getArguments().containsKey("uri_data")) ){
            Uri customURI = Uri.parse(getArguments().getString("uri_data"));
            final String uriPackageName = customURI.getQueryParameter("BDPointPackageName");
            final String uriApiKey = customURI.getQueryParameter("BDPointAPIKey");
            final String uriEmail = customURI.getQueryParameter("BDPointUsername");
            mAlternativeUrl = customURI.getQueryParameter("BDPointAPIUrl");

            // Now decide which credentials to put onto UI
            if (uriApiKey != null && mEdtApiKey.getText().length() == 0){
                // Put launch Uri credentials
                mEdtEmail.setText(uriEmail);
                mEdtApiKey.setText(uriApiKey);
                mEdtPackageName.setText(uriPackageName);
            } else if ( ! uriApiKey.equals(mEdtApiKey.getText().toString()) ){
                // Both credentials present and Uri credentials are different
                // Ask user if he wants to replace with Uri
                new AlertDialog.Builder(getActivity())
                        .setTitle("Different Credentials")
                        .setCancelable(false)
                        .setMessage(
                                "Bluedot Service is already running with ApiKey :"+ mEdtApiKey.getText().toString()
                                        + "\nDo you want to use new ApiKey : " + uriApiKey + "?")
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mEdtEmail.setText(uriEmail);
                                        mEdtApiKey.setText(uriApiKey);
                                        mEdtPackageName.setText(uriPackageName);
                                    }
                                })
                        .setNegativeButton(R.string.no, null).create().show();
            }
        }
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        ServiceManager.getInstance(getActivity()).addBlueDotPointServiceStatusListener(this);
		
		refresh();

        if (ServiceManager.getInstance(getActivity()).isBlueDotPointServiceConfiguredToRestart()){
            mRestartMode.setChecked(true);
        }
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

    @Override
    public void onResume()
    {
        super.onResume();
        refresh();
    }

	public void refresh() {

        //Checking the Bluedot Point Service status using isBlueDotPointServiceRunning in the ServiceManager
        mIsAuthenticated = ServiceManager.getInstance(getActivity()).isBlueDotPointServiceRunning();
        if(mIsAuthenticated){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.clear_logout));
                    mTxtTitle.setText(getString(R.string.loggned_in_as));
                }
            });
        }else{
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.save_authenticate));
                    mTxtTitle.setText(getString(R.string.enter_details));
                }
            });
        }
	}

    public void refresh(Bundle newCredentials){
        refresh();
        // get credentials from resuming Uri
        if ( (newCredentials != null && newCredentials.containsKey("uri_data")) ){
            Uri customURI = Uri.parse(newCredentials.getString("uri_data"));
            final String uriPackageName = customURI.getQueryParameter("BDPointPackageName");
            final String uriApiKey = customURI.getQueryParameter("BDPointAPIKey");
            final String uriEmail = customURI.getQueryParameter("BDPointUsername");
            mAlternativeUrl = customURI.getQueryParameter("BDPointAPIUrl");

            // Now decide which credentials to put onto UI
            if (uriApiKey != null && mEdtApiKey.getText().length() == 0){
                // Put launch Uri credentials
                mEdtEmail.setText(uriEmail);
                mEdtApiKey.setText(uriApiKey);
                mEdtPackageName.setText(uriPackageName);
            } else if ( ! uriApiKey.equals(mEdtApiKey.getText().toString()) ){
                // Both credentials present and Uri credentials are different
                // Ask user if he wants to replace with Uri
                new AlertDialog.Builder(getActivity())
                        .setTitle("Different Credentials")
                        .setCancelable(false)
                        .setMessage(
                                "Bluedot Service is already running with ApiKey :"+ mEdtApiKey.getText().toString()
                                        + "\nDo you want to use new ApiKey : " + uriApiKey + "?")
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mEdtEmail.setText(uriEmail);
                                        mEdtApiKey.setText(uriApiKey);
                                        mEdtPackageName.setText(uriPackageName);
                                    }
                                })
                        .setNegativeButton(R.string.no, null).create().show();
            }
        }
    }

	@Override
	public void onClick(View v) {
		if (mIsAuthenticated) {
			mActivity.stopService();
            mIsAuthenticated = false;
		} else {
			if (TextUtils.isEmpty(mEdtEmail.getText())
					|| TextUtils.isEmpty(mEdtApiKey.getText())
					|| TextUtils.isEmpty(mEdtPackageName.getText())) {
				new AlertDialog.Builder(getActivity()).setTitle("Error")
						.setMessage("Please enter login details.")
						.setPositiveButton("OK", null).create().show();
			} else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (checkPermission() && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)) {
                    mActivity.startAuthentication(mEdtEmail.getText().toString(),
                            mEdtApiKey.getText().toString(), mEdtPackageName
                                    .getText().toString(), mRestartMode.isChecked(), mAlternativeUrl);
                    mIsAuthenticated = true;
                } else {
                    requestLocationPermission();
                }
            }
		}
	}

    //Update the button status when the Bluedot Point Service status callback is invoked
    @Override
    public void onBlueDotPointServiceStartedSuccess() {
        mIsAuthenticated = true;

        //Here you can store the credentials in your app shared preference since they are correct
        if (mSharedPreferences != null) {
            mSharedPreferences.edit()
                    .putString(AppConstants.KEY_API_KEY, mEdtApiKey.getText().toString())
                    .putString(AppConstants.KEY_USERNAME, mEdtEmail.getText().toString())
                    .putString(AppConstants.KEY_PACKAGE_NAME, mEdtPackageName.getText().toString())
                    .commit();
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnAuthenticate.setText(getString(R.string.clear_logout));
                mTxtTitle.setText(getString(R.string.loggned_in_as));
            }
        });

    }

    @Override
    public void onBlueDotPointServiceStop() {
        mIsAuthenticated = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnAuthenticate.setText(getString(R.string.save_authenticate));
                mTxtTitle.setText(getString(R.string.enter_details));
            }
        });
    }


    @Override
    public void onBlueDotPointServiceError(BDError bdError) {
        if(bdError.isFatal()){
            mIsAuthenticated = false;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.save_authenticate));
                    mTxtTitle.setText(getString(R.string.enter_details));
                }
            });
        }
    }

    @Override
    public void onRuleUpdate(List<ZoneInfo> zoneInfos) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceManager.getInstance(getActivity()).removeBlueDotPointServiceStatusListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:

                boolean permissionGranted = true;
                for(Integer i : grantResults) {
                    permissionGranted = permissionGranted && (i == PackageManager.PERMISSION_GRANTED);
                }

                if(permissionGranted) {
                    mActivity.startAuthentication(mEdtEmail.getText().toString(),
                            mEdtApiKey.getText().toString(), mEdtPackageName
                                    .getText().toString(), mRestartMode.isChecked(), mAlternativeUrl);
                    mIsAuthenticated = true;
                } else {

                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ) {

                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                        alertDialog.setTitle("Information");
                        alertDialog.setMessage(getResources().getString(R.string.permission_needed));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                       dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    } else {
                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                        alertDialog.setTitle("Information");
                        alertDialog.setMessage(getResources().getString(R.string.location_permissions_mandatory));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                                    }
                                });
                        alertDialog.show();
                    }


                }
        }
    }

    /**
     * Checks for status of required Location permission
     * @return - status of required permission
     */
    private boolean checkPermission() {
        int status_fine = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        int status_coarse = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION);
        return (status_fine == PackageManager.PERMISSION_GRANTED) && (status_coarse == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Displays user dialog for runtime permission request
     */
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
    }
}
