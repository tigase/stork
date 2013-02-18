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
