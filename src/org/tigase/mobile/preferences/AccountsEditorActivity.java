package org.tigase.mobile.preferences;

import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.providers.AccountsProvider;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class AccountsEditorActivity extends FragmentActivity {

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		MenuInflater m = new MenuInflater(this);
		m.inflate(R.menu.accounts_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.accountEdit: {
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			DialogFragment newFragment = AccountEditDialog.newInstance(info.id);
			newFragment.show(getSupportFragmentManager(), "dialog");
			return true;
		}
		case R.id.accountRemove: {
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to remove this account?").setCancelable(false).setPositiveButton(
					android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							getContentResolver().delete(
									ContentUris.withAppendedId(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), info.id), null,
									null);
							dialog.cancel();
						}
					}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
		default:
			return super.onContextItemSelected(item);
		}

	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.accounts_list);

		Cursor c = managedQuery(Uri.parse(AccountsProvider.ACCOUNTS_LIST_KEY), null, null, null, null);
		startManagingCursor(c);

		String[] cols = new String[] { AccountsTableMetaData.FIELD_JID };
		int[] names = new int[] { R.id.account_name_entry };

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.accounts_list_entry, c, cols, names);
		ListView listView = ((ListView) findViewById(android.R.id.list));
		registerForContextMenu(listView);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				DialogFragment newFragment = AccountEditDialog.newInstance(id);
				newFragment.show(getSupportFragmentManager(), "dialog");
			}
		});

		((Button) findViewById(R.id.accountAdd)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogFragment newFragment = AccountEditDialog.newInstance();
				newFragment.show(getSupportFragmentManager(), "dialog");

			}
		});

	}

}
