package org.tigase.mobile.db.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.db.RosterCacheTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBRosterCacheProvider implements RosterCacheProvider {

	private static String createKey(SessionObject sessionObject) {
		return Preferences.ROSTER_VERSION_KEY + "." + sessionObject.getUserJid().getBareJid();
	}

	private final Context context;

	private final MessengerDatabaseHelper dbHelper;

	private SharedPreferences prefs;

	public DBRosterCacheProvider(Context context) {
		this.context = context;
		this.dbHelper = new MessengerDatabaseHelper(context);
		this.prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);

	}

	@Override
	public String getCachedVersion(SessionObject sessionObject) {
		return prefs.getString(createKey(sessionObject), "");
	}

	@Override
	public Collection<RosterItem> loadCachedRoster(final SessionObject sessionObject) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String where = null;
		if (sessionObject.getUserJid() != null)
			where = RosterCacheTableMetaData.FIELD_ACCOUNT + "='" + sessionObject.getUserJid().getBareJid() + "'";
		final Cursor c = db.query(RosterCacheTableMetaData.TABLE_NAME, new String[] { RosterCacheTableMetaData.FIELD_ID,
				RosterCacheTableMetaData.FIELD_ACCOUNT, RosterCacheTableMetaData.FIELD_JID,
				RosterCacheTableMetaData.FIELD_NAME, RosterCacheTableMetaData.FIELD_GROUP_NAME,
				RosterCacheTableMetaData.FIELD_SUBSCRIPTION, RosterCacheTableMetaData.FIELD_ASK }, where, null, null, null,
				null);
		final ArrayList<RosterItem> items = new ArrayList<RosterItem>();
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(c.getColumnIndex(RosterCacheTableMetaData.FIELD_ID));
				final BareJID jid = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_JID)));
				final String name = c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME));
				final String groups = c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_GROUP_NAME));
				final Subscription s = Subscription.valueOf(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_SUBSCRIPTION)));
				boolean ask = c.getInt(c.getColumnIndex(RosterCacheTableMetaData.FIELD_ASK)) == 1;

				final RosterItem ri = new RosterItem(jid, sessionObject);
				ri.setData(RosterItem.ID_KEY, id);
				ri.setName(name);
				ri.setAsk(ask);
				ri.setSubscription(s);
				if (groups != null && groups.length() > 0) {
					String[] ggs = groups.split(";");
					for (String string : ggs) {
						ri.getGroups().add(string);
					}
				}
				updateAvatarData(ri);
				items.add(ri);
			}
		} finally {
			c.close();
		}
		return items;
	}

	private String serializeGroups(ArrayList<String> groups) {
		String r = "";
		Iterator<String> it = groups.iterator();
		while (it.hasNext()) {
			r += it.next();
			if (it.hasNext())
				r += ";";
		}
		return r;
	}

	private void storeCache(SessionObject sessionObject, final String receivedVersion) {
		final Jaxmpp jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(sessionObject);

		final List<RosterItem> items = jaxmpp.getRoster().getAll();
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM " + RosterCacheTableMetaData.TABLE_NAME + " WHERE "
					+ RosterCacheTableMetaData.FIELD_ACCOUNT + "='" + sessionObject.getUserJid().getBareJid().toString() + "'");
			for (RosterItem rosterItem : items) {
				ContentValues v = new ContentValues();

				v.put(RosterCacheTableMetaData.FIELD_ID, rosterItem.getId());
				v.put(RosterCacheTableMetaData.FIELD_JID, rosterItem.getJid().toString());
				v.put(RosterCacheTableMetaData.FIELD_ACCOUNT, sessionObject.getUserJid().getBareJid().toString());
				v.put(RosterCacheTableMetaData.FIELD_NAME, rosterItem.getName());
				v.put(RosterCacheTableMetaData.FIELD_GROUP_NAME, serializeGroups(rosterItem.getGroups()));
				v.put(RosterCacheTableMetaData.FIELD_SUBSCRIPTION, rosterItem.getSubscription().name());
				v.put(RosterCacheTableMetaData.FIELD_ASK, rosterItem.isAsk());
				v.put(RosterCacheTableMetaData.FIELD_TIMESTAMP, (new Date()).getTime());

				db.insert(RosterCacheTableMetaData.TABLE_NAME, null, v);
			}
			prefs.edit().putString(createKey(sessionObject), receivedVersion).commit();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

	}

	private void updateAvatarData(RosterItem item) {
		final Cursor c = dbHelper.getReadableDatabase().rawQuery(
				"SELECT * FROM " + VCardsCacheTableMetaData.TABLE_NAME + " WHERE " + VCardsCacheTableMetaData.FIELD_JID + "='"
						+ item.getJid() + "'", null);
		try {
			while (c.moveToNext()) {
				String sha = c.getString(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_HASH));
				item.setData("photo", sha);
			}
		} finally {
			c.close();
		}

	}

	@Override
	public void updateReceivedVersion(SessionObject sessionObject, String ver) {
		if (ver.equals(getCachedVersion(sessionObject)))
			return;
		storeCache(sessionObject, ver);
	}

}
