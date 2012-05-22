package org.tigase.mobile.db.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.db.RosterTableMetaData;

import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore.Predicate;
import android.content.Context;
import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import android.preference.PreferenceManager;
import android.util.Log;

public class GroupsCursor extends AbstractCursor {

	private final static boolean DEBUG = false;

	private final String[] COLUMN_NAMES = { RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_GROUP_NAME };

	private final Context context;

	private final ArrayList<String> items = new ArrayList<String>();

	private final Predicate predicate;

	public GroupsCursor(Context ctx, Predicate predicate) {
		this.context = ctx;
		this.predicate = predicate;
		loadData();
	}

	private Object get(int column) {
		List<String> items = this.items;
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
			// XXX DIRTY HACK!
			return items.get(mPos).hashCode();
			// return items.get(mPos).getData("ID");
		case 1:
			return items.get(mPos);
		default:
			throw new CursorIndexOutOfBoundsException("Unknown column!");
		}
	}

	@Override
	public String[] getColumnNames() {
		return COLUMN_NAMES;
	}

	@Override
	public int getCount() {
		if (DEBUG)
			Log.d("tigase", "groupsCursor.getCount()==" + items.size());
		return items.size();
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

	@Override
	public short getShort(int column) {
		Object value = get(column);
		return (value instanceof String) ? Short.valueOf((String) value) : ((Number) value).shortValue();
	}

	@Override
	public String getString(int column) {
		return String.valueOf(get(column));
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

	private final void loadData() {
		synchronized (this.items) {
			final MultiJaxmpp multi = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();

			final boolean showOffline = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
					Preferences.SHOW_OFFLINE, Boolean.TRUE);

			Predicate pr = predicate;
			if (!showOffline) {
				pr = new Predicate() {
					@Override
					public boolean match(RosterItem item) {
						if (predicate != null && !predicate.match(item)) {
							return false;
						}
						SessionObject session = item.getSessionObject();
						if (session == null)
							return false;
						try {
							return session.getPresence().isAvailable(item.getJid());
						} catch (XMLException e) {
							return false;
						}
					}
				};
			}

			final HashSet<String> tmp = new HashSet<String>();
			for (JaxmppCore j : multi.get()) {
				List<RosterItem> al = j.getRoster().getAll(pr);
				for (RosterItem rosterItem : al) {
					tmp.addAll(rosterItem.getGroups());
				}
			}

			this.items.clear();
			this.items.addAll(tmp);
			Collections.sort(this.items);
			this.items.add(0, "All");
			this.items.add(1, "default");
		}
	}

	@Override
	public boolean requery() {
		if (DEBUG)
			Log.d("tigase", "Requery()");
		loadData();
		return super.requery();
	}
}
