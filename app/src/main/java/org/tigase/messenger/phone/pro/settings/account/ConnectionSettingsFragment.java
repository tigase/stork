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
package org.tigase.messenger.phone.pro.settings.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountProperties;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.LoginActivity;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.AbstractServicePreferencesActivity;

public class ConnectionSettingsFragment
		extends PreferenceFragmentCompat
		implements AbstractServicePreferencesActivity.XmppServiceAware {

	private Account account;
	private EditTextPreference accountIdPreference;
	private EditTextPreference hostnamePreference;
	private AccountManager mAccountManager;
	private boolean modified = false;
	private EditTextPreference passwordPreference;
	private EditTextPreference resourcePreference;

	@Override
	public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mAccountManager = ((AccountProperties) getActivity()).getAccountManager();
		this.account = ((AccountProperties) getActivity()).getAccount();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.connection_preferences);

		this.accountIdPreference = findPreference("account_id");
		this.passwordPreference = findPreference("account_password");
		this.hostnamePreference = findPreference("account_hostname");
		this.resourcePreference = findPreference("account_resource");

		passwordPreference.setOnPreferenceChangeListener((preference, newValue) -> {
			updatePassword(newValue.toString());
			return true;
		});
		hostnamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
			updateHostname(newValue.toString());
			return true;
		});
		resourcePreference.setOnPreferenceChangeListener((preference, newValue) -> {
			updateResource(newValue.toString());
			return true;
		});
	}

	@Override
	public void setXmppService(XMPPService service) {
		accountIdPreference.setTitle(account.name);

		passwordPreference.setText("");

		String hostname = mAccountManager.getUserData(account, AccountsConstants.FIELD_HOSTNAME);
		hostnamePreference.setText(hostname);
		hostnamePreference.setSummary(hostname == null || hostname.trim().isEmpty() ? "(default)" : hostname);

		String resource = mAccountManager.getUserData(account, AccountsConstants.FIELD_RESOURCE);
		resourcePreference.setText(resource);
		resourcePreference.setSummary(resource);
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

	private void updateResource(String newValue) {
		String x = newValue == null || newValue.trim().isEmpty() ? null : newValue.trim();
		resourcePreference.setText(x);
		resourcePreference.setSummary(x);
		mAccountManager.setUserData(account, AccountsConstants.FIELD_RESOURCE, x);

		modified = true;
	}

	private void updateHostname(String newValue) {
		String hostname = newValue == null || newValue.trim().isEmpty() ? null : newValue.trim();
		mAccountManager.setUserData(account, AccountsConstants.FIELD_HOSTNAME, hostname);
		hostnamePreference.setText(hostname);
		hostnamePreference.setSummary(hostname == null || hostname.trim().isEmpty() ? "(default)" : hostname);

		modified = true;
	}

	private void updatePassword(String value) {
		mAccountManager.setPassword(account, value);
		modified = true;

	}
}
