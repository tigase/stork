package org.tigase.mobile.db.providers;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class MessengerDatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger.db";

	public static final Integer DATABASE_VERSION = 1;

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public MessengerDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE " + RosterTableMetaData.TABLE_NAME + " (";
		sql += RosterTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += RosterTableMetaData.FIELD_JID + " TEXT, ";
		sql += RosterTableMetaData.FIELD_NAME + " TEXT, ";
		sql += RosterTableMetaData.FIELD_DISPLAY_NAME + " TEXT, ";
		sql += RosterTableMetaData.FIELD_SUBSCRIPTION + " TEXT, ";
		sql += RosterTableMetaData.FIELD_ASK + " INTEGER, ";
		sql += RosterTableMetaData.FIELD_PRESENCE + " INTEGER";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE TABLE " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += ChatTableMetaData.FIELD_TYPE + " INTEGER, ";
		sql += ChatTableMetaData.FIELD_JID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_TIMESTAMP + " DATETIME, ";
		sql += ChatTableMetaData.FIELD_THREAD_ID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_BODY + " TEXT, ";
		sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
		sql += ");";

		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Database upgrade from version " + oldVersion + " to " + newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + RosterTableMetaData.TABLE_NAME);
		onCreate(db);
	}

}
