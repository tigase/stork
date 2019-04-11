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

package org.tigase.messenger.jaxmpp.android.caps;

import android.database.sqlite.SQLiteDatabase;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class CapsDbHelper {

	private static final String CREATE_CAPS_IDENTITIES_TABLE =
			"CREATE TABLE " + DatabaseContract.CapsIdentities.TABLE_NAME + " (" +
					DatabaseContract.CapsIdentities.FIELD_ID + " INTEGER PRIMARY KEY, " +
					DatabaseContract.CapsIdentities.FIELD_NODE + " TEXT, " +
					DatabaseContract.CapsIdentities.FIELD_NAME + " TEXT, " +
					DatabaseContract.CapsIdentities.FIELD_CATEGORY + " TEXT, " +
					DatabaseContract.CapsIdentities.FIELD_TYPE + " TEXT" + ");";

	private static final String CREATE_CAPS_FEATURES_TABLE =
			"CREATE TABLE " + DatabaseContract.CapsFeatures.TABLE_NAME + " (" + DatabaseContract.CapsFeatures.FIELD_ID +
					" INTEGER PRIMARY KEY, " + DatabaseContract.CapsFeatures.FIELD_NODE + " TEXT, " +
					DatabaseContract.CapsFeatures.FIELD_FEATURE + " TEXT" + ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_CAPS_IDENTITIES_TABLE);
		database.execSQL(CREATE_CAPS_FEATURES_TABLE);

		String sql = "CREATE INDEX IF NOT EXISTS ";
		sql += DatabaseContract.CapsFeatures.TABLE_NAME + "_" + DatabaseContract.CapsFeatures.FIELD_NODE + "_idx";
		sql += " ON " + DatabaseContract.CapsFeatures.TABLE_NAME + " (";
		sql += DatabaseContract.CapsFeatures.FIELD_NODE;
		sql += ")";
		database.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += DatabaseContract.CapsFeatures.TABLE_NAME + "_" + DatabaseContract.CapsFeatures.FIELD_FEATURE + "_idx";
		sql += " ON " + DatabaseContract.CapsFeatures.TABLE_NAME + " (";
		sql += DatabaseContract.CapsFeatures.FIELD_FEATURE;
		sql += ")";
		database.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += DatabaseContract.CapsIdentities.TABLE_NAME + "_" + DatabaseContract.CapsFeatures.FIELD_NODE + "_idx";
		sql += " ON " + DatabaseContract.CapsIdentities.TABLE_NAME + " (";
		sql += DatabaseContract.CapsIdentities.FIELD_NODE;
		sql += ")";
		database.execSQL(sql);

	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}
