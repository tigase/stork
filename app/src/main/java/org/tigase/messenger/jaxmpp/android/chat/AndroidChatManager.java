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

import android.database.Cursor;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;

import java.util.ArrayList;
import java.util.List;

public class AndroidChatManager
		extends AbstractChatManager {

	private final static String TAG = "AChatManager";
	private ChatProvider provider;

	public AndroidChatManager(ChatProvider provider) {
		this.provider = provider;
	}

	@Override
	public boolean close(Chat chat) throws JaxmppException {
		boolean closed = provider.close(context.getSessionObject(), chat.getId());
		if (closed) {
			MessageModule.ChatClosedHandler.ChatClosedEvent event = new MessageModule.ChatClosedHandler.ChatClosedEvent(
					context.getSessionObject(), chat);
			context.getEventBus().fire(event);
		}
		return closed;
	}

	@Override
	public Chat createChat(JID fromJid, String threadId) throws JaxmppException {
		Log.d(TAG, "Create chat object and database entry JID=" + fromJid);
		SessionObject sessionObject = context.getSessionObject();
		long id = provider.createChat(sessionObject, fromJid, threadId);
		Chat chat = new AndroidChat(id, context);
		chat.setJid(fromJid);
		chat.setThreadId(threadId);

		MessageModule.ChatCreatedHandler.ChatCreatedEvent event = new MessageModule.ChatCreatedHandler.ChatCreatedEvent(
				context.getSessionObject(), chat, null);

		context.getEventBus().fire(event);

		return chat;
	}

	@Override
	public Chat getChat(JID jid, String threadId) {
		Log.d(TAG, "Searching chat jid=" + jid + " threadId=" + threadId);
		try (Cursor c = provider.getChat(context.getSessionObject(), jid, threadId)) {
			if (c.moveToNext()) {
				return createChat(c);
			}
		}
		return null;
	}

	public Chat getChat(final int chatId) {
		Log.d(TAG, "Searching chat id=" + chatId);
		try (Cursor c = provider.getChat(context.getSessionObject(), chatId)) {
			if (c.moveToNext()) {
				return createChat(c);
			}
		}
		return null;
	}

	@Override
	public List<Chat> getChats() {
		List<Chat> chats = new ArrayList<>();
		try (Cursor c = provider.getChats(context.getSessionObject())) {
			while (c.moveToNext()) {
				chats.add(createChat(c));
			}
		}
		return chats;
	}

	@Override
	public boolean isChatOpenFor(BareJID jid) {
		return provider.isChatOpenFor(context.getSessionObject(), jid);
	}

	private Chat createChat(final Cursor c) {
		BareJID jid = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID)));
		Log.d(TAG, "Create chat object from cursor. jid=" + jid);
		final long id = c.getLong(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
		Chat chat = new AndroidChat(id, context);
		chat.setThreadId(c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_THREAD_ID)));
		final String resource = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_RESOURCE));

		if (resource != null && !resource.isEmpty()) {
			chat.setJid(JID.jidInstance(jid, resource));
		} else {
			chat.setJid(JID.jidInstance(jid));
		}
		return chat;
	}

}
