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
package org.tigase.messenger.jaxmpp.android.roster;

import android.database.sqlite.SQLiteDatabase;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class RosterDbHelper {

	private static final String CREATE_ITEMS_TABLE =
			"CREATE TABLE " + DatabaseContract.RosterItemsCache.TABLE_NAME + " (" +
					DatabaseContract.RosterItemsCache.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
					DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " TEXT, " +
					DatabaseContract.RosterItemsCache.FIELD_JID + " TEXT, " +
					DatabaseContract.RosterItemsCache.FIELD_NAME + " TEXT, " +
					DatabaseContract.RosterItemsCache.FIELD_ASK + " BOOLEAN, " +
					DatabaseContract.RosterItemsCache.FIELD_SUBSCRIPTION + " TEXT, " +
					DatabaseContract.RosterItemsCache.FIELD_TIMESTAMP + " DATETIME" + ");";

	private static final String CREATE_GROUPS_TABLE =
			"CREATE TABLE " + DatabaseContract.RosterGroupsCache.TABLE_NAME + " (" +
					DatabaseContract.RosterGroupsCache.FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					DatabaseContract.RosterGroupsCache.FIELD_NAME + " TEXT NOT NULL" + ");";

	private static final String CREATE_ITEMS_GROUPS_TABLE =
			"CREATE TABLE " + DatabaseContract.RosterItemsGroups.TABLE_NAME + " (" +
					DatabaseContract.RosterItemsGroups.FIELD_ITEM + " INTEGER, " +
					DatabaseContract.RosterItemsGroups.FIELD_GROUP + " INTEGER, " + "FOREIGN KEY(" +
					DatabaseContract.RosterItemsGroups.FIELD_ITEM + ") REFERENCES " +
					DatabaseContract.RosterItemsCache.TABLE_NAME + "(" + DatabaseContract.RosterItemsCache.FIELD_ID +
					")," + "FOREIGN KEY(" + DatabaseContract.RosterItemsGroups.FIELD_GROUP + ") REFERENCES " +
					DatabaseContract.RosterGroupsCache.TABLE_NAME + "(" + DatabaseContract.RosterGroupsCache.FIELD_ID +
					")" + ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_ITEMS_TABLE);
		database.execSQL(CREATE_GROUPS_TABLE);
		database.execSQL(CREATE_ITEMS_GROUPS_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
