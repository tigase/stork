package org.tigase.mobile.db.providers;

import java.util.HashMap;
import java.util.Map;

import org.tigase.mobile.db.AccountsTableMetaData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class AccountsProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.AccountsProvider";

	private final static int ACCOUNT_INDICATOR = 2;

	public static final String ACCOUNT_KEY = "content://" + AUTHORITY + "/account";

	private final static int ACCOUNTS_LIST_INDICATOR = 1;

	public static final String ACCOUNTS_LIST_KEY = "content://" + AUTHORITY + "/accounts";

	private final static Map<String, String> accountsProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;
		{
			put(AccountsTableMetaData.FIELD_ID, AccountsTableMetaData.TABLE_NAME + "." + AccountsTableMetaData.FIELD_ID);
			put(AccountsTableMetaData.FIELD_ROSTER_VERSION, AccountsTableMetaData.TABLE_NAME + "."
					+ AccountsTableMetaData.FIELD_ROSTER_VERSION);
			put(AccountsTableMetaData.FIELD_JID, AccountsTableMetaData.TABLE_NAME + "." + AccountsTableMetaData.FIELD_JID);
			put(AccountsTableMetaData.FIELD_PASSWORD, AccountsTableMetaData.TABLE_NAME + "."
					+ AccountsTableMetaData.FIELD_PASSWORD);
			put(AccountsTableMetaData.FIELD_NICKNAME, AccountsTableMetaData.TABLE_NAME + "."
					+ AccountsTableMetaData.FIELD_NICKNAME);
			put(AccountsTableMetaData.FIELD_HOSTNAME, AccountsTableMetaData.TABLE_NAME + "."
					+ AccountsTableMetaData.FIELD_HOSTNAME);
			put(AccountsTableMetaData.FIELD_PORT, AccountsTableMetaData.TABLE_NAME + "." + AccountsTableMetaData.FIELD_PORT);
			put(AccountsTableMetaData.FIELD_ACTIVE, AccountsTableMetaData.TABLE_NAME + "." + AccountsTableMetaData.FIELD_ACTIVE);
		}
	};

	private MessengerDatabaseHelper dbHelper;

	private final UriMatcher uriMatcher;

	public AccountsProvider() {
		this.uriMatcher = new UriMatcher(0);
		this.uriMatcher.addURI(AUTHORITY, "accounts", ACCOUNTS_LIST_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "account/#", ACCOUNT_INDICATOR);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ACCOUNTS_LIST_INDICATOR:
			return AccountsTableMetaData.ACCOUNTS_LIST_TYPE;
		case ACCOUNT_INDICATOR:
			return AccountsTableMetaData.ACCOUNT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch (uriMatcher.match(uri)) {
		case ACCOUNTS_LIST_INDICATOR:
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			long rowId = db.insert(AccountsTableMetaData.TABLE_NAME, null, values);
			if (rowId > 0) {
				Uri insertedItem = ContentUris.withAppendedId(Uri.parse(ACCOUNT_KEY), rowId);
				getContext().getContentResolver().notifyChange(insertedItem, null);
				return insertedItem;
			}
			throw new RuntimeException("Account is not added");
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (uriMatcher.match(uri)) {
		case ACCOUNTS_LIST_INDICATOR:
			qb.setTables(AccountsTableMetaData.TABLE_NAME);
			qb.setProjectionMap(accountsProjectionMap);
			break;
		case ACCOUNT_INDICATOR:
			qb.setTables(AccountsTableMetaData.TABLE_NAME);
			qb.setProjectionMap(accountsProjectionMap);
			qb.appendWhere(AccountsTableMetaData.FIELD_ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		if (TextUtils.isEmpty(sortOrder)) {
			sortOrder = AccountsTableMetaData.FIELD_JID + " ASC";
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
