/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.roster.view;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.multiselect.BaseCursorAdapter;

public class RefreshRosterTask
		extends AsyncTask<String, Void, Cursor> {

	private final BaseCursorAdapter<?> adapter;
	private final ContentResolver contentResolver;
	private final String enabledAccounts;
	private final SharedPreferences sharedPref;

	public RefreshRosterTask(BaseCursorAdapter<?> adapter, SharedPreferences sharedPref, String enabledAccounts,
							 ContentResolver contentResolver) {
		this.sharedPref = sharedPref;
		this.enabledAccounts = enabledAccounts;
		this.contentResolver = contentResolver;
		this.adapter = adapter;
	}

	protected void onPostExecute(Cursor cursor) {
		adapter.swapCursor(cursor);
	}

	@Override
	protected Cursor doInBackground(String... params) {
		if (sharedPref == null) {
			Log.e("RosterItemFragment", "Shared preferences are empty?");
			return null;
		}
		String[] columnsToReturn = new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
												DatabaseContract.RosterItemsCache.FIELD_ACCOUNT,
												DatabaseContract.RosterItemsCache.FIELD_JID,
												DatabaseContract.RosterItemsCache.FIELD_NAME,
												DatabaseContract.RosterItemsCache.FIELD_STATUS};

		boolean showOffline = sharedPref.getBoolean("show_offline", RosterFragment.SHOW_OFFLINE_DEFAULT);
		final String searchText = params != null && params.length > 0 ? params[0] : null;

		String selection = "1=1 ";
		String[] args = null;

		if (!showOffline) {
			selection += " AND " + DatabaseContract.RosterItemsCache.FIELD_STATUS + ">=5 ";
		}

		if (searchText != null) {
			selection += " AND (" + DatabaseContract.RosterItemsCache.FIELD_NAME + " like ? OR " +
					DatabaseContract.RosterItemsCache.FIELD_JID + " like ?" + " )";
			args = new String[]{"%" + searchText + "%", "%" + searchText + "%"};
		}

		if (enabledAccounts != null) {
			selection += " AND (" + DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " IN (" + enabledAccounts + "))";
		}

		String sort;
		String s = sharedPref.getString("roster_sort", "presence");
		switch (s) {
			case "name":
				sort = DatabaseContract.RosterItemsCache.FIELD_NAME + " COLLATE NOCASE ASC";
				break;
			case "jid":
				sort = DatabaseContract.RosterItemsCache.FIELD_JID + " ASC";
				break;
			case "presence":
				sort = DatabaseContract.RosterItemsCache.FIELD_STATUS + " DESC," +
						DatabaseContract.RosterItemsCache.FIELD_NAME + " COLLATE NOCASE ASC";
				break;
			default:
				sort = "";
		}

		return contentResolver.query(RosterProvider.ROSTER_URI, columnsToReturn, selection, args, sort);
	}
}
