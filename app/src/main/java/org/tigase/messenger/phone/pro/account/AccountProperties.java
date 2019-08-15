/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.*;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.PushController;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.serverfeatures.ServerFeaturesActivity;
import org.tigase.messenger.phone.pro.serverfeatures.ServerFeaturesFragment;
import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.AccountCat;
import org.tigase.messenger.phone.pro.settings.AppCompatPreferenceActivity;
import org.tigase.messenger.phone.pro.settings.FingerprintPreference;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.JaXMPPSignalProtocolStore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.security.cert.X509Certificate;
import java.util.List;

public class AccountProperties
		extends AppCompatPreferenceActivity {

	public static final String ACCEPT_CERTIFICATE_KEY = "ACCEPT_CERTIFICATE";
	private final static String TAG = "AccountProperties";
	private Account account;
	//	private Account account;
	private Fragment settingsFragment;
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			if (AccountProperties.this.settingsFragment instanceof AccountPropertiesFragment) {
				((AccountPropertiesFragment) settingsFragment).checkMAM();
			}
		}

	};

	public static String getAccountName(Intent intent) {
		if (intent == null) {
			return null;
		}
		if (intent.getStringExtra("account_name") != null) {
			return intent.getStringExtra("account_name");
		}
		Account account = intent.getParcelableExtra("account");
		return account == null ? null : account.name;
	}

	public Jaxmpp getJaxmpp() {
		return mConnection.getService().getJaxmpp(account.name);
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		Intent intent = new Intent(getApplicationContext(), XMPPService.class);
		bindService(intent, mConnection, 0);
	}

	@Override
	public void onDetachedFromWindow() {
		unbindService(mConnection);
		super.onDetachedFromWindow();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.force_reconnect) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to reconnect this account now?").setTitle("Force reconnect");

			builder.setPositiveButton(R.string.yes, (dialog, which) -> {
				Intent i = new Intent();
				i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
				i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, getAccount().name);
				i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, true);
				sendBroadcast(i);
			});
			builder.setNegativeButton(R.string.no, (dialog, which) -> {
			});

			AlertDialog dialog = builder.create();
			dialog.show();

			return true;
		} else if (item.getItemId() == R.id.remove_account) {
			showRemoveAccountDialog();
			return true;
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
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

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (this.settingsFragment instanceof AccountPropertiesFragment) {
			getMenuInflater().inflate(R.menu.menu_account_pref, menu);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	Account getAccount() {
		if (account == null) {
			final String accountName = getAccountName(getIntent());
			this.account = AccountHelper.getAccount(getmAccountManager(), accountName);
		}
		return account;
	}

	AccountManager getmAccountManager() {
		return AccountManager.get(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String title;
		switch (getIntent() == null || getIntent().getAction() == null ? "" : getIntent().getAction()) {
			case "ACCOUNT_SETTINGS_SERVER_FEATURES":
				this.settingsFragment = new ServerFeaturesFragment();
				((ServerFeaturesFragment) this.settingsFragment).setAccount(getAccountName(getIntent()));
				title = "Server features";
				break;
			case "ACCOUNT_SETTINGS_SCREEN":
				this.settingsFragment = new ConnectionSettingsFragment();
				title = "Account settings";
				break;
			case "PRIORITIES_SCREEN":
				this.settingsFragment = new PrioritiesFragment();
				title = "Priorities";
				break;
			default:
				this.settingsFragment = new AccountPropertiesFragment();
				title = null;
		}

		setupActionBar(title);
		getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();

		if (getIntent() != null && getIntent().getAction() != null &&
				ACCEPT_CERTIFICATE_KEY.equals(getIntent().getAction())) {
			showCertificateDialog(getIntent());
		}
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

	private void showCertificateDialog(Intent intent) {

		final String account = intent.getExtras().getString("account_name");
		X509Certificate[] chain = (X509Certificate[]) intent.getExtras().getSerializable("chain");

		final CertificateDialogBuilder builder = new CertificateDialogBuilder(this, chain);
		builder.setTitle(this.getString(R.string.account_certificate_info_title))
				.setMessage(R.string.account_certificate_info_description)
				.setCancelable(true)
				.setPositiveButton(R.string.account_certificate_info_button_accept, (dialog, which) -> {
					SecureTrustManagerFactory.addCertificate(getApplicationContext(), chain[0]);

					Intent i = new Intent();
					i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
					i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account);
					i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, true);
					getApplication().sendBroadcast(i);
				})
				.setNegativeButton(R.string.account_certificate_info_button_reject, (dialog, which) -> {
				})
				.create()
				.show();

	}

	private void showRemoveAccountDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Remove account");
		builder.setMessage("Account " + getAccount().name + " will be removed. Are you sure?");

		builder.setNegativeButton(R.string.no, (dialog, which) -> {
		});
		builder.setPositiveButton(R.string.yes, (dialog, which) -> {
			Intent i = new Intent();
			i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
			i.putExtra(AccountManager.KEY_ACCOUNT_NAME, getAccount().name);

			getmAccountManager().removeAccount(getAccount(), null, null);

			sendBroadcast(i);

			AccountProperties.this.finish();
		});

		builder.create().show();
	}

	public static class AccountPropertiesFragment
			extends PreferenceFragment {

		private Account account;
		private AccountManager mAccountManager;
		private SwitchPreference mamAutoSync;
		private SwitchPreference mamEnabled;
		private ListPreference mamSynchronizationTime;
		private boolean modified = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			String tmp;

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.account_preferences);
			this.mAccountManager = ((AccountProperties) getActivity()).getmAccountManager();
			this.account = ((AccountProperties) getActivity()).getAccount();

			if (account == null || mAccountManager == null) {
				Toast.makeText(getActivity(), "Jebło", Toast.LENGTH_LONG).show();
				return;
			}

			AccountCat avatarPreference = (AccountCat) findPreference("pref_account_vcard");
			avatarPreference.setAccount(account);
			avatarPreference.setOnPreferenceClickListener(preference -> {
				try {
					Intent i = new Intent(getActivity(), VCardEditActivity.class);
					i.putExtra("account", account.name);
					getActivity().startActivity(i);
					getActivity().finish();
				} catch (Exception e) {
					Log.e("X", "WHY????", e);
				}
				return true;
			});

			PreferenceScreen connectionSettingsPreference = (PreferenceScreen) findPreference(
					"pref_account_connection");
			if (connectionSettingsPreference != null) {
				connectionSettingsPreference.getIntent().putExtra("account_name", account.name);
			}

			PreferenceScreen serverFeaures = (PreferenceScreen) findPreference("pref_account_server_features");
			if (serverFeaures != null) {
				serverFeaures.getIntent().putExtra(ServerFeaturesActivity.ACCOUNT_JID, account.name);
			}

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

			final PreferenceScreen prioritiesScreen = (PreferenceScreen) findPreference("account_priorites_screen");
			if (prioritiesScreen != null) {
				prioritiesScreen.getIntent().putExtra("account_name", account.name);
			}

			SwitchPreference autoPrioritiesPreference = (SwitchPreference) findPreference("account_priorites_enabled");
			if (autoPrioritiesPreference != null) {
				tmp = mAccountManager.getUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES);
				autoPrioritiesPreference.setChecked(tmp == null || Boolean.parseBoolean(tmp));
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
			}

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

			if (!PushController.isAvailable()) {
				PreferenceCategory pushCat = (PreferenceCategory) findPreference("pref_account_pushnotifications");
				getPreferenceScreen().removePreference(pushCat);
			} else {
				SwitchPreference pushNotificationPreference = (SwitchPreference) findPreference(
						"account_push_notification");
				tmp = mAccountManager.getUserData(account, AccountsConstants.PUSH_NOTIFICATION);
				pushNotificationPreference.setChecked(tmp != null && Boolean.parseBoolean(tmp));
				pushNotificationPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						mAccountManager.setUserData(account, AccountsConstants.PUSH_NOTIFICATION,
													Boolean.toString((Boolean) newValue));

						Intent action = new Intent(PushController.PUSH_NOTIFICATION_CHANGED);
						action.putExtra("account", account);
						action.putExtra("state", (Boolean) newValue);
						getActivity().sendBroadcast(action);

						return true;
					}
				});
			}

			this.mamEnabled = (SwitchPreference) findPreference("account_mam_enabled");
			this.mamAutoSync = (SwitchPreference) findPreference("account_mam_automatic_sync");
			this.mamSynchronizationTime = (ListPreference) findPreference("account_mam_synchronization");

			this.mamEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
				updateMAM((Boolean) newValue);
				return true;
			});
			this.mamAutoSync.setOnPreferenceChangeListener((preference, newValue) -> {
				mAccountManager.setUserData(account, AccountsConstants.MAM_AUTOSYNC,
											Boolean.toString((Boolean) newValue));
				updateSyncTimeField();
				return true;
			});
			this.mamSynchronizationTime.setOnPreferenceChangeListener((preference, newValue) -> {
				mAccountManager.setUserData(account, AccountsConstants.MAM_SYNC_TIME, newValue.toString());
				updateSyncTimeField();
				return true;
			});
			updateSyncTimeField();
		}

		@Override
		public void onPause() {
			if (modified) {
				sendBroadcast(true);
			}
			super.onPause();
		}

		void checkMAM() {
			showOMEMODetails();
			try {
				Jaxmpp jaxmpp = ((AccountProperties) getActivity()).getJaxmpp();
				MessageArchiveManagementModule mam = jaxmpp.getModule(MessageArchiveManagementModule.class);
				mam.retrieveSettings(new MessageArchiveManagementModule.SettingsCallback() {
					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						try {
							setMamSwitch(false, MessageArchiveManagementModule.DefaultValue.never);
						} catch (Exception e) {
							Log.w(TAG, "Cannot update switch", e);
						}
					}

					@Override
					public void onSuccess(MessageArchiveManagementModule.DefaultValue defValue, List<JID> always,
										  List<JID> never) throws JaxmppException {
						try {
							setMamSwitch(true, defValue);
						} catch (Exception e) {
							Log.w(TAG, "Cannot update switch", e);
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						try {
							setMamSwitch(false, MessageArchiveManagementModule.DefaultValue.never);
						} catch (Exception e) {
							Log.w(TAG, "Cannot update switch", e);
						}
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Cannot check MAM status", e);
			}
		}

		private void showOMEMODetails() {
			final Jaxmpp jaxmpp = ((AccountProperties) getActivity()).getJaxmpp();
			final JaXMPPSignalProtocolStore omemoStore = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
			final String jid = jaxmpp.getSessionObject().getUserBareJid().toString();
			final int localId = omemoStore.getLocalRegistrationId();

			FingerprintPreference omemoFingerprint = (FingerprintPreference) findPreference("omemo_fingerprint");
			omemoFingerprint.setFingerprint(omemoStore.getIdentityKeyPair().getPublicKey().serialize(), 1);

			PreferenceCategory otherDevices = (PreferenceCategory) findPreference("omemo_other");

			List<Integer> ids = omemoStore.getSubDevice(jid);
			boolean added = false;
			for (Integer id : ids) {
				if (id == localId) {
					continue;
				}
				FingerprintPreference checkBoxPref = new FingerprintPreference(getContext());

				checkBoxPref.setTitle("title");
				checkBoxPref.setSummary("summary");
				IdentityKey identity = omemoStore.getIdentity(new SignalProtocolAddress(jid, id));
				checkBoxPref.setFingerprint(identity.getPublicKey().serialize(), 1);

				otherDevices.addPreference(checkBoxPref);
				added = true;
			}

			if (!added) {
				getPreferenceScreen().removePreference(otherDevices);
			}

		}

		private void sendBroadcast(boolean forceDisconnect) {
			Intent i = new Intent();
			i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
			i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account.name);
			i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, forceDisconnect);
			getActivity().sendBroadcast(i);
			this.modified = false;
		}

		private void setMamSwitch(final boolean enabled, final MessageArchiveManagementModule.DefaultValue value) {
			Activity a = getActivity();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mamEnabled.setEnabled(enabled);
						mamEnabled.setChecked(value == MessageArchiveManagementModule.DefaultValue.roster);
					}
				});
				updateSyncTimeField();
			}
		}

		private void updateMAM(Boolean enabled) {
			MessageArchiveManagementModule.DefaultValue v = enabled
															? MessageArchiveManagementModule.DefaultValue.roster
															: MessageArchiveManagementModule.DefaultValue.never;
			try {
				Jaxmpp jaxmpp = ((AccountProperties) getActivity()).getJaxmpp();
				MessageArchiveManagementModule mam = jaxmpp.getModule(MessageArchiveManagementModule.class);
				mam.updateSetttings(v, null, null, new MessageArchiveManagementModule.SettingsCallback() {
					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						checkMAM();
					}

					@Override
					public void onSuccess(MessageArchiveManagementModule.DefaultValue defValue, List<JID> always,
										  List<JID> never) throws JaxmppException {
						checkMAM();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						checkMAM();

					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Cannot update MAM status", e);
			}
		}

		private void updateSyncTimeField() {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					boolean enabled = mamEnabled.isChecked();

					String syncTime = mAccountManager.getUserData(account, AccountsConstants.MAM_SYNC_TIME);
					String autoSync = mAccountManager.getUserData(account, AccountsConstants.MAM_AUTOSYNC);

					mamAutoSync.setChecked(autoSync == null || Boolean.parseBoolean(autoSync));
//					mamAutoSync.setEnabled(enabled);

					final String[] vs = getResources().getStringArray(R.array.account_mam_sync_values);
					final String[] ls = getResources().getStringArray(R.array.account_mam_sync_labels);

					if (syncTime == null) {
						mAccountManager.setUserData(account, AccountsConstants.MAM_SYNC_TIME, "24");
						syncTime = "24";
					}

					final String value = syncTime;

					int p = 0;
					if (value != null) {
						for (int i = 0; i < vs.length; i++) {
							if (vs[i].equals(value)) {
								p = i;
							}
						}
					}
					mamSynchronizationTime.setValue(syncTime);
					mamSynchronizationTime.setSummary(ls[p]);
					mamSynchronizationTime.setEnabled(mamAutoSync.isChecked());
				}
			});

		}
	}

	public static class ConnectionSettingsFragment
			extends PreferenceFragment {

		private Account account;
		private AccountManager mAccountManager;

		private boolean modified = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.connection_preferences);
			this.mAccountManager = ((AccountProperties) getActivity()).getmAccountManager();
			this.account = ((AccountProperties) getActivity()).getAccount();

			EditTextPreference accountIdPreference = (EditTextPreference) findPreference("account_id");
			accountIdPreference.setTitle(account.name);

			EditTextPreference passwordPreference = (EditTextPreference) findPreference("account_password");
			passwordPreference.setText("");
			passwordPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					mAccountManager.setPassword(account, newValue.toString());
					modified = true;
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
		}

		@Override
		public void onPause() {
			if (this.modified) {
				Intent i = new Intent();
				i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
				i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account.name);
				i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, true);
				getActivity().sendBroadcast(i);
			}
			super.onPause();
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

}
