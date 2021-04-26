/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.preference.*;
import org.tigase.messenger.phone.pro.PushController;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.AbstractServicePreferencesActivity;
import org.tigase.messenger.phone.pro.settings.FingerprintPreference;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.JaXMPPSignalProtocolStore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.List;

public class AccountPropertiesFragment
		extends PreferenceFragmentCompat
		implements AbstractServicePreferencesActivity.XmppServiceAware {

	private final static String TAG = "AccountPropertiesFrg";

	private Account account;
	private AccountManager accountManager;
	private SwitchPreference mamAutoSync;
	private SwitchPreference mamEnabled;
	private ListPreference mamSynchronizationTime;
	private boolean modified = false;

	@Override
	public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.accountManager = ((AccountProperties) getActivity()).getAccountManager();
		this.account = ((AccountProperties) getActivity()).getAccount();

		Preference avatarPreference = findPreference("pref_account_vcard");

		Bitmap avatar = AvatarHelper.getAvatar(getContext(), BareJID.bareJIDInstance(account.name), true);
		if (avatar == null) {
			avatar = AvatarHelper.createInitialAvatar(BareJID.bareJIDInstance(account.name), null);
		}

		avatarPreference.setIcon(new BitmapDrawable(getResources(), avatar));
		avatarPreference.setTitle(account.name);
		avatarPreference.setIconSpaceReserved(true);

		avatarPreference.setOnPreferenceClickListener(preference -> {
			try {
				Intent i = new Intent(getActivity(), VCardEditActivity.class);
				i.putExtra("account", account.name);
				getActivity().startActivity(i);
				getActivity().finish();
			} catch (Exception e) {
				Log.wtf(TAG, "WHY????", e);
			}
			return true;
		});

		SwitchPreference accountEnabledPreference = findPreference("accountEnabled");
		boolean active = Boolean.parseBoolean(accountManager.getUserData(account, AccountsConstants.FIELD_ACTIVE));
		accountEnabledPreference.setChecked(active);
		accountEnabledPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				accountManager.setUserData(account, AccountsConstants.FIELD_ACTIVE, newValue.toString());
				modified = true;
				sendBroadcast(true);
				return true;
			}
		});

		final PreferenceScreen prioritiesScreen = findPreference("account_priorites_screen");
		if (prioritiesScreen != null) {
			prioritiesScreen.getExtras().putString("account_name", account.name);
		}

		SwitchPreference autoPrioritiesPreference = findPreference("account_priorites_enabled");
		if (autoPrioritiesPreference != null) {
			String tmp = accountManager.getUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES);
			autoPrioritiesPreference.setChecked(tmp == null || Boolean.parseBoolean(tmp));
			autoPrioritiesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					accountManager.setUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES,
											   Boolean.toString((Boolean) newValue));
					prioritiesScreen.setEnabled(!(Boolean) newValue);
					sendBroadcast(false);
					return true;
				}
			});
			prioritiesScreen.setEnabled(!autoPrioritiesPreference.isChecked());
		}

		String nickname = accountManager.getUserData(account, AccountsConstants.FIELD_NICKNAME);
		EditTextPreference nicknamePreference = findPreference("account_nickname");
		nicknamePreference.setText(nickname);
		nicknamePreference.setSummary(nickname);
		nicknamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String x = newValue == null || newValue.toString().trim().isEmpty() ? null : newValue.toString().trim();
				nicknamePreference.setText(x);
				nicknamePreference.setSummary(x);
				accountManager.setUserData(account, AccountsConstants.FIELD_NICKNAME, x);

				modified = true;
				return true;
			}
		});

		if (!PushController.isAvailable()) {
			PreferenceCategory pushCat = findPreference("pref_account_pushnotifications");
			getPreferenceScreen().removePreference(pushCat);
		} else {
			SwitchPreference pushNotificationPreference = findPreference("account_push_notification");
			String tmp = accountManager.getUserData(account, AccountsConstants.PUSH_NOTIFICATION);
			pushNotificationPreference.setChecked(tmp != null && Boolean.parseBoolean(tmp));
			pushNotificationPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					accountManager.setUserData(account, AccountsConstants.PUSH_NOTIFICATION,
											   Boolean.toString((Boolean) newValue));

					Intent action = new Intent(PushController.PUSH_NOTIFICATION_CHANGED);
					action.putExtra("account", account);
					action.putExtra("state", (Boolean) newValue);
					getActivity().sendBroadcast(action);

					return true;
				}
			});
		}

		this.mamEnabled = findPreference("account_mam_enabled");
		this.mamAutoSync = findPreference("account_mam_automatic_sync");
		this.mamSynchronizationTime = findPreference("account_mam_synchronization");

		this.mamEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
			updateMAM((Boolean) newValue);
			return true;
		});
		this.mamAutoSync.setOnPreferenceChangeListener((preference, newValue) -> {
			accountManager.setUserData(account, AccountsConstants.MAM_AUTOSYNC, Boolean.toString((Boolean) newValue));
			updateSyncTimeField();
			return true;
		});
		this.mamSynchronizationTime.setOnPreferenceChangeListener((preference, newValue) -> {
			accountManager.setUserData(account, AccountsConstants.MAM_SYNC_TIME, newValue.toString());
			updateSyncTimeField();
			return true;
		});
		updateSyncTimeField();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.account_preferences);
	}

	@Override
	public void setXmppService(XMPPService service) {
		if (service != null) {
			checkMAM();
		}
	}

	void checkMAM() {
		showOMEMODetails();
		try {
			Jaxmpp jaxmpp = ((AccountProperties) getActivity()).getJaxmpp();
			if (jaxmpp == null) {
				return;
			}
			MessageArchiveManagementModule mam = jaxmpp.getModule(MessageArchiveManagementModule.class);
			mam.retrieveSettings(new MessageArchiveManagementModule.SettingsCallback() {
				@Override
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
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

	private void showOMEMODetails() {
		final Jaxmpp jaxmpp = ((AccountProperties) getActivity()).getJaxmpp();
		if (jaxmpp == null) {
			return;
		}
		final JaXMPPSignalProtocolStore omemoStore = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		final String jid = jaxmpp.getSessionObject().getUserBareJid().toString();
		final int localId = omemoStore.getLocalRegistrationId();

		FingerprintPreference omemoFingerprint = findPreference("omemo_fingerprint");
		omemoFingerprint.setFingerprint(omemoStore.getIdentityKeyPair().getPublicKey().serialize(), 1);

		PreferenceCategory otherDevices = findPreference("omemo_other");

		List<Integer> ids = omemoStore.getSubDevice(jid);
		boolean added = false;
		for (Integer id : ids) {
			if (id == localId) {
				continue;
			}
			FingerprintPreference checkBoxPref = new FingerprintPreference(getActivity());

			checkBoxPref.setTitle("title");
			checkBoxPref.setSummary("summary");
			IdentityKey identity = omemoStore.getIdentity(new SignalProtocolAddress(jid, id));

			if (identity == null) {
				Log.w(TAG, "Whe the hell there is no Identity for " + jid + ":" + id + "?");
				continue;
			}

			checkBoxPref.setFingerprint(identity.getPublicKey().serialize(), 1);

			otherDevices.addPreference(checkBoxPref);
			added = true;
		}

		if (!added) {
			getPreferenceScreen().removePreference(otherDevices);
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
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
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

				String syncTime = accountManager.getUserData(account, AccountsConstants.MAM_SYNC_TIME);
				String autoSync = accountManager.getUserData(account, AccountsConstants.MAM_AUTOSYNC);

				mamAutoSync.setChecked(autoSync == null || Boolean.parseBoolean(autoSync));
//					mamAutoSync.setEnabled(enabled);

				final String[] vs = getResources().getStringArray(R.array.account_mam_sync_values);
				final String[] ls = getResources().getStringArray(R.array.account_mam_sync_labels);

				if (syncTime == null) {
					accountManager.setUserData(account, AccountsConstants.MAM_SYNC_TIME, "24");
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

	private void sendBroadcast(boolean forceDisconnect) {
		Intent i = new Intent();
		i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
		i.putExtra(LoginActivity.KEY_ACCOUNT_NAME, account.name);
		i.putExtra(LoginActivity.KEY_FORCE_DISCONNECT, forceDisconnect);
		getActivity().sendBroadcast(i);
		this.modified = false;
	}
}
