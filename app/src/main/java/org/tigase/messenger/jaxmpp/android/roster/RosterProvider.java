/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.messenger.jaxmpp.android.roster;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;

import java.util.*;

public class RosterProvider
		implements RosterCacheProvider {

	protected final Context context;
	protected final SQLiteOpenHelper dbHelper;
	protected final Listener listener;
	private final String versionKeyPrefix;
	private SharedPreferences prefs;

	public RosterProvider(Context context, SQLiteOpenHelper dbHelper, Listener listener, String versionKeyPrefix) {
		this.context = context;
		this.dbHelper = dbHelper;
		this.listener = listener;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.versionKeyPrefix = versionKeyPrefix;
	}

	public Set<String> addItem(SessionObject sessionObject, RosterItem rosterItem) {
		Set<String> addedGroups = null;
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentValues v = new ContentValues();

			v.put(DatabaseContract.RosterItemsCache.FIELD_JID, rosterItem.getJid().toString());
			v.put(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
			v.put(DatabaseContract.RosterItemsCache.FIELD_NAME, rosterItem.getName());
			v.put(DatabaseContract.RosterItemsCache.FIELD_SUBSCRIPTION, rosterItem.getSubscription().name());
			v.put(DatabaseContract.RosterItemsCache.FIELD_ASK, rosterItem.isAsk());
			v.put(DatabaseContract.RosterItemsCache.FIELD_TIMESTAMP, (new Date()).getTime());

			// in most of cases we will already have this record
			if (rosterItem.getId() == -1) {
				long id = db.insertWithOnConflict(DatabaseContract.RosterItemsCache.TABLE_NAME, null, v,
												  SQLiteDatabase.CONFLICT_REPLACE); // CONFLICT_REPLACE?
				Log.d("RosterProvider", "Added item " + rosterItem.getJid().toString() + " with id=" + id);
				rosterItem.setData(RosterItem.ID_KEY, id);
			} else {
				int updated = db.update(DatabaseContract.RosterItemsCache.TABLE_NAME, v,
										DatabaseContract.RosterItemsCache.FIELD_ID + " = ?",
										new String[]{String.valueOf(rosterItem.getId())});
			}
			addedGroups = updateRosterItemGroups(db, rosterItem);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (listener != null) {
			listener.onChange(rosterItem.getId());
		}
		return addedGroups;
	}

	@Override
	public String getCachedVersion(SessionObject sessionObject) {
		return prefs.getString(createKey(sessionObject), "");
	}

	public int getCount(SessionObject sessionObject) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT count(" + DatabaseContract.RosterItemsCache.FIELD_ID + ") FROM " +
									   DatabaseContract.RosterItemsCache.TABLE_NAME + " WHERE " +
									   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ?",
							   new String[]{sessionObject.getUserBareJid().toString()});
		try {
			if (c.moveToNext()) {
				return c.getInt(0);
			}

			// should not enter here
			return 0;
		} finally {
			c.close();
		}
	}

	public Collection<? extends String> getGroups(SessionObject sessionObject) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT DISTINCT g." + DatabaseContract.RosterGroupsCache.FIELD_NAME + " FROM " +
									   DatabaseContract.RosterGroupsCache.TABLE_NAME + " g " + " INNER JOIN " +
									   DatabaseContract.RosterItemsGroups.TABLE_NAME + " gi " + " ON g." +
									   DatabaseContract.RosterGroupsCache.FIELD_ID + " = gi." +
									   DatabaseContract.RosterItemsGroups.FIELD_GROUP + " INNER JOIN " +
									   DatabaseContract.RosterItemsCache.TABLE_NAME + " i " + " ON i." +
									   DatabaseContract.RosterItemsCache.FIELD_ID + " = gi." +
									   DatabaseContract.RosterItemsGroups.FIELD_ITEM + " WHERE i." +
									   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ?",
							   new String[]{sessionObject.getUserBareJid().toString()});
		try {
			List<String> groups = new ArrayList<String>();
			while (c.moveToNext()) {
				groups.add(c.getString(0));
			}
			return groups;
		} finally {
			c.close();
		}
	}

	public RosterItem getItem(SessionObject sessionObject, BareJID jid) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(DatabaseContract.RosterItemsCache.TABLE_NAME,
							new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
										 DatabaseContract.RosterItemsCache.FIELD_NAME,
										 DatabaseContract.RosterItemsCache.FIELD_SUBSCRIPTION,
										 DatabaseContract.RosterItemsCache.FIELD_ASK,
										 DatabaseContract.RosterItemsCache.FIELD_TIMESTAMP},
							DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.RosterItemsCache.FIELD_JID + " = ?",
							new String[]{sessionObject.getUserBareJid().toString(), jid.toString()}, null, null, null);

		// if (c.getCount() == 0) {
		// c.close();
		// log.info("no results in first attempt - trying rawQuery");
		// c = db.rawQuery("SELECT " + DatabaseContract.RosterItemsCache.FIELD_ID +
		// ", " + DatabaseContract.RosterItemsCache.FIELD_NAME + ", " +
		// DatabaseContract.RosterItemsCache.FIELD_SUBSCRIPTION
		// + ", " + DatabaseContract.RosterItemsCache.FIELD_ASK + ", " +
		// DatabaseContract.RosterItemsCache.FIELD_TIMESTAMP + " FROM " +
		// DatabaseContract.RosterItemsCache.TABLE_NAME
		// + " WHERE " + DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ? and
		// " + DatabaseContract.RosterItemsCache.FIELD_JID + " = ?",
		// new String[] { sessionObject.getUserBareJid().toString(),
		// DatabaseUtils.sqlEscapeString(jid.toString())});
		// }
		//
		// if (c.getCount() == 0) {
		// c.close();
		// log.info("no results in first attempt - trying rawQuery by id");
		// long id = (sessionObject.getUserBareJid() + "::" + jid).hashCode();
		// c = db.rawQuery("SELECT " + DatabaseContract.RosterItemsCache.FIELD_ID +
		// ", " + DatabaseContract.RosterItemsCache.FIELD_NAME + ", " +
		// DatabaseContract.RosterItemsCache.FIELD_SUBSCRIPTION
		// + ", " + DatabaseContract.RosterItemsCache.FIELD_ASK + ", " +
		// DatabaseContract.RosterItemsCache.FIELD_TIMESTAMP + " FROM " +
		// DatabaseContract.RosterItemsCache.TABLE_NAME
		// + " WHERE " + DatabaseContract.RosterItemsCache.FIELD_ID + " = ?",
		// new String[] { String.valueOf(id) });
		// }

		try {
			if (c.moveToNext()) {
				RosterItem rosterItem = new RosterItem(jid, sessionObject);
				rosterItem.setName(c.getString(1));
				rosterItem.setSubscription(Subscription.valueOf(c.getString(2)));
				rosterItem.setAsk(c.getInt(3) == 1);
				rosterItem.setData(RosterItem.ID_KEY, c.getLong(0));

				c.close();

				List<String> groups = rosterItem.getGroups();
				c = db.rawQuery("SELECT " + DatabaseContract.RosterGroupsCache.FIELD_ID + ", " +
										DatabaseContract.RosterGroupsCache.FIELD_NAME + " FROM " +
										DatabaseContract.RosterGroupsCache.TABLE_NAME + " g " + " INNER JOIN " +
										DatabaseContract.RosterItemsGroups.TABLE_NAME + " gi " + "ON g." +
										DatabaseContract.RosterGroupsCache.FIELD_ID + " = gi." +
										DatabaseContract.RosterItemsGroups.FIELD_GROUP + " WHERE gi." +
										DatabaseContract.RosterItemsGroups.FIELD_ITEM + " = ?",
								new String[]{String.valueOf(rosterItem.getId())});

				while (c.moveToNext()) {
					groups.add(c.getString(1));
				}

				return rosterItem;
			} else {
				return null;
			}
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}

	}

	public long getRosterItemId(SessionObject sessionObject, BareJID jid) {
		RosterItem ri = getItem(sessionObject, jid);
		return ri == null ? -1 : ri.getId();
	}

	@Override
	public Collection<RosterItem> loadCachedRoster(SessionObject sessionObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeAll(SessionObject sessionObject) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM " + DatabaseContract.RosterItemsGroups.TABLE_NAME + " WHERE " +
							   DatabaseContract.RosterItemsGroups.FIELD_ITEM + " IN (" + "SELECT " +
							   DatabaseContract.RosterItemsCache.FIELD_ID + " FROM " +
							   DatabaseContract.RosterItemsCache.TABLE_NAME + " WHERE " +
							   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ?" + ")",
					   new String[]{sessionObject.getUserBareJid().toString()});
			db.delete(DatabaseContract.RosterItemsCache.TABLE_NAME,
					  DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " = ?",
					  new String[]{sessionObject.getUserBareJid().toString()});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (listener != null) {
			listener.onChange(null);
		}

	}

	public void removeItem(SessionObject sessionObject, RosterItem rosterItem) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(DatabaseContract.RosterItemsGroups.TABLE_NAME,
					  DatabaseContract.RosterItemsGroups.FIELD_ITEM + " = ?",
					  new String[]{String.valueOf(rosterItem.getId())});
			db.delete(DatabaseContract.RosterItemsCache.TABLE_NAME, DatabaseContract.RosterItemsCache.FIELD_ID + " = ?",
					  new String[]{String.valueOf(rosterItem.getId())});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (listener != null) {
			listener.onChange(rosterItem.getId());
		}
	}

	@Override
	public void updateReceivedVersion(SessionObject sessionObject, String ver) {
		prefs.edit().putString(createKey(sessionObject), ver).commit();
	}

	private String createKey(SessionObject sessionObject) {
		return versionKeyPrefix + "." + sessionObject.getUserBareJid();
	}

	private void removeEmptyGroups(SQLiteDatabase db) {
		db.delete(DatabaseContract.RosterGroupsCache.TABLE_NAME,
				  DatabaseContract.RosterGroupsCache.FIELD_ID + " NOT IN (" + "SELECT " +
						  DatabaseContract.RosterItemsGroups.FIELD_GROUP + " FROM " +
						  DatabaseContract.RosterItemsGroups.FIELD_GROUP + ")", new String[0]);
	}

	private Set<String> updateRosterItemGroups(final SQLiteDatabase db, RosterItem rosterItem) {
		Map<String, Long> groupIds = new HashMap<String, Long>();

		// retrieve current groups
		Cursor c = db.rawQuery("SELECT " + DatabaseContract.RosterGroupsCache.FIELD_ID + ", " +
									   DatabaseContract.RosterGroupsCache.FIELD_NAME + " FROM " +
									   DatabaseContract.RosterGroupsCache.TABLE_NAME + " g " + " INNER JOIN " +
									   DatabaseContract.RosterItemsGroups.TABLE_NAME + " gi " + " ON g." +
									   DatabaseContract.RosterGroupsCache.FIELD_ID + " = gi." +
									   DatabaseContract.RosterItemsGroups.FIELD_GROUP + " WHERE gi." +
									   DatabaseContract.RosterItemsGroups.FIELD_ITEM + " = ?",
							   new String[]{String.valueOf(rosterItem.getId())});

		try {
			while (c.moveToNext()) {
				groupIds.put(c.getString(1), c.getLong(0));
			}
			c.close();

			// remove groups from which roster item was removed
			Set<String> toRemove = groupIds.keySet();
			toRemove.removeAll(rosterItem.getGroups());
			if (!toRemove.isEmpty()) {
				StringBuilder groupsToRemove = new StringBuilder();
				groupsToRemove.append("0");
				for (String group : toRemove) {
					groupsToRemove.append(",");
					groupsToRemove.append(groupIds.get(group));
				}

				db.execSQL("DELETE FROM " + DatabaseContract.RosterItemsGroups.TABLE_NAME + " WHERE " +
								   DatabaseContract.RosterItemsGroups.FIELD_ITEM + " =  ? " + " AND " +
								   DatabaseContract.RosterItemsGroups.FIELD_GROUP + " IN (?)",
						   new String[]{String.valueOf(rosterItem.getId()), groupsToRemove.toString()});
			}

			Set<String> addedGroups = new HashSet<String>();
			// add new groups
			if (rosterItem.getGroups() != null) {
				List<String> toAdd = new ArrayList<String>(rosterItem.getGroups());
				toAdd.removeAll(groupIds.keySet());
				if (!toAdd.isEmpty()) {
					StringBuilder toAddAll = new StringBuilder();
					for (String group : toAdd) {
						if (toAddAll.length() > 0) {
							toAddAll.append(",");
						}
						// toAddAll.append("'");
						toAddAll.append(DatabaseUtils.sqlEscapeString(group));
						// toAddAll.append("'");
					}

					// query for ids of existing groups
					c = db.rawQuery("SELECT " + DatabaseContract.RosterGroupsCache.FIELD_ID + ", " +
											DatabaseContract.RosterGroupsCache.FIELD_NAME + " FROM " +
											DatabaseContract.RosterGroupsCache.TABLE_NAME + " g WHERE g.name IN (" +
											toAddAll.toString() + ")", null);
					List<Long> toAddIds = new ArrayList<Long>();
					while (c.moveToNext()) {
						toAddIds.add(c.getLong(0));
						toAdd.remove(c.getString(1));
						addedGroups.add(c.getString(1));
					}

					// create non existing groups
					for (String group : toAdd) {
						ContentValues v = new ContentValues();
						v.put(DatabaseContract.RosterGroupsCache.FIELD_NAME, group);
						long id = db.insert(DatabaseContract.RosterGroupsCache.TABLE_NAME, null, v);
						toAddIds.add(id);
						addedGroups.add(group);
					}

					// create relations to added groups
					for (long id : toAddIds) {
						ContentValues v = new ContentValues();
						v.put(DatabaseContract.RosterItemsGroups.FIELD_ITEM, rosterItem.getId());
						v.put(DatabaseContract.RosterItemsGroups.FIELD_GROUP, id);
						db.insert(DatabaseContract.RosterItemsGroups.TABLE_NAME, null, v);
					}
				}
			}
			return addedGroups;
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
	}

	public interface Listener {

		void onChange(Long rosterItemId);
	}
}
