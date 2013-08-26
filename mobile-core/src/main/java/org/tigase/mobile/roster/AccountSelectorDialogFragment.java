/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.roster;

import java.util.ArrayList;

import org.tigase.mobile.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class AccountSelectorDialogFragment extends DialogFragment {

	public static AccountSelectorDialogFragment newInstance() {
		AccountSelectorDialogFragment frag = new AccountSelectorDialogFragment();
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Context context = getActivity();

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		AccountManager accountManager = AccountManager.get(context);
		ArrayList<String> accList = new ArrayList<String>();
		for (Account account : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
			accList.add(account.name);
		}

		final String[] items = accList.toArray(new String[] {});

		builder.setTitle("Select account");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {

				dismiss();
				Intent intent = new Intent(getActivity(), ContactEditActivity.class);
				intent.putExtra("account", items[item]);
				startActivityForResult(intent, 0);
			}
		});
		return builder.create();
	}

}
