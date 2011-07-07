package org.tigase.mobile.db;

import java.util.HashMap;
import java.util.Map;

import org.tigase.mobile.db.providers.AbstractRosterProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class MessengerDatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger.db";

	public static final Integer DATABASE_VERSION = 1;

	private final Context context;

	private final Map<String, String> rosterProjectionMap = new HashMap<String, String>();

	public MessengerDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;

		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_ID);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_JID);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_NAME);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_SUBSCRIPTION, RosterTableMetaData.FIELD_SUBSCRIPTION);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_ASK);
	}

	public void clearRoster() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("DELETE FROM " + RosterTableMetaData.TABLE_NAME);

		context.getContentResolver().notifyChange(Uri.parse(AbstractRosterProvider.CONTENT_URI), null);
	}

	public long getRosterItemId(final BareJID jid) {
		final SQLiteDatabase db = getReadableDatabase();
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(RosterTableMetaData.TABLE_NAME);
		qb.setProjectionMap(rosterProjectionMap);
		qb.appendWhere(RosterTableMetaData.FIELD_JID + "='" + jid.toString() + "'");

		Cursor c = qb.query(db, null, null, null, null, null, null);
		final int column = c.getColumnIndex("_id");
		int i = c.getCount();
		if (i > 0) {
			c.moveToFirst();
			long result = c.getLong(column);
			c.close();
			return result;
		} else
			return -1;

	}

	public Map<String, String> getRosterProjectionMap() {
		return rosterProjectionMap;
	}

	public Uri insertRosterItem(final RosterItem item) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);

		long rowId = db.insert(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_JID, values);

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);

		return insertedItem;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + RosterTableMetaData.TABLE_NAME);

		String sql = "CREATE TABLE " + RosterTableMetaData.TABLE_NAME + " (";
		sql += RosterTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += RosterTableMetaData.FIELD_JID + " TEXT, ";
		sql += RosterTableMetaData.FIELD_NAME + " TEXT, ";
		sql += RosterTableMetaData.FIELD_SUBSCRIPTION + " TEXT, ";
		sql += RosterTableMetaData.FIELD_ASK + " INTEGER";
		sql += ");";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("z", "Database upgrade from version " + oldVersion + " to " + newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + RosterTableMetaData.TABLE_NAME);
		onCreate(db);
	}

	public void removeRosterItem(final RosterItem item) {
		long rowId = getRosterItemId(item.getJid());

		SQLiteDatabase db = getWritableDatabase();
		int removed = db.delete(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_ID + '=' + rowId, null);
		System.out.println("REMOVED ROWS=" + removed);

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);
	}

	public void updateRosterItem(final RosterItem item) {
		long rowId = getRosterItemId(item.getJid());

		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);

		int changed = db.update(RosterTableMetaData.TABLE_NAME, values, RosterTableMetaData.FIELD_ID + '=' + rowId, null);
		System.out.println("CHANGED ROWS=" + changed);

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);

	}
}
