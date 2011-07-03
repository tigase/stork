package org.tigase.mobile;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MessengerPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);

	}

}
