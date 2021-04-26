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
package org.tigase.messenger.phone.pro.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountProperties;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.account.NewAccountActivity;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;

public class AccountsPreferenceFragment
		extends PreferenceFragmentCompat {

	private FloatingActionButton floatingActionButton;

	private PreferenceScreen screen;

	static boolean isAccountActive(AccountManager accountManager, Account account) {
		return Boolean.parseBoolean(accountManager.getUserData(account, AccountsConstants.FIELD_ACTIVE));
	}

//	@Override
//	public View onCreateView(@NonNull @NotNull LayoutInflater inflater,
//							 @Nullable @org.jetbrains.annotations.Nullable ViewGroup container,
//							 @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
//
//		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//																				 ViewGroup.LayoutParams.WRAP_CONTENT);
//		layoutParams.setMargins(32, 32, 32, 32);
//		layoutParams.setMarginEnd();//gravity = Gravity.RIGHT | Gravity.BOTTOM | Gravity.END;
//		floatingActionButton.setLayoutParams(layoutParams);
//		floatingActionButton.setImageResource(R.drawable.ic_add);
////		floatingActionButton.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark, null));
//		floatingActionButton.setOnClickListener(
//				v -> Toast.makeText(getContext(), "You clicked Floating Action Button", Toast.LENGTH_SHORT).show());
////		container.addView(floatingActionButton, layoutParams);
//
//
//		return super.onCreateView(inflater, container, savedInstanceState);
//	}

	@Override
	public void onDestroyView() {
		floatingActionButton.hide();
		super.onDestroyView();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.pref_accounts_list, rootKey);
		this.screen = getPreferenceScreen();
		addAllAccounts();

		Preference addAccountPref = new Preference(screen.getContext());
		addAccountPref.setIntent(new Intent(screen.getContext(), NewAccountActivity.class));
		addAccountPref.setTitle(getActivity().getString(R.string.pref_accounts_newaccount));
		addAccountPref.setIcon(android.R.drawable.ic_input_add);
		screen.addPreference(addAccountPref);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.floatingActionButton = new FloatingActionButton(getContext());
		((SettingsActivity) getActivity()).getEventBus()
				.addHandler(Connector.StateChangedHandler.StateChangedEvent.class,
							(sessionObject, oldState, newState) -> updateAccount(sessionObject.getUserBareJid(),
																				 newState));

	}

	private Connector.State calculateState(final JaxmppCore jaxmpp) {
		final Connector.State connectorState =
				jaxmpp.getConnector() == null ? Connector.State.disconnected : jaxmpp.getConnector().getState();
		final boolean established = jaxmpp.isConnected();
		if (connectorState == Connector.State.connected && !established) {
			return Connector.State.connecting;
		} else {
			return connectorState;
		}
	}

	private void addAllAccounts() {
		final AccountManager am = AccountManager.get(screen.getContext());
		for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {

			Preference category = new Preference(screen.getContext());
			category.setTitle(account.name);
			category.setIconSpaceReserved(true);
			category.setIcon(R.drawable.stork_logo);

			Intent x = new Intent(screen.getContext(), AccountProperties.class);
			x.putExtra("account_name", account.name);
			category.setIntent(x);
			screen.addPreference(category);
		}
	}

	private void updateAccount(final BareJID account, final Connector.State newState) {
		for (int i = 0; i < this.screen.getPreferenceCount(); i++) {
			Preference p = this.screen.getPreference(i);
			if (p instanceof AccountCat && ((AccountCat) p).getAccountName().equals(account.toString())) {
				((AccountCat) p).setState(newState);
			}
		}
	}

	//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		addPreferencesFromResource(R.xml.pref_accounts_list);
//		this.screen = this.getPreferenceScreen();
//		((SettingsActivity) getActivity()).getEventBus()
//				.addListener(Connector.StateChangedHandler.StateChangedEvent.class, event -> {
//					BaseAdapter adapter = (BaseAdapter) screen.getRootAdapter();
//					if (getActivity() != null) {
//						getActivity().runOnUiThread(adapter::notifyDataSetChanged);
//					}
//				});
//	}

//	@Override
//	public void onResume() {
//		super.onResume();
//		screen.removeAll();
//
//		setHasOptionsMenu(true);
//
//		AccountManager am = AccountManager.get(screen.getContext());
//
//		for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
//			AccountCat category = AccountCat.instance(screen.getContext(), account,
//													  (SettingsActivity) getActivity());
//			Intent x = new Intent(screen.getContext(), AccountProperties.class);
//			x.putExtra("account_name", account.name);
//			category.setIntent(x);
//			screen.addPreference(category);
//		}
//
//		Preference addAccountPref = new Preference(screen.getContext());
//		addAccountPref.setIntent(new Intent(screen.getContext(), NewAccountActivity.class));
//		addAccountPref.setTitle(getActivity().getString(R.string.pref_accounts_newaccount));
//		addAccountPref.setIcon(android.R.drawable.ic_input_add);
//		screen.addPreference(addAccountPref);
//	}

}
