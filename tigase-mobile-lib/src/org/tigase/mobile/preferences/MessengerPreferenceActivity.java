package org.tigase.mobile.preferences;

import org.tigase.mobile.Constants;
import org.tigase.mobile.R;

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class MessengerPreferenceActivity extends PreferenceActivity {

	private static final boolean DEBUG = false;

	private static final int MISSING_SETTING = 0;

	private static final int PICK_ACCOUNT = 1;
	
	private static final String TAG = "tigase";

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@TargetApi(14)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate() " + savedInstanceState);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			PreferenceScreen accounts = (PreferenceScreen) this.findPreference("accounts_manager");
			accounts.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intentChooser = AccountManager.newChooseAccountIntent(null, null,
							new String[] { Constants.ACCOUNT_TYPE }, true, null, null, null, null);
					startActivityForResult(intentChooser, PICK_ACCOUNT);					
					return true;
				}
				
			});
		}

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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_ACCOUNT) {
			if (data == null || data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) == null) {
				//this.finish();
				return;
			}

			String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			Log.v(TAG, "selected account = " + accName);
			Intent intent = new Intent(this, AccountPreferenceActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accName);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
		}
	}
}
