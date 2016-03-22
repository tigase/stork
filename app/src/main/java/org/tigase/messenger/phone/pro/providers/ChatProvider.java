/*
 * ChatProvider.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.providers;

import java.util.HashMap;
import java.util.Map;

import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class ChatProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.messenger.phone.pro.Chat";
	public static final String SCHEME = "content://";
	public static final Uri OPEN_CHATS_URI = Uri.parse(SCHEME + AUTHORITY + "/openchat");
	public static final Uri CHAT_HISTORY_URI = Uri.parse(SCHEME + AUTHORITY + "/chat");
	public static final Uri UNSENT_MESSAGES_URI = Uri.parse(SCHEME + AUTHORITY + "/unsent");
	public static final String FIELD_NAME = "name";
	public static final String FIELD_UNREAD_COUNT = "unread";
	public static final String FIELD_STATE = "state";
	public static final String FIELD_LAST_MESSAGE = "last_message";
	private static final UriMatcher sUriMatcher = new UriMatcher(1);
	private final static int URI_INDICATOR_OPENCHATS = 2;
	private final static int URI_INDICATOR_OPENCHAT_BY_ID = 3;
	private final static int URI_INDICATOR_OPENCHAT_BY_JID = 4;
	private final static Map<String, String> openChatsProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;

		{
			put(DatabaseContract.OpenChats.FIELD_ACCOUNT, "open_chats." + DatabaseContract.OpenChats.FIELD_ACCOUNT + " as "
					+ DatabaseContract.OpenChats.FIELD_ACCOUNT);
			put(DatabaseContract.OpenChats.FIELD_ID,
					"open_chats." + DatabaseContract.OpenChats.FIELD_ID + " as " + DatabaseContract.OpenChats.FIELD_ID);
			put(DatabaseContract.OpenChats.FIELD_JID,
					"open_chats." + DatabaseContract.OpenChats.FIELD_JID + " as " + DatabaseContract.OpenChats.FIELD_JID);
			put(ChatProvider.FIELD_NAME,
					"CASE WHEN recipient." + RosterItemsCacheTableMetaData.FIELD_NAME + " IS NULL THEN " + " open_chats."
							+ DatabaseContract.OpenChats.FIELD_JID + " ELSE recipient."
							+ RosterItemsCacheTableMetaData.FIELD_NAME + " END as " + ChatProvider.FIELD_NAME);
			put(ChatProvider.FIELD_UNREAD_COUNT, "(SELECT COUNT(" + DatabaseContract.ChatHistory.TABLE_NAME + "."
					+ DatabaseContract.ChatHistory.FIELD_ID + ") from " + DatabaseContract.ChatHistory.TABLE_NAME + " WHERE "
					+ DatabaseContract.ChatHistory.FIELD_ACCOUNT + " = open_chats." + DatabaseContract.OpenChats.FIELD_ACCOUNT
					+ " AND " + DatabaseContract.ChatHistory.FIELD_JID + " = open_chats." + DatabaseContract.OpenChats.FIELD_JID
					+ " AND " + DatabaseContract.ChatHistory.FIELD_STATE + " = "
					+ DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD + ") as " + ChatProvider.FIELD_UNREAD_COUNT);
			put(DatabaseContract.OpenChats.FIELD_TYPE,
					"open_chats." + DatabaseContract.OpenChats.FIELD_TYPE + " as " + DatabaseContract.OpenChats.FIELD_TYPE);
			put(ChatProvider.FIELD_STATE,
					"CASE WHEN open_chats." + DatabaseContract.OpenChats.FIELD_TYPE + " = "
							+ DatabaseContract.OpenChats.TYPE_MUC + " THEN " + " open_chats."
							+ DatabaseContract.OpenChats.FIELD_ROOM_STATE + " ELSE recipient."
							+ DatabaseContract.RosterItemsCache.FIELD_STATUS + " END as " + ChatProvider.FIELD_STATE);
			put(ChatProvider.FIELD_LAST_MESSAGE,
					"(SELECT " + DatabaseContract.ChatHistory.FIELD_BODY + " FROM " + DatabaseContract.ChatHistory.TABLE_NAME
							+ " WHERE " + DatabaseContract.ChatHistory.FIELD_ACCOUNT + " = open_chats."
							+ DatabaseContract.OpenChats.FIELD_ACCOUNT + " AND " + DatabaseContract.ChatHistory.FIELD_JID
							+ " = open_chats." + DatabaseContract.OpenChats.FIELD_JID + " ORDER BY "
							+ DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC LIMIT 1) as "
							+ ChatProvider.FIELD_LAST_MESSAGE);
			put(DatabaseContract.OpenChats.FIELD_THREAD_ID, "open_chats." + DatabaseContract.OpenChats.FIELD_THREAD_ID + " as "
					+ DatabaseContract.OpenChats.FIELD_THREAD_ID);
			put(DatabaseContract.OpenChats.FIELD_NICKNAME, "open_chats." + DatabaseContract.OpenChats.FIELD_NICKNAME + " as "
					+ DatabaseContract.OpenChats.FIELD_NICKNAME);
		}
	};

	/**
	 * Chat message by /account/destinationBareJID/id
	 */
	private static final int URI_INDICATOR_CHAT_ITEM = 6;
	private static final int URI_INDICATOR_UNSENT = 7;

	/**
	 * List of chat messages by /account/destinationBareJID
	 */
	private static final int URI_INDICATOR_CHATS_ACCOUNT = 8;

	static {
		sUriMatcher.addURI(AUTHORITY, "unsent/*", URI_INDICATOR_UNSENT);
		sUriMatcher.addURI(AUTHORITY, "openchat", URI_INDICATOR_OPENCHATS);
		sUriMatcher.addURI(AUTHORITY, "openchat/#", URI_INDICATOR_OPENCHAT_BY_ID);
		sUriMatcher.addURI(AUTHORITY, "openchat/*", URI_INDICATOR_OPENCHAT_BY_JID);
		sUriMatcher.addURI(AUTHORITY, "chat/*/*/#", URI_INDICATOR_CHAT_ITEM);
		sUriMatcher.addURI(AUTHORITY, "chat/*/*", URI_INDICATOR_CHATS_ACCOUNT);
	}

	private DatabaseHelper dbHelper;

	public ChatProvider() {
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Implement this to handle requests to delete one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case URI_INDICATOR_OPENCHATS:
			return DatabaseContract.OpenChats.OPENCHATS_TYPE;
		case URI_INDICATOR_OPENCHAT_BY_ID:
		case URI_INDICATOR_OPENCHAT_BY_JID:
			return DatabaseContract.OpenChats.OPENCHAT_ITEM_TYPE;
		case URI_INDICATOR_CHATS_ACCOUNT:
		case URI_INDICATOR_UNSENT:
			return DatabaseContract.ChatHistory.CHATS_TYPE;
		case URI_INDICATOR_CHAT_ITEM:
			return DatabaseContract.ChatHistory.CHATS_ITEM_TYPE;
		default:
			throw new UnsupportedOperationException("Not yet implemented");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch (sUriMatcher.match(uri)) {
		case URI_INDICATOR_CHATS_ACCOUNT:
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			long rowId = db.insert(DatabaseContract.ChatHistory.TABLE_NAME, DatabaseContract.ChatHistory.FIELD_JID, values);
			if (rowId > 0) {
				Uri insertedItem = ContentUris.withAppendedId(uri, rowId);
				getContext().getContentResolver().notifyChange(insertedItem, null);
				return insertedItem;
			} else {
				throw new RuntimeException("Cannot insert message!");
			}
		default:
			throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}

	@Override
	public boolean onCreate() {
		this.dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor;
		switch (sUriMatcher.match(uri)) {
		case URI_INDICATOR_OPENCHATS:
			cursor = queryOpenChats(projection, selection, selectionArgs, sortOrder);
			break;
		case URI_INDICATOR_OPENCHAT_BY_ID:
			cursor = queryOpenChats(projection, DatabaseContract.OpenChats.FIELD_ID + "=?",
					new String[] { uri.getLastPathSegment() }, sortOrder);
			break;
		case URI_INDICATOR_OPENCHAT_BY_JID:
			cursor = queryOpenChats(projection, DatabaseContract.OpenChats.FIELD_JID + "=?",
					new String[] { uri.getLastPathSegment() }, sortOrder);
			break;
		case URI_INDICATOR_UNSENT:
			cursor = dbHelper.getReadableDatabase().query(DatabaseContract.ChatHistory.TABLE_NAME, projection,
					DatabaseContract.ChatHistory.FIELD_ACCOUNT + "=? AND " + DatabaseContract.ChatHistory.FIELD_STATE + "=?",
					new String[] { uri.getLastPathSegment(), "" + DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT }, null, null,
					sortOrder);
			break;
		case URI_INDICATOR_CHAT_ITEM:
			String jid = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
			cursor = dbHelper.getReadableDatabase().query(DatabaseContract.ChatHistory.TABLE_NAME, projection,
					// DatabaseContract.ChatHistory.FIELD_JID + "=? AND " +
					// DatabaseContract.ChatHistory.FIELD_ID + "=?", new
					// String[]{jid, uri.getLastPathSegment()},
					DatabaseContract.ChatHistory.FIELD_ID + "=?", new String[] { uri.getLastPathSegment() }, null, null,
					sortOrder);
			break;
		case URI_INDICATOR_CHATS_ACCOUNT:
			String account = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
			jid = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
			cursor = dbHelper.getReadableDatabase().query(DatabaseContract.ChatHistory.TABLE_NAME, projection,
					// DatabaseContract.ChatHistory.FIELD_JID + "=? AND " +
					// DatabaseContract.ChatHistory.FIELD_ID + "=?", new
					// String[]{jid, uri.getLastPathSegment()},
					DatabaseContract.ChatHistory.FIELD_JID + "=? AND " + DatabaseContract.ChatHistory.FIELD_ACCOUNT + "=?",
					new String[] { jid, account }, null, null, sortOrder);
			break;
		default:
			throw new UnsupportedOperationException("Unrecognized URI " + uri);
		}

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	private Cursor queryOpenChats(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setProjectionMap(openChatsProjectionMap);
		qb.setTables(DatabaseContract.OpenChats.TABLE_NAME + " open_chats LEFT JOIN "
				+ DatabaseContract.RosterItemsCache.TABLE_NAME + " recipient ON recipient."
				+ DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = open_chats." + DatabaseContract.OpenChats.FIELD_ACCOUNT
				+ " AND recipient." + DatabaseContract.RosterItemsCache.FIELD_JID + " = open_chats."
				+ DatabaseContract.OpenChats.FIELD_JID);

		// may be removed later on production build - left to make tests easier
		qb.appendWhere("open_chats." + DatabaseContract.OpenChats.FIELD_TYPE + " IS NOT NULL");

		return qb.query(dbHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		switch (sUriMatcher.match(uri)) {
		case URI_INDICATOR_CHAT_ITEM:
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			// long rowId = db.update(DatabaseContract.ChatHistory.TABLE_NAME,
			// DatabaseContract.ChatHistory.FIELD_JID, values);

			int updated = db.update(DatabaseContract.ChatHistory.TABLE_NAME, values,
					DatabaseContract.ChatHistory.FIELD_ID + "=?", new String[] { uri.getLastPathSegment() });
			if (updated > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
				return updated;
			} else {
				throw new RuntimeException("Cannot update message!");
			}
		default:
			throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}
}
