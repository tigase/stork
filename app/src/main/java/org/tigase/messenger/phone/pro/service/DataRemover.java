package org.tigase.messenger.phone.pro.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;

import java.util.ArrayList;

public class DataRemover
		extends BroadcastReceiver {

	private final DatabaseHelper dbHelper;

	DataRemover(DatabaseHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	private void doDirtyJob(SQLiteDatabase db, String tableName, String fieldName, ArrayList<String> accounts) {
		Log.d("DataRemover", "Remove data from " + tableName + ". Given list: " + accounts);

		if (accounts.size() == 0) {
			db.execSQL("DELETE FROM " + tableName + ";");
		} else {
			String args = TextUtils.join(", ", accounts);
			db.execSQL(String.format("DELETE FROM " + tableName + " WHERE " + fieldName + " NOT IN (%s);", args));
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("DataRemover", "Removing data after account remove");
		removeUnusedData(context);
	}

	void removeUnusedData(Context context) {
		try {
			final AccountManager am = AccountManager.get(context);

			ArrayList<String> accounts = new ArrayList<>();
			for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
				accounts.add("'" + account.name + "'");
			}

			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.beginTransaction();
			try {
				doDirtyJob(db, DatabaseContract.RosterItemsCache.TABLE_NAME,
						   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT, accounts);
				doDirtyJob(db, DatabaseContract.ChatHistory.TABLE_NAME, DatabaseContract.ChatHistory.FIELD_ACCOUNT,
						   accounts);
				doDirtyJob(db, DatabaseContract.OpenChats.TABLE_NAME, DatabaseContract.OpenChats.FIELD_ACCOUNT,
						   accounts);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		} finally {
			context.getContentResolver().notifyChange(RosterProvider.ROSTER_URI, null);
			context.getContentResolver().notifyChange(ChatProvider.OPEN_CHATS_URI, null);
		}
	}
}
