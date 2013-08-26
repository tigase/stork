/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.db.providers;

import java.util.Date;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.OpenMUCTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.AbstractRoomsManager;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBMUCManager extends AbstractRoomsManager {

	private static final boolean DEBUG = false;

	private static final String TAG = "DBMUCManager";

	private final Context context;

	private final MessengerDatabaseHelper helper;

	public DBMUCManager(Context context) {
		this.context = context;
		this.helper = new MessengerDatabaseHelper(this.context);
	}

	@Override
	protected Room createRoomInstance(final BareJID jid, final String nickname, final String password) {
		SQLiteDatabase db = helper.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(OpenMUCTableMetaData.FIELD_ROOM_JID, jid.toString());
		values.put(OpenMUCTableMetaData.FIELD_NICKNAME, nickname);
		values.put(OpenMUCTableMetaData.FIELD_PASSWORD, password);
		values.put(OpenMUCTableMetaData.FIELD_TIMESTAMP, (new Date()).getTime());
		values.put(OpenMUCTableMetaData.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

		long rowId = db.insert(OpenMUCTableMetaData.TABLE_NAME, null, values);

		Room room = new Room(rowId, packetWriter, jid, nickname, sessionObject);
		room.setPassword(password);
		room.setObservable(observable);

		long lastMsgTmstmp = 0;
		String sql1 = "SELECT MAX(" + ChatTableMetaData.FIELD_TIMESTAMP + ") FROM " + ChatTableMetaData.TABLE_NAME + " WHERE "
				+ ChatTableMetaData.FIELD_JID + "='" + jid.toString() + "'";

		final Cursor c1 = db.rawQuery(sql1, null);
		try {
			if (c1.moveToNext()) {
				lastMsgTmstmp = c1.getLong(0);
			}
		} finally {
			c1.close();
		}

		if (lastMsgTmstmp != 0)
			room.setLastMessageDate(new Date(lastMsgTmstmp));

		return room;
	}

	@Override
	protected void initialize() {
		super.initialize();
		rooms.clear();
		SQLiteDatabase db = this.helper.getReadableDatabase();
		String sql = "SELECT " + OpenMUCTableMetaData.FIELD_ID + ", " + OpenMUCTableMetaData.FIELD_ROOM_JID + ", "
				+ OpenMUCTableMetaData.FIELD_NICKNAME + ", " + OpenMUCTableMetaData.FIELD_PASSWORD + ", "
				+ OpenMUCTableMetaData.FIELD_TIMESTAMP + " FROM " + OpenMUCTableMetaData.TABLE_NAME + " WHERE "
				+ OpenMUCTableMetaData.FIELD_ACCOUNT + "='" + sessionObject.getUserBareJid() + "'";
		final Cursor c = db.rawQuery(sql, null);
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(c.getColumnIndex(OpenMUCTableMetaData.FIELD_ID));

				final BareJID roomJID = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(OpenMUCTableMetaData.FIELD_ROOM_JID)));
				final String nickname = c.getString(c.getColumnIndex(OpenMUCTableMetaData.FIELD_NICKNAME));
				final String password = c.getString(c.getColumnIndex(OpenMUCTableMetaData.FIELD_PASSWORD));
				final long timestamp = c.getLong(c.getColumnIndex(OpenMUCTableMetaData.FIELD_TIMESTAMP));

				long lastMsgTmstmp = 0;
				String sql1 = "SELECT MAX(" + ChatTableMetaData.FIELD_TIMESTAMP + ") FROM " + ChatTableMetaData.TABLE_NAME
						+ " WHERE " + ChatTableMetaData.FIELD_JID + "='" + roomJID.toString() + "'";

				final Cursor c1 = db.rawQuery(sql1, null);
				try {
					if (c1.moveToNext()) {
						lastMsgTmstmp = c1.getLong(0);
					}
				} finally {
					c1.close();
				}

				Room room = new Room(id, packetWriter, roomJID, nickname, sessionObject);
				room.setPassword(password);
				room.setObservable(observable);
				if (lastMsgTmstmp != 0)
					room.setLastMessageDate(new Date(lastMsgTmstmp));

				rooms.put(room.getRoomJid(), room);
			}

		} finally {
			c.close();
		}
	}

	@Override
	public boolean remove(final Room room) {
		boolean x = super.remove(room);
		if (x) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete(OpenMUCTableMetaData.TABLE_NAME, OpenMUCTableMetaData.FIELD_ID + "=" + room.getId(), null);
		}
		return x;
	}

}
