package org.tigase.messenger.phone.pro.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.*;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.settings.AppCompatPreferenceActivity;

public class AccountProperties
		extends AppCompatPreferenceActivity {

	private Account account;
	private AccountManager mAccountManager;

	static String getAccountName(Intent intent) {
		if (intent == null) {
			return null;
		}
		if (intent.getStringExtra("account_name") != null) {
			return intent.getStringExtra("account_name");
		}
		if (intent != null) {
			Account account = intent.getParcelableExtra("account");
			return account == null ? null : account.name;
		}
		return null;
	}

	Account getAccount() {
		return account;
	}

	private Account getAccount(String name) {
		for (Account account : mAccountManager.getAccounts()) {
			if (account.name.equals(name)) {
				return account;
			}
		}
		return null;
	}

	AccountManager getmAccountManager() {
		return mAccountManager;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAccountManager = AccountManager.get(this);

		final String accountName = getAccountName(getIntent());
		this.account = getAccount(accountName);

		Fragment settingsFragment;
		String title;
		switch (getIntent() == null || getIntent().getAction() == null ? "" : getIntent().getAction()) {
			case "PRIORITIES_SCREEN":
				settingsFragment = new PrioritiesFragment();
				title = "Priorities";
				break;
			default:
				settingsFragment = new SettingsFragment();
				title = null;
		}

		setupActionBar(title);
		getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 *
	 * @param title
	 */
	private void setupActionBar(String title) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
			if (title != null) {
				actionBar.setTitle(title);
			}
		}
	}

	public static class PrioritiesFragment
			extends PreferenceFragment {

		private Account account;
		private AccountManager mAccountManager;
		private boolean modified = false;
		private NumberPickerPreference npAway;
		private NumberPickerPreference npChat;
		private NumberPickerPreference npDnd;
		private NumberPickerPreference npOnline;
		private NumberPickerPreference npXa;
		private PrioritiesEntity pr;

		private boolean changeListener(Preference preference, Object o) {
			switch (preference.getKey()) {
				case "pr_chat":
					pr.setChat((Integer) o);
					break;
				case "pr_online":
					pr.setOnline((Integer) o);
					break;
				case "pr_away":
					pr.setAway((Integer) o);
					break;
				case "pr_xa":
					pr.setXa((Integer) o);
					break;
				case "pr_dnd":
					pr.setDnd((Integer) o);
					break;
			}

			mAccountManager.setUserData(this.account, AccountsConstants.CUSTOM_PRIORITIES, pr.toString());

			updateSummaries();
			modified = true;
			return true;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.priorities_preferences);
			this.mAccountManager = ((AccountProperties) getActivity()).getmAccountManager();
			this.account = ((AccountProperties) getActivity()).getAccount();

			this.npChat = (NumberPickerPreference) findPreference("pr_chat");
			this.npChat.setOnPreferenceChangeListener((preference, o) -> changeListener(preference, o));
			this.npOnline = (NumberPickerPreference) findPreference("pr_online");
			this.npOnline.setOnPreferenceChangeListener((preference, o) -> changeListener(preference, o));
			this.npAway = (NumberPickerPreference) findPreference("pr_away");
			this.npAway.setOnPreferenceChangeListener((preference, o) -> changeListener(preference, o));
			this.npXa = (NumberPickerPreference) findPreference("pr_xa");
			this.npXa.setOnPreferenceChangeListener((preference, o) -> changeListener(preference, o));
			this.npDnd = (NumberPickerPreference) findPreference("pr_dnd");
			this.npDnd.setOnPreferenceChangeListener((preference, o) -> changeListener(preference, o));

			this.pr = PrioritiesEntity.instance(
					mAccountManager.getUserData(this.account, AccountsConstants.CUSTOM_PRIORITIES));

			updateSummaries();
		}

		@Override
		public void onPause() {
			if (this.modified) {
				Intent i = new Intent();
				i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
				i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account.name);
				i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, false);
				getActivity().sendBroadcast(i);
			}
			super.onPause();
		}

		private void updateSummaries() {
			this.npChat.setSummary(String.valueOf(pr.getChat()));
			this.npChat.setValue(pr.getChat());
			this.npOnline.setSummary(String.valueOf(pr.getOnline()));
			this.npOnline.setValue(pr.getOnline());
			this.npAway.setSummary(String.valueOf(pr.getAway()));
			this.npAway.setValue(pr.getAway());
			this.npXa.setSummary(String.valueOf(pr.getXa()));
			this.npXa.setValue(pr.getXa());
			this.npDnd.setSummary(String.valueOf(pr.getDnd()));
			this.npDnd.setValue(pr.getDnd());
		}

	}

	public static class SettingsFragment
			extends PreferenceFragment {

		private Account account;
		private boolean modified = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.account_preferences);
			final AccountManager mAccountManager = ((AccountProperties) getActivity()).getmAccountManager();
			this.account = ((AccountProperties) getActivity()).getAccount();

			SwitchPreference accountEnabledPreference = (SwitchPreference) findPreference("accountEnabled");
			boolean active = Boolean.parseBoolean(mAccountManager.getUserData(account, AccountsConstants.FIELD_ACTIVE));
			accountEnabledPreference.setChecked(active);
			accountEnabledPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					mAccountManager.setUserData(account, AccountsConstants.FIELD_ACTIVE, newValue.toString());
					modified = true;
					sendBroadcast(true);
					return true;
				}
			});

			EditTextPreference accountIdPreference = (EditTextPreference) findPreference("account_id");
			accountIdPreference.setTitle(account.name);

			EditTextPreference passwordPreference = (EditTextPreference) findPreference("account_password");
			passwordPreference.setText("");
			passwordPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					mAccountManager.setPassword(account, newValue.toString());
					modified = true;
					sendBroadcast(true);
					return true;
				}
			});

			String hostname = mAccountManager.getUserData(account, AccountsConstants.FIELD_HOSTNAME);
			EditTextPreference hostnamePreference = (EditTextPreference) findPreference("account_hostname");
			hostnamePreference.setText(hostname);
			hostnamePreference.setSummary(hostname == null || hostname.trim().isEmpty() ? "(default)" : hostname);
			hostnamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String hostname = newValue == null || newValue.toString().trim().isEmpty()
									  ? null
									  : newValue.toString().trim();
					mAccountManager.setUserData(account, AccountsConstants.FIELD_HOSTNAME, hostname);
					hostnamePreference.setText(hostname);
					hostnamePreference.setSummary(
							hostname == null || hostname.trim().isEmpty() ? "(default)" : hostname);

					modified = true;
					return true;
				}
			});

			String resource = mAccountManager.getUserData(account, AccountsConstants.FIELD_RESOURCE);
			EditTextPreference resourcePreference = (EditTextPreference) findPreference("account_resource");
			resourcePreference.setText(resource);
			resourcePreference.setSummary(resource);
			resourcePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String x = newValue == null || newValue.toString().trim().isEmpty()
							   ? null
							   : newValue.toString().trim();
					resourcePreference.setText(x);
					resourcePreference.setSummary(x);
					mAccountManager.setUserData(account, AccountsConstants.FIELD_RESOURCE, x);

					modified = true;
					return true;
				}
			});

			PreferenceScreen loginDetailsScreen = (PreferenceScreen) findPreference("loginDetails");
			if (loginDetailsScreen != null) {
				loginDetailsScreen.getIntent().putExtra("account_name", account.name);
			}

			final PreferenceScreen prioritiesScreen = (PreferenceScreen) findPreference("account_priorites_screen");
			if (prioritiesScreen != null) {
				prioritiesScreen.getIntent().putExtra("account_name", account.name);
			}

			SwitchPreference autoPrioritiesPreference = (SwitchPreference) findPreference("account_priorites_enabled");
			String tmp = mAccountManager.getUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES);
			autoPrioritiesPreference.setChecked(tmp == null ? true : Boolean.parseBoolean(tmp));
			autoPrioritiesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					mAccountManager.setUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES,
												Boolean.toString((Boolean) newValue));
					prioritiesScreen.setEnabled(!(Boolean) newValue);

					sendBroadcast(false);
					return true;
				}
			});
			prioritiesScreen.setEnabled(!autoPrioritiesPreference.isChecked());

			String nickname = mAccountManager.getUserData(account, AccountsConstants.FIELD_NICKNAME);
			EditTextPreference nicknamePreference = (EditTextPreference) findPreference("account_nickname");
			nicknamePreference.setText(nickname);
			nicknamePreference.setSummary(nickname);
			nicknamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String x = newValue == null || newValue.toString().trim().isEmpty()
							   ? null
							   : newValue.toString().trim();
					nicknamePreference.setText(x);
					nicknamePreference.setSummary(x);
					mAccountManager.setUserData(account, AccountsConstants.FIELD_NICKNAME, x);

					modified = true;
					return true;
				}
			});

			Preference reconnectPreference = findPreference("account_force_reconnect");
			reconnectPreference.setOnPreferenceClickListener(preference -> {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("Are you sure you want to reconnect this account now?").setTitle("Force reconnect");

				builder.setPositiveButton(R.string.yes, (dialog, which) -> sendBroadcast(true));
				builder.setNegativeButton(R.string.no, (dialog, which) -> {
				});

				AlertDialog dialog = builder.create();
				dialog.show();

				return true;
			});
		}

		@Override
		public void onPause() {
			if (modified) {
				sendBroadcast(true);
			}
			super.onPause();
		}

		private void sendBroadcast(boolean forceDisconnect) {
			Intent i = new Intent();
			i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
			i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account.name);
			i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, forceDisconnect);
			getActivity().sendBroadcast(i);
			this.modified = false;
		}
	}

}
