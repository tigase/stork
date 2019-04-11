/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;

public class RosterProvider
		extends ContentProvider {

	private static final String AUTHORITY = "org.tigase.messenger.phone.pro.Roster";
	private static final String SCHEME = "content://";
	public static final Uri ROSTER_URI = Uri.parse(SCHEME + AUTHORITY + "/roster");
	public static final Uri VCARD_URI = Uri.parse(SCHEME + AUTHORITY + "/vcard");

	private static final UriMatcher sUriMatcher = new UriMatcher(1);

	private final static int URI_INDICATOR_ROSTER = 2;
	private final static int URI_INDICATOR_ROSTERITEM_ID = 3;
	private final static int URI_INDICATOR_ROSTERITEM_JID = 4;
	private final static int URI_INDICATOR_VCARDITEM_ID = 5;
	private final static int URI_INDICATOR_VCARDITEM_JID = 6;
	private final static int URI_INDICATOR_ROSTERITEM_ACCOUNT_JID = 7;

	static {
		sUriMatcher.addURI(AUTHORITY, "roster", URI_INDICATOR_ROSTER);
		sUriMatcher.addURI(AUTHORITY, "roster/#", URI_INDICATOR_ROSTERITEM_ID);
		sUriMatcher.addURI(AUTHORITY, "roster/*/*", URI_INDICATOR_ROSTERITEM_ACCOUNT_JID);
		sUriMatcher.addURI(AUTHORITY, "roster/*", URI_INDICATOR_ROSTERITEM_JID);
		sUriMatcher.addURI(AUTHORITY, "vcard/#", URI_INDICATOR_VCARDITEM_ID);
		sUriMatcher.addURI(AUTHORITY, "vcard/*", URI_INDICATOR_VCARDITEM_JID);
	}

	private DatabaseHelper dbHelper;

	public RosterProvider() {
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		// Implement this to handle requests to delete one or more rows.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(@NonNull Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case URI_INDICATOR_ROSTER:
				return DatabaseContract.RosterItemsCache.ROSTER_TYPE;
			case URI_INDICATOR_ROSTERITEM_ID:
			case URI_INDICATOR_ROSTERITEM_JID:
			case URI_INDICATOR_ROSTERITEM_ACCOUNT_JID:
				return DatabaseContract.RosterItemsCache.ROSTER_ITEM_TYPE;
			case URI_INDICATOR_VCARDITEM_ID:
			case URI_INDICATOR_VCARDITEM_JID:
				return DatabaseContract.VCardsCache.VCARD_ITEM_TYPE;

			default:
				throw new UnsupportedOperationException("Not yet implemented");
		}

	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public boolean onCreate() {
		this.dbHelper = DatabaseHelper.getInstance(getContext());
		return true;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {

		Cursor cursor;
		switch (sUriMatcher.match(uri)) {
			case URI_INDICATOR_ROSTER:
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection, selection, selectionArgs, null,
							   null, sortOrder);
				break;
			case URI_INDICATOR_ROSTERITEM_ID:
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection,
							   DatabaseContract.RosterItemsCache.FIELD_ID + "=?",
							   new String[]{uri.getLastPathSegment()}, null, null, sortOrder);
				break;
			case URI_INDICATOR_ROSTERITEM_JID:
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection,
							   DatabaseContract.RosterItemsCache.FIELD_JID + "=?",
							   new String[]{uri.getLastPathSegment()}, null, null, sortOrder);
				break;
			case URI_INDICATOR_ROSTERITEM_ACCOUNT_JID:
				String account = uri.getPathSegments().get(uri.getPathSegments().size() - 2);
				String jid = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.RosterItemsCache.TABLE_NAME, projection,
							   DatabaseContract.RosterItemsCache.FIELD_JID + "=? AND " +
									   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + "=?",
							   new String[]{jid, account}, null, null, sortOrder);
				break;

			case URI_INDICATOR_VCARDITEM_ID:
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.VCardsCache.TABLE_NAME, projection,
							   DatabaseContract.VCardsCache.FIELD_ID + "=?", new String[]{uri.getLastPathSegment()},
							   null, null, sortOrder);
				break;
			case URI_INDICATOR_VCARDITEM_JID:
				cursor = dbHelper.getReadableDatabase()
						.query(DatabaseContract.VCardsCache.TABLE_NAME, projection,
							   DatabaseContract.VCardsCache.FIELD_JID + "=?", new String[]{uri.getLastPathSegment()},
							   null, null, sortOrder);
				break;
			default:
				throw new UnsupportedOperationException("Unrecognized URI " + uri);
		}

		Context context = getContext();
		if (context != null) {
			cursor.setNotificationUri(context.getContentResolver(), uri);
		}
		Log.i("RosterProvider", "Setting notificationUri=" + cursor.getNotificationUri());

		return cursor;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
