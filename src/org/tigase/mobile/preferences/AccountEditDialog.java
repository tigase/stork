package org.tigase.mobile.preferences;

import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.providers.AccountsProvider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class AccountEditDialog extends DialogFragment {

	public static AccountEditDialog newInstance() {
		return new AccountEditDialog(null);
	}

	public static AccountEditDialog newInstance(long id) {
		return new AccountEditDialog(id);
	}

	private Long accountId = null;

	private AccountEditDialog(Long id) {
		this.accountId = id;
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.account_edit_dialog, container, false);
		final Button addButton = (Button) v.findViewById(R.id.newAccountAddButton);
		final Button cancelButton = (Button) v.findViewById(R.id.newAccountcancelButton);

		final EditText jid = (EditText) v.findViewById(R.id.newAccountJID);
		final EditText passowrd = (EditText) v.findViewById(R.id.newAccountPassowrd);
		final EditText nickname = (EditText) v.findViewById(R.id.newAccountNickname);
		final EditText hostname = (EditText) v.findViewById(R.id.newAccountHostname);

		if (accountId != null) {
			Cursor c = inflater.getContext().getContentResolver().query(
					ContentUris.withAppendedId(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), accountId), null, null, null,
					null);
			try {
				while (c.moveToNext()) {
					jid.setText(c.getString(c.getColumnIndex(AccountsTableMetaData.FIELD_JID)));
					passowrd.setText(c.getString(c.getColumnIndex(AccountsTableMetaData.FIELD_PASSWORD)));
					nickname.setText(c.getString(c.getColumnIndex(AccountsTableMetaData.FIELD_NICKNAME)));
					hostname.setText(c.getString(c.getColumnIndex(AccountsTableMetaData.FIELD_HOSTNAME)));
				}
			} finally {
				c.close();
			}
		}

		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(AccountsTableMetaData.FIELD_JID, jid.getText().toString());
				values.put(AccountsTableMetaData.FIELD_PASSWORD, passowrd.getText().toString());
				values.put(AccountsTableMetaData.FIELD_NICKNAME, nickname.getText().toString());
				values.put(AccountsTableMetaData.FIELD_HOSTNAME, hostname.getText().toString());

				if (accountId == null)
					inflater.getContext().getContentResolver().insert(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), values);
				else
					inflater.getContext().getContentResolver().update(
							ContentUris.withAppendedId(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), accountId), values, null,
							null);

			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();

			}
		});

		getDialog().setTitle("Add new account");

		return v;
	}
}
