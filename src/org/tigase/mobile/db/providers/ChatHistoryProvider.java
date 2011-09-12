package org.tigase.mobile.db.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tigase.mobile.db.ChatTableMetaData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ChatHistoryProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.ChatHistoryProvider";

	protected static final int CHAT_ITEM_URI_INDICATOR = 2;

	public static final String CHAT_URI = "content://" + AUTHORITY + "/chat";

	protected static final int CHAT_URI_INDICATOR = 1;

	private final static Map<String, String> chatHistoryProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;
		{
			put(ChatTableMetaData.FIELD_BODY, ChatTableMetaData.FIELD_BODY);
			put(ChatTableMetaData.FIELD_ID, ChatTableMetaData.FIELD_ID);
			put(ChatTableMetaData.FIELD_JID, ChatTableMetaData.FIELD_JID);
			put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_STATE);
			put(ChatTableMetaData.FIELD_THREAD_ID, ChatTableMetaData.FIELD_THREAD_ID);
			put(ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_TIMESTAMP);
			put(ChatTableMetaData.FIELD_TYPE, ChatTableMetaData.FIELD_TYPE);
		}
	};

	public static final String UNSENT_MESSAGES_URI = "content://" + AUTHORITY + "/unsent";

	protected static final int UNSENT_MESSAGES_URI_INDICATOR = 3;

	private MessengerDatabaseHelper dbHelper;

	public ChatHistoryProvider() {
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (match(uri)) {
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
		switch (match(uri)) {
		case CHAT_URI_INDICATOR:
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			long rowId = db.insert(ChatTableMetaData.TABLE_NAME, ChatTableMetaData.FIELD_JID, values);
			if (rowId > 0) {
				Uri insertedItem = ContentUris.withAppendedId(
						Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + values.getAsString(ChatTableMetaData.FIELD_JID)), rowId);
				getContext().getContentResolver().notifyChange(insertedItem, null);
				return insertedItem;
			}
			break;
		case CHAT_ITEM_URI_INDICATOR:
			throw new IllegalArgumentException("Inserting not supported for URI " + uri);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		return null;
	}

	private int match(Uri uri) {
		// /chat/${JID}
		// /chat/${JID}/#

		List<String> l = uri.getPathSegments();

		if (l.get(0).equals("unsent"))
			return UNSENT_MESSAGES_URI_INDICATOR;

		if (!l.get(0).equals("chat"))
			return 0;

		else if (l.size() == 2)
			return CHAT_URI_INDICATOR;
		else if (l.size() == 3)
			return CHAT_ITEM_URI_INDICATOR;
		else
			return 0;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (match(uri)) {
		case UNSENT_MESSAGES_URI_INDICATOR:
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(chatHistoryProjectionMap);
			qb.appendWhere(ChatTableMetaData.FIELD_STATE + "=" + ChatTableMetaData.STATE_OUT_NOT_SENT);
			break;
		case CHAT_URI_INDICATOR:
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(chatHistoryProjectionMap);
			String jid = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.FIELD_JID + "='" + jid + "'");
			break;
		case CHAT_ITEM_URI_INDICATOR:
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(chatHistoryProjectionMap);
			String id = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.FIELD_ID + "=" + id + "");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI ");
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, ChatTableMetaData.FIELD_TIMESTAMP + " ASC, "
				+ ChatTableMetaData.FIELD_ID + " ASC");

		// int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	private void sentUnsentMessages() {

	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (match(uri) != CHAT_ITEM_URI_INDICATOR)
			throw new IllegalArgumentException("Unknown URI ");

		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		long pk = Long.parseLong(uri.getLastPathSegment());
		int changed = db.update(ChatTableMetaData.TABLE_NAME, values, ChatTableMetaData.FIELD_ID + '=' + pk, null);

		if (changed > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return changed;
	}

}
