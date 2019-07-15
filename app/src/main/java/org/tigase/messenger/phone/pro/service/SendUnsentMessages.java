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

package org.tigase.messenger.phone.pro.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.utils.Parser;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OMEMOEncryptableMessage;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OMEMOMessage;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SendUnsentMessages
		implements Runnable {

	public static final String TAG = "SendUnsentChat";
	public static Executor executor = Executors.newFixedThreadPool(2);
	private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
											   DatabaseContract.ChatHistory.FIELD_ACCOUNT,
											   DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
											   DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
											   DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
											   DatabaseContract.ChatHistory.FIELD_BODY,
											   DatabaseContract.ChatHistory.FIELD_DATA,
											   DatabaseContract.ChatHistory.FIELD_JID,
											   DatabaseContract.ChatHistory.FIELD_STATE,
											   DatabaseContract.ChatHistory.FIELD_THREAD_ID,
											   DatabaseContract.ChatHistory.FIELD_STANZA_ID,
											   DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
											   DatabaseContract.ChatHistory.FIELD_ENCRYPTION,
											   DatabaseContract.ChatHistory.FIELD_TIMESTAMP};
	private final Context context;
	private final Jaxmpp jaxmpp;
	private final SessionObject sessionObject;

	static void addOOB(final int messageType, String oobData, Message msg, ContentValues values)
			throws JaxmppException {
		if (oobData != null) {
			Element url = Parser.parseElement(oobData);
			Element x = ElementBuilder.create("x", "jabber:x:oob").getElement();
			x.addChild(url);
			msg.addChild(x);

			values.put(DatabaseContract.ChatHistory.FIELD_DATA, url.getAsString());
		}
	}

	public SendUnsentMessages(Context context, Jaxmpp jaxmpp) {
		this.context = context;
		this.jaxmpp = jaxmpp;
		this.sessionObject = jaxmpp.getSessionObject();
	}

	public void run(final Uri itemUri) {
		try (Cursor c = context.getContentResolver().query(itemUri, cols, null, null, null)) {
			while (c.moveToNext()) {
				send(c);
			}
		}
	}

	@Override
	public void run() {
		Uri u = Uri.parse(ChatProvider.UNSENT_MESSAGES_URI + "/" + sessionObject.getUserBareJid());
		try (Cursor c = context.getContentResolver()
				.query(u, cols, DatabaseContract.ChatHistory.FIELD_CHAT_TYPE + "=?",
					   new String[]{"" + DatabaseContract.ChatHistory.CHAT_TYPE_P2P},
					   DatabaseContract.ChatHistory.FIELD_TIMESTAMP)) {
			while (c.moveToNext()) {
				send(c);
			}
		}
	}

	private void send(final Cursor c) {
		if (!jaxmpp.isConnected()) {
			return;
		}
		final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
		final int encryption = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ENCRYPTION));
		final JID toJid = JID.jidInstance(c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID)));
		final String threadId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_THREAD_ID));
		final String body = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
		final String stanzaId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STANZA_ID));
		final String oobData = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_DATA));
		final int messageType = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE));
		final String localContent = c.getString(
				c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));

		try {
			final ContentValues values = new ContentValues();
			final OMEMOEncryptableMessage msg = new OMEMOEncryptableMessage(Message.create());
			msg.setEncryption(encryption == 1
							  ? OMEMOEncryptableMessage.Encryption.Required
							  : OMEMOEncryptableMessage.Encryption.Disabled);
			msg.setTo(toJid);
			msg.setType(StanzaType.chat);
			msg.setThread(threadId);
			msg.setBody(body);
			msg.setId(stanzaId);

			addOOB(messageType, oobData, msg, values);

			if (messageType == DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE && localContent != null &&
					oobData == null) {
				FileUploaderTask uploader = new FileUploaderTask(context, jaxmpp, Uri.parse(localContent)) {
					@Override
					protected void sendMessage(String getUri, String mimeType) {
						try {
							addOOB(messageType, "<url>" + getUri + "</url>", msg, values);
							String bd = getUri + (body == null ? "" : "\n" + body);
							values.put(DatabaseContract.ChatHistory.FIELD_BODY, bd);
							msg.setBody(bd);
							send(id, msg, values);
						} catch (Exception e) {
							Log.w(TAG, "Babollo", e);
							e.printStackTrace();
						}
					}
				};
				uploader.executeOnExecutor(executor);
			} else {
				send(id, msg, values);
			}

		} catch (Exception e) {
			Log.w(TAG, "Cannot send unsent message", e);
		}
	}

	private void send(final int id, final Message msgToSend, final ContentValues values) throws JaxmppException {
		final MessageModule messageModule = jaxmpp.getModule(MessageModule.class);

		Message sentMsg = messageModule.sendMessage(msgToSend);
		int encryptionStatus = (sentMsg instanceof OMEMOMessage && ((OMEMOMessage) sentMsg).isSecured()) ? 1 : 0;

		values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_OUT_SENT);
		values.put(DatabaseContract.ChatHistory.FIELD_ENCRYPTION, encryptionStatus);
		context.getContentResolver()
				.update(Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" +
										  sentMsg.getTo().getBareJid() + "/" + id), values, null, null);
	}

}
