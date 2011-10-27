package org.tigase.mobile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class MessengerPreferenceActivity extends PreferenceActivity {

	private static final boolean DEBUG = false;

	private static final int MISSING_SETTING = 0;

	private static final String TAG = "tigase";

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate() " + savedInstanceState);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);

		// EditTextPreference awayPriority = (EditTextPreference)
		// findPreference("away_priority");
		// awayPriority.getEditText().setKeyListener(DigitsKeyListener.getInstance(false,
		// true));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MISSING_SETTING: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Please set login data").setCancelable(true).setIcon(android.R.drawable.ic_dialog_alert);
			return builder.create();
		}
		default:
			return null;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(TAG, "New INtent!!! " + intent);
		super.onNewIntent(intent);
		if (intent.getBooleanExtra("missingLogin", false)) {
			showDialog(MISSING_SETTING);
		}
	}

}
