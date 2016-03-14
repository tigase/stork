package org.tigase.messenger.phone.pro.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import tigase.jaxmpp.android.chat.OpenChatDbHelper;
import tigase.jaxmpp.android.roster.RosterDbHelper;
import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("DatabaseHelper", "Create database");
        RosterDbHelper.onCreate(db);
        OpenChatDbHelper.onCreate(db);

        db.execSQL("ALTER TABLE " + RosterItemsCacheTableMetaData.TABLE_NAME + " ADD COLUMN " + DatabaseContract.RosterItemsCache.FIELD_STATUS + " INTEGER DEFAULT 0;");

        db.execSQL(DatabaseContract.ChatHistory.CREATE_TABLE);
        db.execSQL(DatabaseContract.ChatHistory.CREATE_INDEX);

        db.execSQL(DatabaseContract.VCardsCache.CREATE_TABLE);
        db.execSQL(DatabaseContract.VCardsCache.CREATE_INDEX);

        db.execSQL(DatabaseContract.Geolocation.CREATE_TABLE);
        db.execSQL(DatabaseContract.Geolocation.CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DatabaseHelper", "Update database from " + oldVersion + " to " + newVersion);
        db.execSQL("ALTER TABLE " + RosterItemsCacheTableMetaData.TABLE_NAME + " ADD COLUMN " + DatabaseContract.RosterItemsCache.FIELD_STATUS + " INTEGER DEFAULT 0;");

    }
}
