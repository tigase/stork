package org.tigase.mobile.preferences;

import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.providers.AccountsProvider;

import android.content.ContentValues;
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
		return new AccountEditDialog();
	}

	private Long accountId = null;

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.account_edit_dialog, container, false);
		final Button addButton = (Button) v.findViewById(R.id.newAccountAddButton);
		final Button cancelButton = (Button) v.findViewById(R.id.newAccountcancelButton);

		final EditText jid = (EditText) v.findViewById(R.id.newAccountJID);
		final EditText passowrd = (EditText) v.findViewById(R.id.newAccountPassowrd);
		final EditText nickname = (EditText) v.findViewById(R.id.newAccountNickname);
		final EditText hostname = (EditText) v.findViewById(R.id.newAccountHostname);

		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ContentValues values = new ContentValues();
				values.put(AccountsTableMetaData.FIELD_JID, jid.getText().toString());
				values.put(AccountsTableMetaData.FIELD_PASSWORD, passowrd.getText().toString());
				values.put(AccountsTableMetaData.FIELD_NICKNAME, nickname.getText().toString());
				values.put(AccountsTableMetaData.FIELD_HOSTNAME, hostname.getText().toString());

				inflater.getContext().getApplicationContext().getContentResolver().insert(
						Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), values);
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
