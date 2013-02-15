package org.tigase.mobile.db.providers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore.Predicate;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

public class RosterCursor extends AbstractCursor {

	private final static boolean DEBUG = false;

	// is should not be used - use AvatarHelper class instead
	// private HashMap<RosterItem, byte[]> avatarCache = new HashMap<RosterItem,
	// byte[]>();

	private final String[] COLUMN_NAMES = { RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_JID,
			RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_SUBSCRIPTION,
			RosterTableMetaData.FIELD_DISPLAY_NAME, RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_GROUP_NAME,
			RosterTableMetaData.FIELD_STATUS_MESSAGE, /*
													 * RosterTableMetaData.
													 * FIELD_AVATAR,
													 */RosterTableMetaData.FIELD_ACCOUNT };

	private final Context context;

	private final SQLiteDatabase db;

	private final ArrayList<RosterItem> items = new ArrayList<RosterItem>();

	private final Predicate predicate;

	private final RosterDisplayTools rdt;

	public RosterCursor(Context ctx, SQLiteDatabase sqLiteDatabase, RosterStore.Predicate predicate) {
		this.rdt = new RosterDisplayTools(ctx);
		this.context = ctx;
		this.predicate = predicate;
		this.db = sqLiteDatabase;
		loadData();
	}

	private Object get(int column) {
		List<RosterItem> items = getRoster();
		if (column < 0 || column >= COLUMN_NAMES.length) {
			throw new CursorIndexOutOfBoundsException("Requested column: " + column + ", # of columns: " + COLUMN_NAMES.length);
		}
		if (mPos < 0) {
			throw new CursorIndexOutOfBoundsException("Before first row.");
		}
		if (mPos >= items.size()) {
			throw new CursorIndexOutOfBoundsException("After last row.");
		}
		switch (column) {
		case 0:
			return items.get(mPos).getId();
		case 1:
			return items.get(mPos).getJid();
		case 2:
			return items.get(mPos).getName();
		case 3:
			return items.get(mPos).isAsk();
		case 4:
			return items.get(mPos).getSubscription();
		case 5: {
			RosterItem item = items.get(mPos);
			if (item.getName() != null && item.getName().length() != 0) {
				return item.getName();
			} else {
				return item.getJid().toString();
			}
		}
		case 6: {
			RosterItem item = items.get(mPos);
			return rdt.getShowOf(item.getSessionObject(), item.getJid()).getId();
		}
		case 7: {
			RosterItem item = items.get(mPos);
			String x = item.getGroups().size() == 0 ? "default" : item.getGroups().get(0);
			return x;
		}
		case 8: {
			RosterItem item = items.get(mPos);
			return rdt.getStatusMessageOf(item);
		}
		case 9: {
			// RosterItem item = items.get(mPos);
			// return readAvatar(item);
			// }
			// case 10: {
			RosterItem item = items.get(mPos);
			BareJID jid = item.getSessionObject().getUserBareJid();
			return jid == null ? null : jid.toString();
		}
		default:
			throw new CursorIndexOutOfBoundsException("Unknown column!");
		}
	}

	@Override
	public byte[] getBlob(int column) {
		Object value = get(column);
		return (byte[]) value;
	}

	@Override
	public String[] getColumnNames() {
		return COLUMN_NAMES;
	}

	@Override
	public int getCount() {
		if (DEBUG)
			Log.d("tigase", "rosterCursor.getCount()==" + getRoster().size());
		return getRoster().size();
	}

	@Override
	public double getDouble(int column) {
		Object value = get(column);
		return (value instanceof String) ? Double.valueOf((String) value) : ((Number) value).doubleValue();
	}

	@Override
	public float getFloat(int column) {
		Object value = get(column);
		return (value instanceof String) ? Float.valueOf((String) value) : ((Number) value).floatValue();
	}

	@Override
	public int getInt(int column) {
		Object value = get(column);
		return (value instanceof String) ? Integer.valueOf((String) value) : ((Number) value).intValue();
	}

	@Override
	public long getLong(int column) {
		Object value = get(column);
		return (value instanceof String) ? Long.valueOf((String) value) : ((Number) value).longValue();
	}

	private final synchronized List<RosterItem> getRoster() {
		return items;
	}

	@Override
	public short getShort(int column) {
		Object value = get(column);
		return (value instanceof String) ? Short.valueOf((String) value) : ((Number) value).shortValue();
	}

	@Override
	public String getString(int column) {
		Object s = get(column);
		return s == null ? null : String.valueOf(s);
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

	private final void loadData() {
		final MultiJaxmpp multi = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();
		final boolean showOffline = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.SHOW_OFFLINE,
				Boolean.TRUE);

		Predicate pr = predicate;
		if (!showOffline) {
			pr = new Predicate() {
				@Override
				public boolean match(RosterItem item) {
					try {
						if (predicate != null && !predicate.match(item))
							return false;
						SessionObject session = item.getSessionObject();
						if (session == null)
							return false;
						return session.getPresence().isAvailable(item.getJid());
					} catch (XMLException e) {
						return false;
					}
				}
			};
		}

		ArrayList<RosterItem> r = new ArrayList<RosterItem>();
		for (JaxmppCore jaxmpp : multi.get()) {
			r.addAll(jaxmpp.getRoster().getAll(pr));
		}

		String sorting = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.ROSTER_SORTING_KEY,
				"status");

		if ("status".equals(sorting)) {
			MergeSort.sort(r, new Comparator<RosterItem>() {

				@Override
				public int compare(RosterItem object1, RosterItem object2) {
					try {
						CPresence s1 = rdt.getShowOf(object1);
						CPresence s2 = rdt.getShowOf(object2);

						int sc = s2.getId().compareTo(s1.getId());
						if (sc != 0)
							return sc;

						String n1 = RosterDisplayTools.getDisplayName(object1);
						String n2 = RosterDisplayTools.getDisplayName(object2);

						return n1.compareTo(n2);
					} catch (Exception e) {
						return 0;
					}
				}
			});
		} else if ("name".endsWith(sorting)) {
			MergeSort.sort(r, new Comparator<RosterItem>() {

				@Override
				public int compare(RosterItem object1, RosterItem object2) {
					try {
						String n1 = RosterDisplayTools.getDisplayName(object1);
						String n2 = RosterDisplayTools.getDisplayName(object2);

						return n1.compareTo(n2);
					} catch (Exception e) {
						return 0;
					}
				}
			});
		}
		synchronized (this.items) {
			this.items.clear();
			this.items.addAll(r);
		}
	}

	// private byte[] readAvatar(RosterItem item) {
	// // if (item.getData("photo") == null)
	// // return null;
	// // if (avatarCache.containsKey(item)) {
	// // if (DEBUG)
	// // Log.d("tigase", "Getting from cache avatar of user " + item.getJid());
	// // return avatarCache.get(item);
	// // }
	// if (DEBUG)
	// Log.d("tigase", "Reading avatar of user " + item.getJid());
	// final Cursor c = db.rawQuery("SELECT * FROM " +
	// VCardsCacheTableMetaData.TABLE_NAME + " WHERE "
	// + VCardsCacheTableMetaData.FIELD_JID + "='" + item.getJid() + "'", null);
	// try {
	// while (c.moveToNext()) {
	// String sha =
	// c.getString(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_HASH));
	// item.setData("photo", sha);
	// byte[] data =
	// c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
	// // avatarCache.put(item, data);
	// return data;
	// }
	// return null;
	// } finally {
	// c.close();
	// }
	//
	// }

	@Override
	public boolean requery() {
		if (DEBUG)
			Log.d("tigase", "Requery()");
		loadData();
		return super.requery();
	}
}
