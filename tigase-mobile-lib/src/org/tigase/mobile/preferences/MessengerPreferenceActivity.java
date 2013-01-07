package org.tigase.mobile.preferences;

import org.tigase.mobile.Constants;
import org.tigase.mobile.R;

import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

public class MessengerPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final boolean DEBUG = false;

	private static final int MISSING_SETTING = 0;

	private static final int PICK_ACCOUNT = 1;

	private static final String TAG = "tigase";

	private void initSummary(Preference p) {
		if (p instanceof PreferenceScreen) {
			PreferenceScreen pCat = (PreferenceScreen) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updateSummary(p.getKey());
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_ACCOUNT) {
			if (data == null || data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) == null) {
				// this.finish();
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
		initSummary(getPreferenceScreen());
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
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateSummary(key);
	}

	private void updateSummary(String key) {
		Preference p = findPreference(key);
		if (p instanceof EditTextPreference) {
			final EditTextPreference pref = (EditTextPreference) p;
			if ("reconnect_time".equals(key)) {
				pref.setSummary(getResources().getString(R.string.pref_reconnect_time_summary, pref.getText()));
				this.onContentChanged();
			} else if ("default_priority".equals(key)) {
				pref.setSummary(getResources().getString(R.string.pref_default_priority_summary, pref.getText()));
				this.onContentChanged();
			} else if ("away_priority".equals(key)) {
				pref.setSummary(getResources().getString(R.string.pref_auto_away_priority_summary, pref.getText()));
				this.onContentChanged();
			} else if ("keepalive_time".equals(key)) {
				pref.setSummary(getResources().getString(R.string.pref_keepalive_time_summary, pref.getText()));
				this.onContentChanged();
			}
		}
	}
}
