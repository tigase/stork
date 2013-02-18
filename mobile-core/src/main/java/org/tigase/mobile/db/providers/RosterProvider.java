package org.tigase.mobile.db.providers;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore.Predicate;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class RosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.RosterProvider";

	public static final String CONTENT_URI = "content://" + AUTHORITY + "/roster";

	private static final boolean DEBUG = false;

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static final int GROUP_ITEM_URI_INDICATOR = 4;

	public static final String GROUP_URI = "content://" + AUTHORITY + "/groups";

	protected static final int GROUPS_URI_INDICATOR = 3;

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
			put(RosterTableMetaData.FIELD_ACCOUNT, RosterTableMetaData.FIELD_ACCOUNT);
		}
	};

	private static final String TAG = "tigase";

	public static final String VCARD_URI = "content://" + AUTHORITY + "/vcard";

	protected static final int VCARD_URI_INDICATOR = 5;

	public static String encodeHex(byte[] data) {

		int l = data.length;

		char[] out = new char[l << 1];

		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}

		return new String(out);
	}

	private MessengerDatabaseHelper dbHelper;

	protected final UriMatcher uriMatcher;

	public RosterProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "roster", ROSTER_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "roster/*", ROSTER_ITEM_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "groups", GROUPS_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "groups/*", GROUP_ITEM_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "vcard/*", VCARD_URI_INDICATOR);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new RuntimeException("There is nothing to delete! uri=" + uri);
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_TYPE;
		case GROUPS_URI_INDICATOR:
			return RosterTableMetaData.GROUPS_TYPE;
		case GROUP_ITEM_URI_INDICATOR:
			return RosterTableMetaData.GROUP_ITEM_TYPE;
		case ROSTER_ITEM_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int indicator = uriMatcher.match(uri);
		if (indicator == VCARD_URI_INDICATOR) {
			String jid = uri.getLastPathSegment();
			Log.v(TAG, "inserting vcard for = " + jid);
			// final Jaxmpp jaxmpp = ((MessengerApplication)
			// getContext().getApplicationContext()).getJaxmpp();
			// RosterItem rosterItem =
			// jaxmpp.getRoster().get(BareJID.bareJIDInstance(jid));
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.beginTransaction();
			try {
				db.execSQL("DELETE FROM " + VCardsCacheTableMetaData.TABLE_NAME + " WHERE "
						+ VCardsCacheTableMetaData.FIELD_JID + "='" + jid + "'");
				try {
					MessageDigest md = MessageDigest.getInstance("SHA1");
					md.update(values.getAsByteArray(VCardsCacheTableMetaData.FIELD_DATA));
					String md5 = encodeHex(md.digest());
					values.put(VCardsCacheTableMetaData.FIELD_HASH, md5);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				values.put(VCardsCacheTableMetaData.FIELD_JID, jid);
				values.put(VCardsCacheTableMetaData.FIELD_TIMESTAMP, (new Date()).getTime());
				db.insert(VCardsCacheTableMetaData.TABLE_NAME, null, values);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}

			AvatarHelper.clearAvatar(BareJID.bareJIDInstance(jid));

			for (JaxmppCore jaxmpp : ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get()) {
				RosterItem rosterItem = jaxmpp.getRoster().get(BareJID.bareJIDInstance(jid));
				if (rosterItem != null) {
					Uri u = Uri.parse(CONTENT_URI + "/" + rosterItem.getId());
					getContext().getContentResolver().notifyChange(u, null);
				}
			}

			return null;
		} else
			throw new RuntimeException("There is nothing to insert! (" + indicator + ") uri=" + uri);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, final String[] selectionArgs, String sortOrder) {
		Cursor c;
		Predicate p = null;
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			if (selectionArgs != null) {
				final String g = selectionArgs[0];
				p = new Predicate() {

					@Override
					public boolean match(RosterItem item) {
						if (g.equals("All"))
							return true;
						else if (g.equals("default") && item.getGroups().isEmpty())
							return true;
						else
							return item.getGroups().contains(g);
					}
				};
			}
			if (selection != null && "status".equals(selection)) {
				final Predicate parent = p;
				p = new Predicate() {
					@Override
					public boolean match(RosterItem item) {
						try {
							if (parent != null && !parent.match(item))
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
			if (selection != null && "feature".equals(selection)) {
				final Predicate parent = p;
				Iterator<JaxmppCore> jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get().iterator();
				if (jaxmpp.hasNext()) {
					CapabilitiesCache capsCache = jaxmpp.next().getModule(CapabilitiesModule.class).getCache();
					final Set<String> nodes = capsCache.getNodesWithFeature(selectionArgs[1]);
					for (int i = 2; i < selectionArgs.length; i++) {
						nodes.retainAll(capsCache.getNodesWithFeature(selectionArgs[i]));
					}
					p = new Predicate() {
						@Override
						public boolean match(RosterItem item) {
							try {
								if (parent != null && !parent.match(item))
									return false;
								SessionObject session = item.getSessionObject();
								if (session == null)
									return false;

								boolean has = false;
								Map<String, tigase.jaxmpp.core.client.xmpp.stanzas.Presence> presences = session.getPresence().getPresences(
										item.getJid());
								if (presences != null) {
									for (Map.Entry<String, tigase.jaxmpp.core.client.xmpp.stanzas.Presence> entry : presences.entrySet()) {
										Element c = entry.getValue().getChildrenNS("c", "http://jabber.org/protocol/caps");
										if (c == null)
											continue;

										final String node = c.getAttribute("node");
										final String ver = c.getAttribute("ver");

										has |= nodes.contains(node + "#" + ver);
									}
								}
								return has;
							} catch (XMLException e) {
								return false;
							}
						}
					};
				}
			}

			if (DEBUG)
				Log.d(TAG, "Querying " + uri + " projection=" + Arrays.toString(projection) + "; selection=" + selection
						+ "; selectionArgs=" + Arrays.toString(selectionArgs));

			c = new RosterCursor(getContext(), dbHelper.getReadableDatabase(), p);
			break;
		case ROSTER_ITEM_URI_INDICATOR:
			final String l = uri.getLastPathSegment();
			p = new Predicate() {

				@Override
				public boolean match(RosterItem item) {
					if (item.getJid().toString().equals(l))
						return true;
					else if (("" + item.getId()).equals(l))
						return true;
					else
						return false;
				}
			};
			c = new RosterCursor(getContext(), dbHelper.getReadableDatabase(), p);
			break;
		case GROUPS_URI_INDICATOR:
			c = new GroupsCursor(getContext(), null);
			// using CONTENT_URI for groups allows to update group visibility
			// when contacts are offline
			c.setNotificationUri(getContext().getContentResolver(), Uri.parse(CONTENT_URI));
			return c;
			// break;

			// case for VCard?
		case VCARD_URI_INDICATOR:
			c = dbHelper.getReadableDatabase().query(
					VCardsCacheTableMetaData.TABLE_NAME,
					new String[] { VCardsCacheTableMetaData.FIELD_JID, VCardsCacheTableMetaData.FIELD_DATA,
							VCardsCacheTableMetaData.FIELD_HASH }, VCardsCacheTableMetaData.FIELD_JID + "=?",
					new String[] { uri.getLastPathSegment() }, null, null, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		// int i = c.getCount();
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new RuntimeException("There is nothing to update! uri=" + uri);
	}

}
