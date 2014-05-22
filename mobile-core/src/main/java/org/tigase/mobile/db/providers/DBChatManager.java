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

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.db.OpenChatsTableMetaData;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBChatManager extends AbstractChatManager {

	private static long chatIds = 1;

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public static boolean isReceiptAvailable(JaxmppCore jaxmpp, JID j) {
		return true;
		// try {
		// CapabilitiesCache capsCache =
		// jaxmpp.getModule(CapabilitiesModule.class).getCache();
		// Presence p = jaxmpp.getPresence().getPresence(j);
		// if (p != null) {
		// Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
		// if (c == null) {
		// return false;
		// }
		//
		// final String node = c.getAttribute("node");
		// final String ver = c.getAttribute("ver");
		//
		// Set<String> features = capsCache.getFeatures(node + "#" + ver);
		//
		// return features.contains(MessageModule.RECEIPTS_XMLNS);
		// }
		//
		// return false;
		// } catch (Exception ex) {
		// return false;
		// }
	}

	private final Context context;

	private final MessengerDatabaseHelper helper;

	public DBChatManager(Context context) {
		this.context = context;
		this.helper = new MessengerDatabaseHelper(this.context);
	}

	@Override
	public boolean close(Chat chat) throws JaxmppException {
		boolean x = super.close(chat);
		if (x) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete(OpenChatsTableMetaData.TABLE_NAME, OpenChatsTableMetaData.FIELD_ID + "=" + chat.getId(), null);
		}
		return x;
	}

	@Override
	protected Chat createChatInstance(JID jid, String threadId) {
		SQLiteDatabase db = helper.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(OpenChatsTableMetaData.FIELD_JID, jid.getBareJid().toString());
		if (jid.getResource() != null)
			values.put(OpenChatsTableMetaData.FIELD_RESOURCE, jid.getResource());
		if (threadId != null)
			values.put(OpenChatsTableMetaData.FIELD_THREAD_ID, threadId);
		values.put(OpenChatsTableMetaData.FIELD_TIMESTAMP, (new Date()).getTime());
		values.put(OpenChatsTableMetaData.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

		long rowId = db.insert(OpenChatsTableMetaData.TABLE_NAME, null, values);

		Chat chat = new Chat(rowId, packetWriter, sessionObject);
		chat.setJid(jid);
		chat.setThreadId(threadId);

		chat.setMessageDeliveryReceiptsEnabled(isReceiptAvailable(jid));

		return chat;
	}

	@Override
	protected void initialize() {
		super.initialize();
		chats.clear();
		SQLiteDatabase db = this.helper.getReadableDatabase();
		String sql = "SELECT " + OpenChatsTableMetaData.FIELD_ID + ", " + OpenChatsTableMetaData.FIELD_JID + ", "
				+ OpenChatsTableMetaData.FIELD_RESOURCE + ", " + OpenChatsTableMetaData.FIELD_THREAD_ID + ", "
				+ OpenChatsTableMetaData.FIELD_TIMESTAMP + " FROM " + OpenChatsTableMetaData.TABLE_NAME + " WHERE "
				+ OpenChatsTableMetaData.FIELD_ACCOUNT + "='" + sessionObject.getUserBareJid() + "'";
		final Cursor c = db.rawQuery(sql, null);
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(c.getColumnIndex(OpenChatsTableMetaData.FIELD_ID));

				final JID sJid = JID.jidInstance(c.getString(c.getColumnIndex(OpenChatsTableMetaData.FIELD_JID)));
				final String sRes = c.getString(c.getColumnIndex(OpenChatsTableMetaData.FIELD_RESOURCE));
				final JID jid = sRes == null ? sJid : JID.jidInstance(sJid.getBareJid(), sRes);
				final String threadId = c.getString(c.getColumnIndex(OpenChatsTableMetaData.FIELD_THREAD_ID));
				final long timestamp = c.getLong(c.getColumnIndex(OpenChatsTableMetaData.FIELD_TIMESTAMP));

				Chat chat = new Chat(id, packetWriter, sessionObject);
				chat.setJid(jid);
				chat.setThreadId(threadId);

				chat.setMessageDeliveryReceiptsEnabled(isReceiptAvailable(jid));

				chats.add(chat);

			}

		} finally {
			c.close();
		}
	}

	protected boolean isReceiptAvailable(JID j) {
		final JaxmppCore jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(sessionObject);
		return isReceiptAvailable(jaxmpp, j);
	}

	@Override
	protected boolean update(Chat chat, JID fromJid, String threadId) throws JaxmppException {
		chat.setMessageDeliveryReceiptsEnabled(isReceiptAvailable(fromJid));
		boolean x = super.update(chat, fromJid, threadId);
		if (x) {

			SQLiteDatabase db = helper.getWritableDatabase();
			final ContentValues values = new ContentValues();
			JID jid = chat.getJid();
			values.put(OpenChatsTableMetaData.FIELD_JID, jid.getBareJid().toString());
			if (jid.getResource() != null)
				values.put(OpenChatsTableMetaData.FIELD_RESOURCE, jid.getResource());
			if (threadId != null)
				values.put(OpenChatsTableMetaData.FIELD_THREAD_ID, threadId);

			db.update(OpenChatsTableMetaData.TABLE_NAME, values, OpenChatsTableMetaData.FIELD_ID + "=" + chat.getId(), null);
		}
		return x;
	}

}
