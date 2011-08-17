package org.tigase.mobile.db;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.tigase.mobile.CPresence;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.XmppService;
import org.tigase.mobile.db.providers.AbstractRosterProvider;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
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

	public static String getDisplayName(final RosterItem item) {
		if (item == null)
			return null;
		else if (item.getName() != null && item.getName().length() != 0) {
			return item.getName();
		} else {
			return item.getJid().toString();
		}
	}

	public static CPresence getShowOf(final BareJID jid) {
		try {
			tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = XmppService.jaxmpp().getRoster().get(jid);
			if (item == null)
				return CPresence.notinroster;
			if (item.isAsk())
				return CPresence.requested;
			if (item.getSubscription() == Subscription.none || item.getSubscription() == Subscription.to)
				return CPresence.offline_nonauth;
			PresenceStore presenceStore = XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).getPresence();
			Presence p = presenceStore.getBestPresence(jid);
			CPresence r = CPresence.offline;
			if (p != null) {
				if (p.getType() == StanzaType.unavailable)
					r = CPresence.offline;
				else if (p.getShow() == Show.online)
					r = CPresence.online;
				else if (p.getShow() == Show.away)
					r = CPresence.away;
				else if (p.getShow() == Show.chat)
					r = CPresence.chat;
				else if (p.getShow() == Show.dnd)
					r = CPresence.dnd;
				else if (p.getShow() == Show.xa)
					r = CPresence.xa;
			}
			return r;
		} catch (Exception e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, "Can't calculate presence", e);
			return CPresence.error;
		}
	}

	private final Map<String, String> chatHistoryProjectionMap = new HashMap<String, String>();

	private final Context context;

	private SQLiteDatabase db;

	private final Map<String, String> rosterProjectionMap = new HashMap<String, String>();

	public MessengerDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;

		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_BODY, ChatTableMetaData.FIELD_BODY);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_ID, ChatTableMetaData.FIELD_ID);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_JID, ChatTableMetaData.FIELD_JID);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_STATE);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_THREAD_ID, ChatTableMetaData.FIELD_THREAD_ID);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_TIMESTAMP);
		this.chatHistoryProjectionMap.put(ChatTableMetaData.FIELD_TYPE, ChatTableMetaData.FIELD_TYPE);

		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_ID);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_JID);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_NAME);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_SUBSCRIPTION, RosterTableMetaData.FIELD_SUBSCRIPTION);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_ASK);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_PRESENCE);
		this.rosterProjectionMap.put(RosterTableMetaData.FIELD_DISPLAY_NAME, RosterTableMetaData.FIELD_DISPLAY_NAME);
	}

	public void addChatHistory(int type, Chat chat, String message) {
		try {
			ContentValues values = new ContentValues();
			values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
			values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
			values.put(ChatTableMetaData.FIELD_BODY, message);
			values.put(ChatTableMetaData.FIELD_TYPE, type);
			// values.put(ChatTableMetaData.FIELD_THREAD_ID, chat.ge);
			values.put(ChatTableMetaData.FIELD_STATE, 0);

			long rowId = db.insert(ChatTableMetaData.TABLE_NAME, ChatTableMetaData.FIELD_JID, values);
			Uri insertedItem = ContentUris.withAppendedId(
					Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString()), rowId);
			context.getContentResolver().notifyChange(insertedItem, null);
		} catch (Exception e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public void clearRoster() {
		db.execSQL("DELETE FROM " + RosterTableMetaData.TABLE_NAME);
		// db.close();
		context.getContentResolver().notifyChange(Uri.parse(AbstractRosterProvider.CONTENT_URI), null);
	}

	@Override
	public void close() {
		if (this.db != null) {
			db.close();
			db = null;
		}
	}

	public Map<String, String> getChatHistoryProjectionMap() {
		return this.chatHistoryProjectionMap;
	}

	public SQLiteDatabase getDatabase() {
		return this.db;
	}

	private long getRosterItemId(final BareJID jid) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(RosterTableMetaData.TABLE_NAME);
		qb.setProjectionMap(rosterProjectionMap);
		qb.appendWhere(RosterTableMetaData.FIELD_JID + "='" + jid.toString() + "'");

		Cursor c = null;
		try {
			c = qb.query(db, null, null, null, null, null, null);
			final int column = c.getColumnIndex("_id");
			int i = c.getCount();
			if (i > 0) {
				c.moveToFirst();
				long result = c.getLong(column);
				return result;
			} else
				return -1;
		} finally {
			c.close();
		}
	}

	public Map<String, String> getRosterProjectionMap() {
		return rosterProjectionMap;
	}

	public Uri insertRosterItem(final RosterItem item) {

		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);
		values.put(RosterTableMetaData.FIELD_PRESENCE, getShowOf(item.getJid()).getId());
		values.put(RosterTableMetaData.FIELD_DISPLAY_NAME, getDisplayName(item));

		long rowId = db.insert(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_JID, values);
		// db.close();

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);

		return insertedItem;
	}

	public void makeAllOffline() {
		ContentValues values = new ContentValues();

		values.put(RosterTableMetaData.FIELD_PRESENCE, CPresence.offline.getId());

		int i = db.update(RosterTableMetaData.TABLE_NAME, values,
				RosterTableMetaData.FIELD_PRESENCE + '>' + CPresence.offline.getId(), null);

		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Update presence to offline of " + i + " buddies");

		// db.close();

		context.getContentResolver().notifyChange(Uri.parse(AbstractRosterProvider.CONTENT_URI), null);
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
		Log.w(TigaseMobileMessengerActivity.LOG_TAG, "Database upgrade from version " + oldVersion + " to " + newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + RosterTableMetaData.TABLE_NAME);
		onCreate(db);
	}

	public void open() {
		this.db = getWritableDatabase();
	}

	public void removeRosterItem(final RosterItem item) {
		long rowId = getRosterItemId(item.getJid());

		int removed = db.delete(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_ID + '=' + rowId, null);
		// db.close();
		System.out.println("REMOVED ROWS=" + removed);

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);
	}

	public void updateRosterItem(final Presence presence) {
		try {
			final long rowId = getRosterItemId(presence.getFrom().getBareJid());
			if (rowId == -1)
				return;

			ContentValues values = new ContentValues();

			values.put(RosterTableMetaData.FIELD_PRESENCE, getShowOf(presence.getFrom().getBareJid()).getId());

			int changed = db.update(RosterTableMetaData.TABLE_NAME, values, RosterTableMetaData.FIELD_ID + '=' + rowId, null);
			System.out.println("CHANGED ROWS=" + changed);
			// db.close();

			Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
			context.getContentResolver().notifyChange(insertedItem, null);
		} catch (Exception e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, "Can't update presence", e);
		}
	}

	public void updateRosterItem(final RosterItem item) {
		long rowId = getRosterItemId(item.getJid());

		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);
		values.put(RosterTableMetaData.FIELD_PRESENCE, getShowOf(item.getJid()).getId());
		values.put(RosterTableMetaData.FIELD_DISPLAY_NAME, getDisplayName(item));

		int changed = db.update(RosterTableMetaData.TABLE_NAME, values, RosterTableMetaData.FIELD_ID + '=' + rowId, null);
		System.out.println("CHANGED ROWS=" + changed);
		// db.close();

		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(AbstractRosterProvider.CONTENT_URI), rowId);
		context.getContentResolver().notifyChange(insertedItem, null);

	}
}
