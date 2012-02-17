package org.tigase.mobile.db.providers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.tigase.mobile.db.CapsFeaturesTableMetaData;
import org.tigase.mobile.db.CapsIdentitiesTableMetaData;

import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CapabilitiesDBCache implements CapabilitiesCache {

	private final Context context;

	private final MessengerDatabaseHelper helper;

	public CapabilitiesDBCache(Context context) {
		this.context = context;
		this.helper = new MessengerDatabaseHelper(this.context);
	}

	@Override
	public Set<String> getFeatures(String node) {
		final Set<String> result = new HashSet<String>();
		SQLiteDatabase db = this.helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM " + CapsFeaturesTableMetaData.TABLE_NAME + " WHERE "
				+ CapsFeaturesTableMetaData.FIELD_NODE + "=?", new String[] { node });
		try {
			while (c.moveToNext()) {
				String f = c.getString(c.getColumnIndex(CapsFeaturesTableMetaData.FIELD_FEATURE));
				result.add(f);
			}
		} finally {
			c.close();
		}

		return result;
	}

	@Override
	public Identity getIdentity(String node) {
		SQLiteDatabase db = this.helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM " + CapsIdentitiesTableMetaData.TABLE_NAME + " WHERE "
				+ CapsIdentitiesTableMetaData.FIELD_NODE + "=?", new String[] { node });
		try {
			if (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex(CapsIdentitiesTableMetaData.FIELD_NAME));
				String category = c.getString(c.getColumnIndex(CapsIdentitiesTableMetaData.FIELD_CATEGORY));
				String type = c.getString(c.getColumnIndex(CapsIdentitiesTableMetaData.FIELD_TYPE));

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
	public boolean isCached(String node) {
		SQLiteDatabase db = this.helper.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + CapsIdentitiesTableMetaData.TABLE_NAME + " WHERE "
				+ CapsIdentitiesTableMetaData.FIELD_NODE + "=?", new String[] { node });
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
				v.put(CapsFeaturesTableMetaData.FIELD_NODE, node);
				v.put(CapsFeaturesTableMetaData.FIELD_FEATURE, string);
				db.insert(CapsFeaturesTableMetaData.TABLE_NAME, null, v);
			}

			ContentValues v = new ContentValues();
			v.put(CapsIdentitiesTableMetaData.FIELD_NODE, node);
			v.put(CapsIdentitiesTableMetaData.FIELD_NAME, name);
			v.put(CapsIdentitiesTableMetaData.FIELD_CATEGORY, category);
			v.put(CapsIdentitiesTableMetaData.FIELD_TYPE, type);
			db.insert(CapsIdentitiesTableMetaData.TABLE_NAME, null, v);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

}
