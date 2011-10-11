package org.tigase.mobile.db.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.tigase.mobile.CPresence;
import org.tigase.mobile.XmppService;
import org.tigase.mobile.db.RosterTableMetaData;

import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore.Predicate;
import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import android.util.Log;

public class RosterCursor extends AbstractCursor {

	private final static boolean DEBUG = false;

	private static final String createComparable(String name, CPresence p) {
		String r = "000" + (1000 - p.getId());
		r = r.substring(r.length() - 5) + ":" + name.toLowerCase();
		return r;
	}

	private final String[] COLUMN_NAMES = { RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_JID,
			RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_SUBSCRIPTION,
			RosterTableMetaData.FIELD_DISPLAY_NAME, RosterTableMetaData.FIELD_PRESENCE, };

	private final ArrayList<RosterItem> items = new ArrayList<RosterItem>();

	private final Predicate predicate;

	public RosterCursor(RosterStore.Predicate predicate) {
		this.predicate = predicate;
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
			// XXX DIRTY HACK!
			return items.get(mPos).hashCode();
			// return items.get(mPos).getData("ID");
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
			return RosterProvider.getShowOf(item.getJid()).getId();
		}
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
		return String.valueOf(get(column));
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

	private final void loadData() {
		List<RosterItem> r = XmppService.jaxmpp().getRoster().getAll(predicate);

		Collections.sort(r, new Comparator<RosterItem>() {

			@Override
			public int compare(RosterItem object1, RosterItem object2) {
				try {
					String n1 = RosterProvider.getDisplayName(object1);
					String n2 = RosterProvider.getDisplayName(object2);

					CPresence s1 = RosterProvider.getShowOf(object1);
					CPresence s2 = RosterProvider.getShowOf(object2);

					return createComparable(n1, s1).compareTo(createComparable(n2, s2));

					// return n1.compareTo(n2);
				} catch (Exception e) {
					return 0;
				}
			}
		});
		synchronized (this.items) {
			this.items.clear();
			this.items.addAll(r);
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
