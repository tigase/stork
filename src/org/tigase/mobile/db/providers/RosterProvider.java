package org.tigase.mobile.db.providers;

import org.tigase.mobile.db.MessengerDatabaseHelper;
import org.tigase.mobile.db.RosterTableMetaData;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class RosterProvider extends AbstractRosterProvider {

	private MessengerDatabaseHelper dbHelper;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		dbHelper.open();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			qb.setTables(RosterTableMetaData.TABLE_NAME);
			qb.setProjectionMap(dbHelper.getRosterProjectionMap());
			break;
		case ROSTER_ITEM_URI_INDICATOR:
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = dbHelper.getDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, RosterTableMetaData.FIELD_PRESENCE
				+ " DESC, " + RosterTableMetaData.FIELD_DISPLAY_NAME + " ASC");

		int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
