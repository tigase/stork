package org.tigase.mobile;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class MessengerPreferenceActivity extends PreferenceActivity {

	private static final boolean DEBUG = true;

	private static final String TAG = "tigase";

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i(TAG, "Zmiana : " + newConfig);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);

		// EditTextPreference awayPriority = (EditTextPreference)
		// findPreference("away_priority");
		// awayPriority.getEditText().setKeyListener(DigitsKeyListener.getInstance(false,
		// true));
	}

}
