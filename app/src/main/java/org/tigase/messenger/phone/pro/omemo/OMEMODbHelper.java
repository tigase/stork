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
package org.tigase.messenger.phone.pro.omemo;

import android.database.sqlite.SQLiteDatabase;

public class OMEMODbHelper {

	private static String CREATE_IDENTITY_STATEMENT =
			"CREATE TABLE " + OMEMOContract.Identities.TABLE_NAME + "(" + OMEMOContract.Identities.FIELD_ACCOUNT +
					" TEXT,  " + OMEMOContract.Identities.FIELD_JID + " TEXT, " + OMEMOContract.Identities.FIELD_KEY +
					" TEXT, " + OMEMOContract.Identities.FIELD_FINGERPRINT + " TEXT, " +
					OMEMOContract.Identities.FIELD_ACTIVE + " INTEGER DEFAULT 1, " +
					OMEMOContract.Identities.FIELD_DEVICE_ID + " INTEGER, " +
					OMEMOContract.Identities.FIELD_HAS_KEYPAIR + " INTEGER DEFAULT 0, " +
					OMEMOContract.Identities.FIELD_TRUST + " INTEGER, " + OMEMOContract.Identities.FIELD_LAST_USAGE +
					" DATETIME, " + "UNIQUE (" + OMEMOContract.Identities.FIELD_ACCOUNT + ", " +
					OMEMOContract.Identities.FIELD_JID + ", " + OMEMOContract.Identities.FIELD_FINGERPRINT +
					") ON CONFLICT REPLACE" + ");";
	private static String CREATE_PREKEYS_STATEMENT =
			"CREATE TABLE " + OMEMOContract.PreKeys.TABLE_NAME + "(" + OMEMOContract.PreKeys.FIELD_ACCOUNT +
					" TEXT,  " + OMEMOContract.PreKeys.FIELD_ID + " INTEGER, " + OMEMOContract.PreKeys.FIELD_KEY +
					" TEXT, " + "UNIQUE (" + OMEMOContract.PreKeys.FIELD_ACCOUNT + ", " +
					OMEMOContract.PreKeys.FIELD_ID + ") ON CONFLICT REPLACE" + ");";
	private static String CREATE_SESSIONS_STATEMENT =
			"CREATE TABLE " + OMEMOContract.Sessions.TABLE_NAME + "(" + OMEMOContract.Sessions.FIELD_ACCOUNT +
					" TEXT, " + OMEMOContract.Sessions.FIELD_JID + " TEXT, " + OMEMOContract.Sessions.FIELD_DEVICE_ID +
					" INTEGER, " + OMEMOContract.Sessions.FIELD_KEY + " TEXT, " + "UNIQUE (" +
					OMEMOContract.Sessions.FIELD_ACCOUNT + ", " + OMEMOContract.Sessions.FIELD_JID + ", " +
					OMEMOContract.Sessions.FIELD_DEVICE_ID + ") ON CONFLICT REPLACE" + ");";
	private static String CREATE_SIGNED_PREKEYS_STATEMENT =
			"CREATE TABLE " + OMEMOContract.SignedPreKeys.TABLE_NAME + "(" + OMEMOContract.SignedPreKeys.FIELD_ACCOUNT +
					" TEXT,  " + OMEMOContract.SignedPreKeys.FIELD_ID + " INTEGER, " +
					OMEMOContract.SignedPreKeys.FIELD_KEY + " TEXT, " + "UNIQUE (" +
					OMEMOContract.SignedPreKeys.FIELD_ACCOUNT + ", " + OMEMOContract.SignedPreKeys.FIELD_ID +
					") ON CONFLICT REPLACE" + ");";

	public void onCreate(final SQLiteDatabase db) {
		db.execSQL(CREATE_PREKEYS_STATEMENT);
		db.execSQL(CREATE_SIGNED_PREKEYS_STATEMENT);
		db.execSQL(CREATE_IDENTITY_STATEMENT);
		db.execSQL(CREATE_SESSIONS_STATEMENT);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion <= 9) {
			db.execSQL(CREATE_PREKEYS_STATEMENT);
			db.execSQL(CREATE_SIGNED_PREKEYS_STATEMENT);
			db.execSQL(CREATE_IDENTITY_STATEMENT);
			db.execSQL(CREATE_SESSIONS_STATEMENT);
		}
		if (oldVersion <= 11) {
			db.execSQL("DROP TABLE " + OMEMOContract.Identities.TABLE_NAME);
			db.execSQL(CREATE_IDENTITY_STATEMENT);

		}
	}
}
