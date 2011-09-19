package org.tigase.mobile.db.providers;

import java.util.HashMap;
import java.util.Map;

import org.tigase.mobile.CPresence;
import org.tigase.mobile.XmppService;
import org.tigase.mobile.db.RosterTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class RosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.RosterProvider";

	public static final String CONTENT_URI = "content://" + AUTHORITY + "/roster";

	private static final boolean DEBUG = false;

	public static final String PRESENCE_URI = "content://" + AUTHORITY + "/presence";

	protected static final int PRESENCE_URI_INDICATOR = 3;

	protected static final int ROSTER_ITEM_URI_INDICATOR = 2;

	protected static final int ROSTER_URI_INDICATOR = 1;

	private final static Map<String, String> rosterProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;

		{
			put(RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_ID);
			put(RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_JID);
			put(RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_NAME);
			put(RosterTableMetaData.FIELD_SUBSCRIPTION, RosterTableMetaData.FIELD_SUBSCRIPTION);
			put(RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_ASK);
			put(RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_PRESENCE);
			put(RosterTableMetaData.FIELD_DISPLAY_NAME, RosterTableMetaData.FIELD_DISPLAY_NAME);
		}
	};

	private static final String TAG = "tigase";

	public static String getDisplayName(final BareJID jid) {
		tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = XmppService.jaxmpp().getRoster().get(jid);
		return getDisplayName(item);
	}

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
			Log.e(TAG, "Can't calculate presence", e);
			return CPresence.error;
		}
	}

	private MessengerDatabaseHelper dbHelper;

	protected final UriMatcher uriMatcher;

	public RosterProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "presence", PRESENCE_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "roster", ROSTER_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "roster/*", ROSTER_ITEM_URI_INDICATOR);

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR: {
			// clearing stored roster
			int c = db.delete(RosterTableMetaData.TABLE_NAME, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(Uri.parse(RosterProvider.CONTENT_URI), null);
			return c;
		}
		case ROSTER_ITEM_URI_INDICATOR: {
			long pk = extractPrimaryKey(uri);
			int c = db.delete(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_ID + '=' + pk, selectionArgs);
			if (c > 0) {
				Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), pk);
				getContext().getContentResolver().notifyChange(insertedItem, null);
			}
			return c;
		}
		case PRESENCE_URI_INDICATOR: {
			ContentValues values = new ContentValues();
			values.put(RosterTableMetaData.FIELD_PRESENCE, CPresence.offline.getId());
			int i = db.update(RosterTableMetaData.TABLE_NAME, values, RosterTableMetaData.FIELD_PRESENCE + '>'
					+ CPresence.offline.getId(), null);

			if (DEBUG)
				Log.d(TAG, "Update presence to offline of " + i + " buddies");
			getContext().getContentResolver().notifyChange(Uri.parse(RosterProvider.CONTENT_URI), null);
			return i;
		}
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

	}

	private long extractPrimaryKey(Uri uri) {
		String d = uri.getLastPathSegment();
		try {
			return Long.valueOf(d);
		} catch (NumberFormatException e) {
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(RosterTableMetaData.TABLE_NAME);
			qb.setProjectionMap(rosterProjectionMap);
			qb.appendWhere(RosterTableMetaData.FIELD_JID + "='" + d + "'");

			SQLiteDatabase db = dbHelper.getReadableDatabase();

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
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_TYPE;
		case ROSTER_ITEM_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (uriMatcher.match(uri) != ROSTER_URI_INDICATOR)
			throw new IllegalArgumentException("Unsupported URI " + uri);

		SQLiteDatabase db = dbHelper.getWritableDatabase();

		long rowId = db.insert(RosterTableMetaData.TABLE_NAME, RosterTableMetaData.FIELD_JID, values);
		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(CONTENT_URI), rowId);
		getContext().getContentResolver().notifyChange(insertedItem, null);
		return insertedItem;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			qb.setTables(RosterTableMetaData.TABLE_NAME);
			qb.setProjectionMap(rosterProjectionMap);
			break;
		case ROSTER_ITEM_URI_INDICATOR:

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, RosterTableMetaData.FIELD_PRESENCE
				+ " DESC, " + RosterTableMetaData.FIELD_DISPLAY_NAME + " ASC");

		int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (uriMatcher.match(uri) != ROSTER_ITEM_URI_INDICATOR)
			throw new IllegalArgumentException("Unsupported URI " + uri);

		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		long pk = extractPrimaryKey(uri);

		int changed = db.update(RosterTableMetaData.TABLE_NAME, values, RosterTableMetaData.FIELD_ID + '=' + pk, null);

		if (changed > 0) {
			Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), pk);
			getContext().getContentResolver().notifyChange(insertedItem, null);
		}

		return changed;
	}

}
