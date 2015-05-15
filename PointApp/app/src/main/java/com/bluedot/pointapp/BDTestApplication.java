package com.bluedot.pointapp;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import android.app.Application;
import com.bluedotinnovation.android.pointapp.R;

@ReportsCrashes(formKey = "", mode = ReportingInteractionMode.TOAST, resToastText = R.string.error_toast_message)
public class BDTestApplication extends Application {

	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);

		super.onCreate();
	}
}
