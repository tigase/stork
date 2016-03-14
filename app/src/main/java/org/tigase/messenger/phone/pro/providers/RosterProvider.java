/*
 * RosterProvider.java
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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;

public class RosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.messenger.phone.pro.Roster";
	public static final String SCHEME = "content://";
	public static final Uri ROSTER_URI = Uri.parse(SCHEME + AUTHORITY + "/roster");
	public static final Uri VCARD_URI = Uri.parse(SCHEME + AUTHORITY + "/vcard");

	private static final UriMatcher sUriMatcher = new UriMatcher(1);

	private final static int URI_INDICATOR_ROSTER = 2;
	private final static int URI_INDICATOR_ROSTERITEM_ID = 3;
	private final static int URI_INDICATOR_ROSTERITEM_JID = 4;
	private final static int URI_INDICATOR_VCARDITEM_ID = 5;
	private final static int URI_INDICATOR_VCARDITEM_JID = 6;

	static {
		sUriMatcher.addURI(AUTHORITY, "roster", URI_INDICATOR_ROSTER);
		sUriMatcher.addURI(AUTHORITY, "roster/#", URI_INDICATOR_ROSTERITEM_ID);
		sUriMatcher.addURI(AUTHORITY, "roster/*", URI_INDICATOR_ROSTERITEM_JID);
		sUriMatcher.addURI(AUTHORITY, "vcard/#", URI_INDICATOR_VCARDITEM_ID);
		sUriMatcher.addURI(AUTHORITY, "vcard/*", URI_INDICATOR_VCARDITEM_JID);
	}

	private DatabaseHelper dbHelper;

	public RosterProvider() {
	}

	private static String nn(String n) {
		return n == null ? "" : n;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Implement this to handle requests to delete one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case URI_INDICATOR_ROSTER:
				return DatabaseContract.RosterItemsCache.ROSTER_TYPE;
			case URI_INDICATOR_ROSTERITEM_ID:
			case URI_INDICATOR_ROSTERITEM_JID:
				return DatabaseContract.RosterItemsCache.ROSTER_ITEM_TYPE;
			case URI_INDICATOR_VCARDITEM_ID:
			case URI_INDICATOR_VCARDITEM_JID:
				return DatabaseContract.VCardsCache.VCARD_ITEM_TYPE;

			default:
				throw new UnsupportedOperationException("Not yet implemented");
		}

	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO: Implement this to handle requests to insert a new row.
		throw new UnsupportedOperationException("Not yet implemented");
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
			case URI_INDICATOR_ROSTER:
				cursor = dbHelper.getReadableDatabase().query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection, selection,
						selectionArgs, null, null, sortOrder);
				break;
			case URI_INDICATOR_ROSTERITEM_ID:
				cursor = dbHelper.getReadableDatabase().query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection,
						DatabaseContract.RosterItemsCache.FIELD_ID + "=?", new String[]{uri.getLastPathSegment()}, null, null,
						sortOrder);
				break;
			case URI_INDICATOR_ROSTERITEM_JID:
				cursor = dbHelper.getReadableDatabase().query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection,
						DatabaseContract.RosterItemsCache.FIELD_JID + "=?", new String[]{uri.getLastPathSegment()}, null, null,
						sortOrder);
				break;

			case URI_INDICATOR_VCARDITEM_ID:
				cursor = dbHelper.getReadableDatabase().query(DatabaseContract.VCardsCache.TABLE_NAME, projection,
						DatabaseContract.VCardsCache.FIELD_ID + "=?", new String[]{uri.getLastPathSegment()}, null, null,
						sortOrder);
				break;
			case URI_INDICATOR_VCARDITEM_JID:
				cursor = dbHelper.getReadableDatabase().query(DatabaseContract.VCardsCache.TABLE_NAME, projection,
						DatabaseContract.VCardsCache.FIELD_JID + "=?", new String[]{uri.getLastPathSegment()}, null, null,
						sortOrder);
				break;
			default:
				throw new UnsupportedOperationException("Unrecognized URI " + uri);
		}

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		Log.i("RosterProvider", "Setting notificationUri=" + cursor.getNotificationUri());

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
