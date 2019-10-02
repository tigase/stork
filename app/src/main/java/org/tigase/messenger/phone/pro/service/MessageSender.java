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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import org.tigase.messenger.jaxmpp.android.chat.AndroidChatManager;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OMEMOEncryptableMessage;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OMEMOMessage;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;

public class MessageSender {

	public static final String SEND_CHAT_MESSAGE_ACTION = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_CHAT_MESSAGE";
	public static final String SEND_GROUPCHAT_MESSAGE_ACTION = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_GROUPCHAT_MESSAGE";
	public final static String CHAT_ID = "CHAT_ID";
	public final static String BODY = "BODY";
	public final static String ACCOUNT = "ACCOUNT";
	/**
	 * Local content to send URI.
	 */
	public final static String LOCAL_CONTENT_URI = "LOCAL_CONTENT_URI";
	public final static String ENCRYPT_MESSAGE = "ENCRYPT_MESSAGE";
	public final static String ROOM_JID = "ROOM_JID";
	private final static String TAG = "MessageSender";
	private final XMPPService service;

	private static Uri copyLocalImageToAlbum(Context context, Uri localContentUri) throws IOException {
		final ContentValues values = new ContentValues();

		Bitmap bmp = getBitmapFromUri(context, localContentUri);
		String imu = MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, "Sent image", "");

		return Uri.parse(imu);
	}

	public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
		ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
		FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
		Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		parcelFileDescriptor.close();
		return image;
	}

	public static String getMimeType(final Context context, final Uri localContentUri) {
		String displayName = null;
		try (Cursor cursor = context.getContentResolver().query(localContentUri, null, null, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

			}
		}
		return FileUploaderTask.guessMimeType(displayName);
	}

	public MessageSender(XMPPService xmppService) {
		this.service = xmppService;
	}

	public void process(Context context, Intent intent) {
		switch (intent.getAction()) {
			case SEND_CHAT_MESSAGE_ACTION:
				sendChatMessage(context, intent);
				break;
			case SEND_GROUPCHAT_MESSAGE_ACTION:
				sendGroupchatMessage(context, intent);
				break;
			default:
				Log.wtf(TAG, "Unknown action: " + intent.getAction());
				throw new RuntimeException("Unknown action: " + intent.getAction());
		}
	}

	private Chat getChat(final BareJID account, int chatId) {
		if (service == null) {
			Log.w("ChatItemFragment", "Service is not binded");
			return null;
		}

		Jaxmpp jaxmpp = service.getJaxmpp(account);

		if (jaxmpp == null) {
			Log.w("ChatItemFragment", "There is no account " + account);
			return null;
		}

		AbstractChatManager chatManager = jaxmpp.getModule(MessageModule.class).getChatManager();

		if (chatManager instanceof AndroidChatManager) {
			return ((AndroidChatManager) chatManager).getChat(chatId);
		} else {
			for (Chat chat : chatManager.getChats()) {
				if (chat.getId() == chatId) {
					return chat;
				}
			}
		}

		return null;
	}

	private void sendChatMessage(final Context context, Intent intent) {
		final int chatId = intent.getIntExtra(CHAT_ID, 0);
		final String body = intent.getStringExtra(BODY);
		final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra(ACCOUNT));
		final Uri localContentUri = intent.getParcelableExtra(LOCAL_CONTENT_URI);
		final boolean encryption = intent.getBooleanExtra(ENCRYPT_MESSAGE, false);

//		if (localContentUri != null) {
//			context.getContentResolver()
//					.takePersistableUriPermission(localContentUri, intent.getFlags() &
//							(Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
//		}

		final Chat chat = getChat(account, chatId);

		int state;
		int encryptionStatus;
		Message msg;
		String stanzaId = null;
		final ContentValues values = new ContentValues();
		int itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
		Jaxmpp jaxmpp = null;
		try {
			jaxmpp = service.getJaxmpp(chat.getSessionObject().getUserBareJid());
			msg = chat.createMessage(body);

			stanzaId = msg.getId();
			MessageModule m = jaxmpp.getModule(MessageModule.class);

			if (localContentUri != null) {
				state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
				encryptionStatus = encryption ? 1 : 0;
				final String mimeType = getMimeType(context, localContentUri);
				if (mimeType.startsWith("image/")) {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE;
				} else if (mimeType.startsWith("video/")) {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO;
				} else {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_FILE;
				}
			} else if (jaxmpp.isConnected()) {
					msg = new OMEMOEncryptableMessage(msg);
				((OMEMOEncryptableMessage)msg).setEncryption(encryption? OMEMOEncryptableMessage.Encryption.Required: OMEMOEncryptableMessage.Encryption.Disabled);
				msg = m.sendMessage(msg);
				state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
				itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
				encryptionStatus = (msg instanceof OMEMOMessage && ((OMEMOMessage) msg).isSecured()) ? 1 : 0;
			} else {
				state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
				itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
				encryptionStatus = encryption ? 1 : 0;
			}
		} catch (Exception e) {
			state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
			encryptionStatus = encryption ? 1 : 0;
			Log.w("ChatItemFragment", "Cannot send message", e);
		}

		final JID recipient = chat.getJid();

		values.put(DatabaseContract.ChatHistory.FIELD_ENCRYPTION, encryptionStatus);
		values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE, DatabaseContract.ChatHistory.CHAT_TYPE_P2P);
		values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_JID, recipient.getBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, new Date().getTime());
		values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
		values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
		values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, itemType);
		if (stanzaId != null) {
			values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);
		}

		if (localContentUri != null) {
			try {
				Uri imageUri = copyLocalImageToAlbum(context, localContentUri);
				values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, imageUri.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + recipient.getBareJid());
		Uri u = context.getContentResolver().insert(uri, values);

		if (u != null) {
			context.getContentResolver().notifyChange(u, null);
		}

		if (localContentUri != null && jaxmpp != null) {
			SendUnsentMessages sum = new SendUnsentMessages(context, jaxmpp);
			sum.run(u);
		}
	}

	private void sendGroupchatMessage(final Context context, final Intent intent) {
		final String body = intent.getStringExtra(BODY);
		final String url = intent.getStringExtra("oob:url");
		final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra(ACCOUNT));
		final BareJID roomJID = BareJID.bareJIDInstance(intent.getStringExtra(ROOM_JID));
		final Jaxmpp jaxmpp = service.getJaxmpp(account);
		final Uri localContentUri = intent.getParcelableExtra(LOCAL_CONTENT_URI);
		Room room = jaxmpp.getModule(MucModule.class).getRoom(roomJID);

		int state;
		Message msg;
		String stanzaId = null;
		int itemType = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;

		try {
			msg = room.createMessage(body);
			stanzaId = msg.getId();

			if (url != null) {
				Element x = ElementBuilder.create("x", "jabber:x:oob").child("url").setValue(url).getElement();
				msg.addChild(x);
			}

			if (localContentUri != null) {
				state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
				final String mimeType = getMimeType(context, localContentUri);
				if (mimeType.startsWith("image/")) {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE;
				} else if (mimeType.startsWith("video/")) {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO;
				} else {
					itemType = DatabaseContract.ChatHistory.ITEM_TYPE_FILE;
				}
			} else if (jaxmpp.isConnected() && room.getState() == Room.State.joined) {
				room.sendMessage(msg);
				state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
			} else {
				state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
			}
		} catch (Exception e) {
			state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
			Log.w("MucItemFragment", "Cannot send message", e);
		}

		ContentValues values = new ContentValues();
		values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, room.getNickname());
		values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, System.currentTimeMillis());
		values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

		values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
		values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);

		values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, itemType);
		values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE, DatabaseContract.ChatHistory.CHAT_TYPE_MUC);

		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, room.getSessionObject().getUserBareJid().toString());

		if (localContentUri != null) {
			try {
				Uri imageUri = copyLocalImageToAlbum(context, localContentUri);
				values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, imageUri.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + room.getSessionObject().getUserBareJid() + "/" +
									Uri.encode(room.getRoomJid().toString()));
		Uri x = context.getContentResolver().insert(uri, values);
		if (x != null) {
			context.getContentResolver().notifyChange(x, null);
		}

		if (localContentUri != null) {
			SendUnsentGroupMessages sum = new SendUnsentGroupMessages(context, jaxmpp, room);
			sum.run(x);
		}
	}

}
