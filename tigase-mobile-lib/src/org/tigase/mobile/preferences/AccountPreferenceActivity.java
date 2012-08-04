package org.tigase.mobile.preferences;

import org.tigase.mobile.R;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class AccountPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.account_preferences);
		
		Intent intent = getIntent();
//	}
//	
//	protected void onNewIntent(Intent intent) {
//		super.onNewIntent(intent);
		String account = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		
		Log.v("PREF", "got account = " + account);
		
		Preference pref = this.findPreference("auth");
		pref.getIntent().putExtra("account_jid", account);
		pref = this.findPreference("vcard");
		pref.getIntent().putExtra("account_jid", account);
		pref = this.findPreference("advanced");
		pref.getIntent().putExtra("account_jid", account);
		
	}
	
}
