/*
 * Tigase XMPP Client Library
 * Copyright (C) 2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package org.tigase.messenger.jaxmpp.android.chat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatProvider {

	private final Context context;
	private final SQLiteOpenHelper dbHelper;
	private final Listener listener;

	public ChatProvider(Context context, SQLiteOpenHelper dbHelper, Listener listener) {
		this.context = context;
		this.dbHelper = dbHelper;
		this.listener = listener;
	}

	public boolean close(SessionObject sessionObject, long chatId) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		int deleted = 0;
		try {
			deleted = db.delete(DatabaseContract.OpenChats.TABLE_NAME, DatabaseContract.OpenChats.FIELD_ID + " = ?",
					new String[]{String.valueOf(chatId)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (listener != null) {
			listener.onChange(chatId);
		}
		return deleted > 0;
	}

	public long createChat(SessionObject sessionObject, JID fromJid, String threadId) throws JaxmppException {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_JID, fromJid.getBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_TIMESTAMP, (new Date()).getTime());
		values.put(DatabaseContract.OpenChats.FIELD_TYPE, DatabaseContract.OpenChats.TYPE_CHAT);

		if (fromJid.getResource() != null) {
			values.put(DatabaseContract.OpenChats.FIELD_RESOURCE, fromJid.getResource());
		}
		if (threadId != null) {
			values.put(DatabaseContract.OpenChats.FIELD_THREAD_ID, threadId);
		}

		long result = db.insert(DatabaseContract.OpenChats.TABLE_NAME, null, values);
		if (listener != null) {
			listener.onChange(result);
		}
		return result;
	}

	public long createMuc(SessionObject sessionObject, JID fromJid, String nickname, String password) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_JID, fromJid.getBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_TIMESTAMP, (new Date()).getTime());
		values.put(DatabaseContract.OpenChats.FIELD_TYPE, DatabaseContract.OpenChats.TYPE_MUC);

		if (nickname != null) {
			values.put(DatabaseContract.OpenChats.FIELD_NICKNAME, nickname);
		}
		if (password != null) {
			values.put(DatabaseContract.OpenChats.FIELD_PASSWORD, password);
		}

		long result = db.insert(DatabaseContract.OpenChats.TABLE_NAME, null, values);
		if (listener != null) {
			listener.onChange(result);
		}
		return result;
	}

	/**
	 * Get parameters needed to create proper Chat instance from DB
	 *
	 * @param sessionObject
	 * @param jid
	 * @param threadId
	 * @return Array of objects { Long id, String threadId, String resourceId }
	 */
	public Object[] getChat(SessionObject sessionObject, JID jid, String threadId) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		if (threadId != null) {
			Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_RESOURCE},
					DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " + DatabaseContract.OpenChats.FIELD_JID + " = ? and " + DatabaseContract.OpenChats.FIELD_TYPE
							+ " = " + DatabaseContract.OpenChats.TYPE_CHAT + " and " + DatabaseContract.OpenChats.FIELD_THREAD_ID + " = ?",
					new String[]{
							sessionObject.getUserBareJid().toString(),
							jid.getBareJid().toString(),
							threadId
					}, null, null, null, null);
			try {
				if (c.moveToNext()) {
					return new Object[]{c.getLong(0), threadId, c.getString(1)};
				}
			} finally {
				c.close();
			}
		}
		if (jid.getResource() != null) {
			Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_THREAD_ID},
					DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " + DatabaseContract.OpenChats.FIELD_JID + " = ? and " + DatabaseContract.OpenChats.FIELD_TYPE
							+ " = " + DatabaseContract.OpenChats.TYPE_CHAT + " and " + DatabaseContract.OpenChats.FIELD_RESOURCE + " = ?",
					new String[]{
							sessionObject.getUserBareJid().toString(),
							jid.getBareJid().toString(),
							jid.getResource()
					}, null, null, null, null);
			try {
				if (c.moveToNext()) {
					return new Object[]{c.getLong(0), c.getString(1), jid.getResource()};
				}
			} finally {
				c.close();
			}
		}
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_THREAD_ID,
						DatabaseContract.OpenChats.FIELD_RESOURCE}, DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " + DatabaseContract.OpenChats.FIELD_JID
						+ " = ? and " + DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_CHAT,
				new String[]{
						sessionObject.getUserBareJid().toString(),
						jid.getBareJid().toString()
				}, null, null, null, null);
		try {
			if (c.moveToNext()) {
				return new Object[]{c.getLong(0), c.getString(1), c.getString(2)};
			}
		} finally {
			c.close();
		}
		return null;
	}

	/**
	 * Get parameters needed to create proper Chat instances from DB
	 *
	 * @param sessionObject
	 * @return List of arrays of objects { Long id, BareJID jid, String threadId, String resourceId }
	 */
	public List<Object[]> getChats(SessionObject sessionObject) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		List<Object[]> chats = new ArrayList<Object[]>();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_JID,
						DatabaseContract.OpenChats.FIELD_THREAD_ID, DatabaseContract.OpenChats.FIELD_RESOURCE}, DatabaseContract.OpenChats.FIELD_ACCOUNT +
						" = ? and " + DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_CHAT,
				new String[]{
						sessionObject.getUserBareJid().toString()
				}, null, null, null, null);
		try {
			while (c.moveToNext()) {
				chats.add(new Object[]{c.getLong(0), BareJID.bareJIDInstance(c.getString(1)), c.getString(2), c.getString(3)});
			}
		} finally {
			c.close();
		}
		return chats;
	}

	public List<Object[]> getRooms(SessionObject sessionObject) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		List<Object[]> rooms = new ArrayList<Object[]>();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME,
				new String[]{
						DatabaseContract.OpenChats.FIELD_ID,
						DatabaseContract.OpenChats.FIELD_JID,
						DatabaseContract.OpenChats.FIELD_NICKNAME,
						DatabaseContract.OpenChats.FIELD_PASSWORD
				}, DatabaseContract.OpenChats.FIELD_ACCOUNT +
						" = ? and " + DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_MUC,
				new String[]{
						sessionObject.getUserBareJid().toString()
				}, null, null, null, null);
		try {
			while (c.moveToNext()) {
				rooms.add(new Object[]{c.getLong(0), BareJID.bareJIDInstance(c.getString(1)), c.getString(2), c.getString(3)});
			}
		} finally {
			c.close();
		}
		return rooms;
	}

	public boolean isChatOpenFor(SessionObject sessionObject, BareJID jid) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID}, DatabaseContract.OpenChats.FIELD_ACCOUNT +
						" = ? and " + DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_CHAT + " and " + DatabaseContract.OpenChats.FIELD_JID + " = ?",
				new String[]{
						sessionObject.getUserBareJid().toString(),
						jid.toString()
				}, null, null, null, null);
		try {
			if (c.moveToNext()) {
				return true;
			}
		} finally {
			c.close();
		}
		return false;
	}

	public void resetRoomState(int state) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_ROOM_STATE, state);

		db.update(DatabaseContract.OpenChats.TABLE_NAME, values, DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_MUC, null);
		if (listener != null) {
			listener.onChange(null);
		}
	}

	public void updateRoomState(SessionObject sessionObject, BareJID room, int state) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID}, DatabaseContract.OpenChats.FIELD_ACCOUNT +
						" = ? and " + DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_MUC + " and " + DatabaseContract.OpenChats.FIELD_JID + "=?",
				new String[]{sessionObject.getUserBareJid().toString(), room.toString()}, null, null, null);

		long id = 0;
		try {
			if (c.moveToNext()) {
				id = c.getLong(0);
			}
		} catch (Exception ex) {
		}

		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_ROOM_STATE, state);

		long result = db.update(DatabaseContract.OpenChats.TABLE_NAME, values, DatabaseContract.OpenChats.FIELD_ID + " = ?", new String[]{String.valueOf(id)});
		if (listener != null && result != 0) {
			listener.onChange(id);
		}
	}

	public interface Listener {
		void onChange(Long chatId);
	}
}
