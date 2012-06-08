package org.tigase.mobile.db.providers;

//import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.CapsFeaturesTableMetaData;
import org.tigase.mobile.db.CapsIdentitiesTableMetaData;
import org.tigase.mobile.db.ChatTableMetaData;
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

	public static final Integer DATABASE_VERSION = 2;

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
		sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
		sql += ");";
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
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Database upgrade from version " + oldVersion + " to " + newVersion);
		if (oldVersion == 1) {
			String sql = "CREATE TABLE " + OpenMUCTableMetaData.TABLE_NAME + " (";
			sql += OpenMUCTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
			sql += OpenMUCTableMetaData.FIELD_ACCOUNT + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_ROOM_JID + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_NICKNAME + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_PASSWORD + " TEXT, ";
			sql += OpenMUCTableMetaData.FIELD_TIMESTAMP + " DATETIME";
			sql += ");";
			db.execSQL(sql);
		}
	}

}
