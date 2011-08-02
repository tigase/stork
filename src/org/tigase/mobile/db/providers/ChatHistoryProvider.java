package org.tigase.mobile.db.providers;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.MessengerDatabaseHelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ChatHistoryProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.ChatHistoryProvider";

	public static final String CHAT_URI = "content://" + AUTHORITY + "/chat";

	public static final String CHAT_ITEM_URI = "content://" + AUTHORITY + "/chatItem";

	protected final UriMatcher uriMatcher;

	private MessengerDatabaseHelper dbHelper;

	protected static final int CHAT_ITEM_URI_INDICATOR = 2;

	protected static final int CHAT_URI_INDICATOR = 1;

	public ChatHistoryProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "chat/*", CHAT_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "chatItem/*", CHAT_ITEM_URI_INDICATOR);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case CHAT_URI_INDICATOR:
			return ChatTableMetaData.CONTENT_TYPE;
		case CHAT_ITEM_URI_INDICATOR:
			return ChatTableMetaData.CONTENT_ITEM_TYPE;
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
		dbHelper.open();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (uriMatcher.match(uri)) {
		case CHAT_URI_INDICATOR:
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(dbHelper.getChatHistoryProjectionMap());
			String jid = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.FIELD_JID + "='" + jid + "'");
			break;
		case CHAT_ITEM_URI_INDICATOR:
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(dbHelper.getChatHistoryProjectionMap());
			String id = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.FIELD_ID + "=" + id + "");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = dbHelper.getDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, ChatTableMetaData.FIELD_TIMESTAMP + " ASC, "
				+ ChatTableMetaData.FIELD_ID + " ASC");

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
