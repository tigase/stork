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

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;

import java.util.ArrayList;
import java.util.List;

public class AndroidChatManager extends AbstractChatManager {

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
		SessionObject sessionObject = context.getSessionObject();
		long id = provider.createChat(sessionObject, fromJid, threadId);
		Chat chat = new Chat(id, context);
		chat.setJid(fromJid);
		chat.setThreadId(threadId);

		MessageModule.ChatCreatedHandler.ChatCreatedEvent event = new MessageModule.ChatCreatedHandler.ChatCreatedEvent(
				context.getSessionObject(), chat, null);

		context.getEventBus().fire(event);

		return chat;
	}

	@Override
	public Chat getChat(JID jid, String threadId) {
		Object[] data = provider.getChat(context.getSessionObject(), jid, threadId);
		if (data == null)
			return null;
		Chat chat = new Chat((Long) data[0], context);
		chat.setThreadId((String) data[1]);
		if (data[2] != null) {
			chat.setJid(JID.jidInstance(jid.getBareJid(), (String) data[2]));
		} else {
			chat.setJid(jid);
		}
		return chat;
	}

	@Override
	public List<Chat> getChats() {
		List<Object[]> datas = provider.getChats(context.getSessionObject());
		List<Chat> chats = new ArrayList<Chat>(datas.size());
		for (Object[] data : datas) {
			Chat chat = new Chat((Long) data[0], context);
			chat.setJid(JID.jidInstance((BareJID) data[1], (String) data[3]));
			chat.setThreadId((String) data[2]);
			chats.add(chat);
		}
		return chats;
	}

	@Override
	public boolean isChatOpenFor(BareJID jid) {
		return provider.isChatOpenFor(context.getSessionObject(), jid);
	}

}
