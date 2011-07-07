package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;
import org.tigase.mobile.db.RosterTableMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class RosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.RosterProvider";

	public static final String CONTENT_URI = "content://" + AUTHORITY + "/roster";

	private static final int ROSTER_ITEM_URI_INDICATOR = 2;

	private static final int ROSTER_URI_INDICATOR = 1;

	private MessengerDatabaseHelper dbHelper;

	private final UriMatcher uriMatcher;

	public RosterProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "roster", ROSTER_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "roster/*", ROSTER_ITEM_URI_INDICATOR);

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_TYPE;
		case ROSTER_ITEM_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
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
		case ROSTER_URI_INDICATOR:
			qb.setTables(RosterTableMetaData.TABLE_NAME);
			qb.setProjectionMap(dbHelper.getRosterProjectionMap());
			break;
		case ROSTER_ITEM_URI_INDICATOR:
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, RosterTableMetaData.FIELD_JID + " ASC");
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
