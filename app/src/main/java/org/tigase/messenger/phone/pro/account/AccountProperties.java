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
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.settings.AbstractServicePreferencesActivity;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.android.Jaxmpp;

public class AccountProperties
		extends AbstractServicePreferencesActivity {

	public static final String ACCEPT_CERTIFICATE_KEY = "ACCEPT_CERTIFICATE";
	private final static String TAG = "AccountProperties";
	private Account account;
	private AccountManager accountManager;

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

	public Account getAccount() {
		return account;
	}

	public AccountManager getAccountManager() {
		return accountManager;
	}

	public Jaxmpp getJaxmpp() {
		return getJaxmpp(this.account.name);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		this.accountManager = AccountManager.get(this);
		final String accountName = getAccountName(getIntent());
		this.account = AccountHelper.getAccount(accountManager, accountName);
	}

	@Override
	protected PreferenceFragmentCompat getDefaultFragment() {
		return new AccountPropertiesFragment();
	}
}
