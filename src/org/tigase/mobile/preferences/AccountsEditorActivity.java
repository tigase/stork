package org.tigase.mobile.preferences;

import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.providers.AccountsProvider;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class AccountsEditorActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.accounts_list);

		Cursor c = getContentResolver().query(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), null, null, null, null);
		startManagingCursor(c);

		String[] cols = new String[] { AccountsTableMetaData.FIELD_JID };
		int[] names = new int[] { R.id.account_name_entry };

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.accounts_list_entry, c, cols, names,
				android.support.v4.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		((ListView) findViewById(R.id.accountsListView)).setAdapter(adapter);

		((Button) findViewById(R.id.accountAdd)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = AccountEditDialog.newInstance();

				newFragment.show(getSupportFragmentManager(), "dialog");

				// TODO Auto-generated method stub

			}
		});

	}

}
