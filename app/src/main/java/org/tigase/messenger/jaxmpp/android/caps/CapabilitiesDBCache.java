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
package org.tigase.messenger.jaxmpp.android.caps;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.Identity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CapabilitiesDBCache
		implements CapabilitiesCache {

	private final SQLiteOpenHelper helper;

	public CapabilitiesDBCache(SQLiteOpenHelper helper) {
		this.helper = helper;
	}

	@Override
	public Set<String> getFeatures(String node) {
		final Set<String> result = new HashSet<String>();
		if (node != null) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			Cursor c = db.rawQuery("SELECT * FROM " + DatabaseContract.CapsFeatures.TABLE_NAME + " WHERE " +
										   DatabaseContract.CapsFeatures.FIELD_NODE + "=?", new String[]{node});
			try {
				while (c.moveToNext()) {
					String f = c.getString(c.getColumnIndex(DatabaseContract.CapsFeatures.FIELD_FEATURE));
					result.add(f);
				}
			} finally {
				c.close();
			}
		}

		return result;
	}

	@Override
	public Identity getIdentity(String node) {
		if (node == null) {
			return null;
		}

		SQLiteDatabase db = this.helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM " + DatabaseContract.CapsIdentities.TABLE_NAME + " WHERE " +
									   DatabaseContract.CapsIdentities.FIELD_NODE + "=?", new String[]{node});
		try {
			if (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex(DatabaseContract.CapsIdentities.FIELD_NAME));
				String category = c.getString(c.getColumnIndex(DatabaseContract.CapsIdentities.FIELD_CATEGORY));
				String type = c.getString(c.getColumnIndex(DatabaseContract.CapsIdentities.FIELD_TYPE));

				Identity n = new Identity();
				n.setCategory(category);
				n.setName(name);
				n.setType(type);

				return n;
			}
		} finally {
			c.close();
		}
		return null;
	}

	@Override
	public Set<String> getNodesWithFeature(String feature) {
		final Set<String> result = new HashSet<String>();
		if (feature != null) {
			SQLiteDatabase db = this.helper.getReadableDatabase();
			Cursor c = db.rawQuery("SELECT distinct(" + DatabaseContract.CapsFeatures.FIELD_NODE + ") FROM " +
										   DatabaseContract.CapsFeatures.TABLE_NAME + " WHERE " +
										   DatabaseContract.CapsFeatures.FIELD_FEATURE + "=?", new String[]{feature});
			try {
				while (c.moveToNext()) {
					String f = c.getString(0);
					result.add(f);
				}
			} finally {
				c.close();
			}
		}

		return result;
	}

	@Override
	public boolean isCached(String node) {
		if (node == null) {
			// if node is null then no point in retrieving features as node does not exist
			// so we can say values are cached
			return true;
		}

		SQLiteDatabase db = this.helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CapsIdentities.TABLE_NAME + " WHERE " +
									   DatabaseContract.CapsIdentities.FIELD_NODE + "=?", new String[]{node});
		try {
			if (c.moveToNext()) {
				int r = c.getInt(0);
				return r > 0;
			}
		} finally {
			c.close();
		}
		return false;
	}

	@Override
	public void store(String node, String name, String category, String type, Collection<String> features) {
		SQLiteDatabase db = this.helper.getWritableDatabase();
		try {
			db.beginTransaction();

			for (String string : features) {
				ContentValues v = new ContentValues();
				v.put(DatabaseContract.CapsFeatures.FIELD_NODE, node);
				v.put(DatabaseContract.CapsFeatures.FIELD_FEATURE, string);
				db.insert(DatabaseContract.CapsFeatures.TABLE_NAME, null, v);
			}

			ContentValues v = new ContentValues();
			v.put(DatabaseContract.CapsIdentities.FIELD_NODE, node);
			v.put(DatabaseContract.CapsIdentities.FIELD_NAME, name);
			v.put(DatabaseContract.CapsIdentities.FIELD_CATEGORY, category);
			v.put(DatabaseContract.CapsIdentities.FIELD_TYPE, type);
			db.insert(DatabaseContract.CapsIdentities.TABLE_NAME, null, v);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

}

