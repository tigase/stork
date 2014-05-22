/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.db.providers;

//import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.CapsFeaturesTableMetaData;
import org.tigase.mobile.db.CapsIdentitiesTableMetaData;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.GeolocationTableMetaData;
import org.tigase.mobile.db.OpenChatsTableMetaData;
import org.tigase.mobile.db.OpenMUCTableMetaData;
import org.tigase.mobile.db.RosterCacheTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessengerDatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger.db";

	public static final Integer DATABASE_VERSION = 4;

	private static final String TAG = "tigase";

	public MessengerDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql;

		sql = "CREATE TABLE " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += ChatTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += ChatTableMetaData.FIELD_THREAD_ID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_JID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_AUTHOR_JID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_AUTHOR_NICKNAME + " TEXT, ";
		sql += ChatTableMetaData.FIELD_TIMESTAMP + " DATETIME, ";
		sql += ChatTableMetaData.FIELD_BODY + " TEXT, ";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_RECEIPT_STATUS + " INTEGER DEFAULT 0, ";
		sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += ChatTableMetaData.INDEX_JID;
		sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID;
		sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID;
		sql += ")";
		db.execSQL(sql);

		sql = "CREATE TABLE " + OpenChatsTableMetaData.TABLE_NAME + " (";
		sql += OpenChatsTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += OpenChatsTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += OpenChatsTableMetaData.FIELD_JID + " TEXT, ";
		sql += OpenChatsTableMetaData.FIELD_RESOURCE + " TEXT, ";
		sql += OpenChatsTableMetaData.FIELD_TIMESTAMP + " DATETIME, ";
		sql += OpenChatsTableMetaData.FIELD_THREAD_ID + " TEXT";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + RosterCacheTableMetaData.TABLE_NAME + " (";
		sql += RosterCacheTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += RosterCacheTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += RosterCacheTableMetaData.FIELD_JID + " TEXT, ";
		sql += RosterCacheTableMetaData.FIELD_NAME + " TEXT, ";
		sql += RosterCacheTableMetaData.FIELD_GROUP_NAME + " DATETIME, ";
		sql += RosterCacheTableMetaData.FIELD_ASK + " BOOLEAN, ";
		sql += RosterCacheTableMetaData.FIELD_SUBSCRIPTION + " TEXT, ";
		sql += RosterCacheTableMetaData.FIELD_TIMESTAMP + " DATETIME";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + VCardsCacheTableMetaData.TABLE_NAME + " (";
		sql += VCardsCacheTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += VCardsCacheTableMetaData.FIELD_JID + " TEXT, ";
		sql += VCardsCacheTableMetaData.FIELD_HASH + " TEXT, ";
		sql += VCardsCacheTableMetaData.FIELD_DATA + " BLOB, ";
		sql += VCardsCacheTableMetaData.FIELD_TIMESTAMP + " DATETIME";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += VCardsCacheTableMetaData.INDEX_JID;
		sql += " ON " + VCardsCacheTableMetaData.TABLE_NAME + " (";
		sql += VCardsCacheTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);

		sql = "CREATE TABLE " + CapsIdentitiesTableMetaData.TABLE_NAME + " (";
		sql += CapsIdentitiesTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += CapsIdentitiesTableMetaData.FIELD_NODE + " TEXT, ";
		sql += CapsIdentitiesTableMetaData.FIELD_NAME + " TEXT, ";
		sql += CapsIdentitiesTableMetaData.FIELD_CATEGORY + " TEXT, ";
		sql += CapsIdentitiesTableMetaData.FIELD_TYPE + " TEXT";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + CapsFeaturesTableMetaData.TABLE_NAME + " (";
		sql += CapsFeaturesTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += CapsFeaturesTableMetaData.FIELD_NODE + " TEXT, ";
		sql += CapsFeaturesTableMetaData.FIELD_FEATURE + " TEXT";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + OpenMUCTableMetaData.TABLE_NAME + " (";
		sql += OpenMUCTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += OpenMUCTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += OpenMUCTableMetaData.FIELD_ROOM_JID + " TEXT, ";
		sql += OpenMUCTableMetaData.FIELD_NICKNAME + " TEXT, ";
		sql += OpenMUCTableMetaData.FIELD_PASSWORD + " TEXT, ";
		sql += OpenMUCTableMetaData.FIELD_TIMESTAMP + " DATETIME";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + GeolocationTableMetaData.TABLE_NAME + " (";
		sql += GeolocationTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += GeolocationTableMetaData.FIELD_JID + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_LON + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_LAT + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_ALT + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_COUNTRY + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_LOCALITY + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_STREET + " TEXT ";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += GeolocationTableMetaData.INDEX_JID;
		sql += " ON " + GeolocationTableMetaData.TABLE_NAME + " (";
		sql += GeolocationTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Database upgrade from version " + oldVersion + " to " + newVersion);

		if (oldVersion < 2) {
			String sql = "CREATE TABLE " + OpenMUCTableMetaData.TABLE_NAME + " (";
			sql += OpenMUCTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
			sql += OpenMUCTableMetaData.FIELD_ACCOUNT + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_ROOM_JID + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_NICKNAME + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_PASSWORD + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_TIMESTAMP + " DATETIME";
			sql += ");";
			db.execSQL(sql);

			sql = "CREATE TABLE " + GeolocationTableMetaData.TABLE_NAME + " (";
			sql += GeolocationTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
			sql += GeolocationTableMetaData.FIELD_JID + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_LON + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_LAT + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_ALT + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_COUNTRY + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_LOCALITY + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_STREET + " TEXT ";
			sql += ");";
			db.execSQL(sql);
		}

		if (oldVersion < 3) {
			String sql = "CREATE INDEX IF NOT EXISTS ";
			sql += ChatTableMetaData.INDEX_JID;
			sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
			sql += ChatTableMetaData.FIELD_JID;
			sql += ")";
			db.execSQL(sql);

			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += VCardsCacheTableMetaData.INDEX_JID;
			sql += " ON " + VCardsCacheTableMetaData.TABLE_NAME + " (";
			sql += VCardsCacheTableMetaData.FIELD_JID;
			sql += ")";
			db.execSQL(sql);

			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += GeolocationTableMetaData.INDEX_JID;
			sql += " ON " + GeolocationTableMetaData.TABLE_NAME + " (";
			sql += GeolocationTableMetaData.FIELD_JID;
			sql += ")";
			db.execSQL(sql);
		}

		if (oldVersion < 4) {
			String sql = "ALTER TABLE " + ChatTableMetaData.TABLE_NAME + " ADD COLUMN " + ChatTableMetaData.FIELD_MESSAGE_ID
					+ " TEXT ";
			db.execSQL(sql);

			sql = "ALTER TABLE " + ChatTableMetaData.TABLE_NAME + " ADD COLUMN " + ChatTableMetaData.FIELD_RECEIPT_STATUS
					+ " INTEGER DEFAULT 0 ";
			db.execSQL(sql);

			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += ChatTableMetaData.FIELD_MESSAGE_ID;
			sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
			sql += ChatTableMetaData.FIELD_MESSAGE_ID;
			sql += ")";
			db.execSQL(sql);
		}
	}

}
