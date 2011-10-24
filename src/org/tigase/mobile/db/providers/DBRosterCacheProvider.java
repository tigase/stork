package org.tigase.mobile.db.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.tigase.mobile.XmppService;
import org.tigase.mobile.db.RosterCacheTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBRosterCacheProvider implements RosterCacheProvider {

	private final Context context;

	private final MessengerDatabaseHelper dbHelper;

	private SharedPreferences prefs;

	public DBRosterCacheProvider(Context context) {
		this.context = context;
		this.dbHelper = new MessengerDatabaseHelper(context);
		this.prefs = context.getSharedPreferences("org.tigase.mobile_preferences", Context.MODE_PRIVATE);

	}

	@Override
	public String getCachedVersion() {
		return prefs.getString("roster_version", "");
	}

	@Override
	public Collection<RosterItem> loadCachedRoster() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor c = db.query(RosterCacheTableMetaData.TABLE_NAME, new String[] { RosterCacheTableMetaData.FIELD_ID,
				RosterCacheTableMetaData.FIELD_JID, RosterCacheTableMetaData.FIELD_NAME,
				RosterCacheTableMetaData.FIELD_GROUP_NAME, RosterCacheTableMetaData.FIELD_SUBSCRIPTION,
				RosterCacheTableMetaData.FIELD_ASK }, null, null, null, null, null);
		final ArrayList<RosterItem> items = new ArrayList<RosterItem>();
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(0);
				final BareJID jid = BareJID.bareJIDInstance(c.getString(1));
				final String name = c.getString(2);
				final String groups = c.getString(3);
				final Subscription s = Subscription.valueOf(c.getString(4));
				boolean ask = c.getInt(5) == 1;

				final RosterItem ri = new RosterItem(jid);
				ri.setData("id", id);
				ri.setName(name);
				ri.setAsk(ask);
				ri.setSubscription(s);
				if (groups != null && groups.length() > 0) {
					String[] ggs = groups.split(";");
					for (String string : ggs) {
						ri.getGroups().add(string);
					}
				}
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

	private void storeCache(final String receivedVersion) {
		final List<RosterItem> items = XmppService.jaxmpp(context).getRoster().getAll();
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM " + RosterCacheTableMetaData.TABLE_NAME);
			for (RosterItem rosterItem : items) {
				ContentValues v = new ContentValues();

				v.put(RosterCacheTableMetaData.FIELD_ID, rosterItem.hashCode());
				v.put(RosterCacheTableMetaData.FIELD_JID, rosterItem.getJid().toString());
				v.put(RosterCacheTableMetaData.FIELD_NAME, rosterItem.getName());
				v.put(RosterCacheTableMetaData.FIELD_GROUP_NAME, serializeGroups(rosterItem.getGroups()));
				v.put(RosterCacheTableMetaData.FIELD_SUBSCRIPTION, rosterItem.getSubscription().name());
				v.put(RosterCacheTableMetaData.FIELD_ASK, rosterItem.isAsk());

				long id = db.insert(RosterCacheTableMetaData.TABLE_NAME, null, v);
			}
			prefs.edit().putString("roster_version", receivedVersion).commit();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

	}

	@Override
	public void updateReceivedVersion(String ver) {
		if (ver.equals(getCachedVersion()))
			return;
		storeCache(ver);
	}

}
