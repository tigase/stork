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
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.*;

public class PrioritiesFragment
		extends PreferenceFragmentCompat {

	private final static String DIALOG_FRAGMENT_TAG = "NumberPickerDialog";
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
	public void onDisplayPreferenceDialog(Preference preference) {
		if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
			return;
		}
		if (preference instanceof NumberPickerPreference) {
			NumberPickerPreference.NumberPickerPreferenceDialog dialog = NumberPickerPreference.NumberPickerPreferenceDialog
					.newInstance(preference.getKey());
			dialog.setTargetFragment(this, 0);
			dialog.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.priorities_preferences);

		this.mAccountManager = ((AccountProperties) getActivity()).getAccountManager();
		this.account = ((AccountProperties) getActivity()).getAccount();

		this.npChat = findPreference("pr_chat");
		this.npChat.setOnPreferenceChangeListener(this::changeListener);
		this.npOnline = findPreference("pr_online");
		this.npOnline.setOnPreferenceChangeListener(this::changeListener);
		this.npAway = findPreference("pr_away");
		this.npAway.setOnPreferenceChangeListener(this::changeListener);
		this.npXa = findPreference("pr_xa");
		this.npXa.setOnPreferenceChangeListener(this::changeListener);
		this.npDnd = findPreference("pr_dnd");
		this.npDnd.setOnPreferenceChangeListener(this::changeListener);

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
