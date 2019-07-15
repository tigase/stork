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
package org.tigase.messenger.jaxmpp.android.chat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

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
		try {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.beginTransaction();
			int deleted = 0;
			try {
				Cursor openChatCursor = db.query(DatabaseContract.OpenChats.TABLE_NAME,
												 new String[]{DatabaseContract.OpenChats.FIELD_ID,
															  DatabaseContract.OpenChats.FIELD_ACCOUNT,
															  DatabaseContract.OpenChats.FIELD_JID},
												 DatabaseContract.OpenChats.FIELD_ID + "=?",
												 new String[]{String.valueOf(chatId)}, null, null, null);

				String account = null;
				String jid = null;
				if (openChatCursor.moveToNext()) {
					account = openChatCursor.getString(
							openChatCursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
					jid = openChatCursor.getString(openChatCursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
				}
				openChatCursor.close();
				Log.i("ChatProvider", "Room history for " + account + ", " + jid);

				deleted = db.delete(DatabaseContract.OpenChats.TABLE_NAME, DatabaseContract.OpenChats.FIELD_ID + " = ?",
									new String[]{String.valueOf(chatId)});
				Log.i("ChatProvider", "Removed " + deleted + " open conversations (" + chatId + ")");

				if (account != null && jid != null) {
					ContentValues values = new ContentValues();
					values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_INCOMING);

					int r = db.update(DatabaseContract.ChatHistory.TABLE_NAME, values,
									  DatabaseContract.ChatHistory.FIELD_ACCOUNT + "=? AND " +
											  DatabaseContract.ChatHistory.FIELD_JID + "=? AND " +
											  DatabaseContract.ChatHistory.FIELD_STATE + "=?",
									  new String[]{account, jid,
												   String.valueOf(DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD)});

					Log.i("ChatProvider", "Marked  " + r + " messages as read ");
				}

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			if (listener != null) {
				listener.onChange(chatId);
			}
			return deleted > 0;
		} catch (Exception e) {
			Log.e("OpenChat", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public long createChat(SessionObject sessionObject, JID fromJid, String threadId) throws JaxmppException {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_JID, fromJid.getBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_TIMESTAMP, (new Date()).getTime());
		values.put(DatabaseContract.OpenChats.FIELD_TYPE, DatabaseContract.OpenChats.TYPE_CHAT);
		values.put(DatabaseContract.OpenChats.FIELD_ENCRYPTION, 0);

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
		values.put(DatabaseContract.OpenChats.FIELD_ENCRYPTION, 0);

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
	 *
	 * @return Cursor
	 */
	public Cursor getChat(SessionObject sessionObject, JID jid, String threadId) {
		String[] columns = new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_JID,
										DatabaseContract.OpenChats.FIELD_THREAD_ID,
										DatabaseContract.OpenChats.FIELD_RESOURCE};

		final SQLiteDatabase db = dbHelper.getReadableDatabase();
//		if (threadId != null) {
//			Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, columns,
//								DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
//										DatabaseContract.OpenChats.FIELD_JID + " = ? and " +
//										DatabaseContract.OpenChats.FIELD_TYPE + " = " +
//										DatabaseContract.OpenChats.TYPE_CHAT + " and " +
//										DatabaseContract.OpenChats.FIELD_THREAD_ID + " = ?",
//								new String[]{sessionObject.getUserBareJid().toString(), jid.getBareJid().toString(),
//											 threadId}, null, null, null, null);
//			return c;
//		}
//		if (jid.getResource() != null) {
//			Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, columns,
//								DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
//										DatabaseContract.OpenChats.FIELD_JID + " = ? and " +
//										DatabaseContract.OpenChats.FIELD_TYPE + " = " +
//										DatabaseContract.OpenChats.TYPE_CHAT + " and " +
//										DatabaseContract.OpenChats.FIELD_RESOURCE + " = ?",
//								new String[]{sessionObject.getUserBareJid().toString(), jid.getBareJid().toString(),
//											 jid.getResource()}, null, null, null, null);
//			return c;
//		}
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, columns,
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_JID + " = ? and " +
									DatabaseContract.OpenChats.FIELD_TYPE + " = " +
									DatabaseContract.OpenChats.TYPE_CHAT,
							new String[]{sessionObject.getUserBareJid().toString(), jid.getBareJid().toString()}, null,
							null, null, null);
		return c;
	}

	public Cursor getChat(final SessionObject sessionObject, final int chatId) {
		String[] columns = new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_JID,
										DatabaseContract.OpenChats.FIELD_THREAD_ID,
										DatabaseContract.OpenChats.FIELD_RESOURCE};
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, columns,
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_ID + " = ?",
							new String[]{sessionObject.getUserBareJid().toString(), String.valueOf(chatId)}, null, null,
							null, null);
		return c;
	}

	/**
	 * Get parameters needed to create proper Chat instances from DB
	 *
	 * @param sessionObject
	 *
	 * @return Cursor
	 */
	public Cursor getChats(SessionObject sessionObject) {
		String[] columns = new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_JID,
										DatabaseContract.OpenChats.FIELD_THREAD_ID,
										DatabaseContract.OpenChats.FIELD_RESOURCE};
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, columns,
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_TYPE + " = " +
									DatabaseContract.OpenChats.TYPE_CHAT,
							new String[]{sessionObject.getUserBareJid().toString()}, null, null, null, null);
		return c;
	}

	public List<Room> getRooms(SessionObject sessionObject, tigase.jaxmpp.core.client.Context context) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final List<Room> rooms = new ArrayList<Room>();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME,
							new String[]{DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_JID,
										 DatabaseContract.OpenChats.FIELD_NICKNAME,
										 DatabaseContract.OpenChats.FIELD_PASSWORD},
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_MUC,
							new String[]{sessionObject.getUserBareJid().toString()}, null, null, null, null);
		try {
			while (c.moveToNext()) {
				Cursor lastMsgCursor = db.query(DatabaseContract.ChatHistory.TABLE_NAME,
												new String[]{DatabaseContract.ChatHistory.FIELD_ID,
															 DatabaseContract.ChatHistory.FIELD_TIMESTAMP},
												DatabaseContract.ChatHistory.FIELD_ACCOUNT + "=? AND " +
														DatabaseContract.ChatHistory.FIELD_JID + "=?",
												new String[]{sessionObject.getUserBareJid().toString(), c.getString(1)},
												null, null, DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC ",
												"1");

				Log.d("ChatProvider", "Last messages =" + lastMsgCursor.getCount() + "; ac=" +
						sessionObject.getUserBareJid().toString() + "; jid=" + c.getString(1));

				Long timestamp = null;
				try {
					if (lastMsgCursor.moveToNext()) {
						timestamp = lastMsgCursor.getLong(
								lastMsgCursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
					}
				} finally {
					lastMsgCursor.close();
				}

				if (timestamp != null) {
					Log.d("ChatProvider", " timestamp=" + (new Date(timestamp)));
				}

				long id = c.getLong(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
				BareJID roomJid = BareJID.bareJIDInstance(
						c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID)));
				String nickname = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_NICKNAME));
				String password = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_PASSWORD));

				Room room = new Room(id, context, roomJid, nickname);
				room.setPassword(password);
				if (timestamp != null) {
					room.setLastMessageDate(new Date(timestamp - 1000 * 30));
				}
				rooms.add(room);
			}
		} finally {
			c.close();
		}
		return rooms;
	}

	public boolean isChatOpenFor(SessionObject sessionObject, BareJID jid) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID},
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_TYPE + " = " +
									DatabaseContract.OpenChats.TYPE_CHAT + " and " +
									DatabaseContract.OpenChats.FIELD_JID + " = ?",
							new String[]{sessionObject.getUserBareJid().toString(), jid.toString()}, null, null, null,
							null);
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

		db.update(DatabaseContract.OpenChats.TABLE_NAME, values,
				  DatabaseContract.OpenChats.FIELD_TYPE + " = " + DatabaseContract.OpenChats.TYPE_MUC, null);
		if (listener != null) {
			listener.onChange(null);
		}
	}

	public void updateChat(final Chat chat) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.OpenChats.FIELD_JID, chat.getJid().getBareJid().toString());
		values.put(DatabaseContract.OpenChats.FIELD_THREAD_ID, chat.getThreadId());
		values.put(DatabaseContract.OpenChats.FIELD_RESOURCE, chat.getJid().getResource());

		long result = db.update(DatabaseContract.OpenChats.TABLE_NAME, values,
								DatabaseContract.OpenChats.FIELD_ID + " = ?",
								new String[]{String.valueOf(chat.getId())});

	}

	public void updateRoomState(SessionObject sessionObject, BareJID room, int state) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ID},
							DatabaseContract.OpenChats.FIELD_ACCOUNT + " = ? and " +
									DatabaseContract.OpenChats.FIELD_TYPE + " = " +
									DatabaseContract.OpenChats.TYPE_MUC + " and " +
									DatabaseContract.OpenChats.FIELD_JID + "=?",
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

		long result = db.update(DatabaseContract.OpenChats.TABLE_NAME, values,
								DatabaseContract.OpenChats.FIELD_ID + " = ?", new String[]{String.valueOf(id)});
		if (listener != null && result != 0) {
			listener.onChange(id);
		}
	}

	public interface Listener {

		void onChange(Long chatId);
	}
}
