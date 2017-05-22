package org.tigase.messenger.phone.pro.service;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

import java.util.Date;

public class MessageSender
		extends BroadcastReceiver {

	public static final String SEND_CHAT_MESSAGE = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_CHAT_MESSAGE";
	public static final String SEND_GROUPCHAT_MESSAGE = "org.tigase.messenger.phone.pro.service.MessageSender.SEND_GROUPCHAT_MESSAGE";
	private final static String TAG = "MessageSender";
	private final XMPPService service;

	public MessageSender(XMPPService xmppService) {
		this.service = xmppService;
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

		for (Chat chat : jaxmpp.getModule(MessageModule.class).getChatManager().getChats()) {
			if (chat.getId() == chatId) {
				return chat;
			}
		}

		return null;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		switch (intent.getAction()) {
			case SEND_CHAT_MESSAGE:
				sendChatMessage(context, intent);
				break;
			case SEND_GROUPCHAT_MESSAGE:
				sendGroupchatMessage(context, intent);
				break;
			default:
				Log.wtf(TAG, "Unknown action: " + intent.getAction());
				throw new RuntimeException("Unknown action: " + intent.getAction());
		}
	}

	private void sendChatMessage(final Context context, Intent intent) {
		final int chatId = intent.getIntExtra("chatId", 0);
		final String body = intent.getStringExtra("body");
		final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra("account"));

		final Chat chat = getChat(account, chatId);
		final Jaxmpp jaxmpp = service.getJaxmpp(chat.getSessionObject().getUserBareJid());

		int state;
		Message msg;
		String stanzaId = null;
		try {
			msg = chat.createMessage(body);
			stanzaId = msg.getId();

			MessageModule m = jaxmpp.getModule(MessageModule.class);

			if (jaxmpp.isConnected()) {
				m.sendMessage(msg);
				state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
			} else {
				state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
			}
		} catch (Exception e) {
			state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
			Log.w("ChatItemFragment", "Cannot send message", e);
		}

		final JID recipient = chat.getJid();

		ContentValues values = new ContentValues();
		values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_JID, recipient.getBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, new Date().getTime());
		values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
		values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
		values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
		values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
		if (stanzaId != null) {
			values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);
		}

		Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + recipient.getBareJid());
		context.getContentResolver().insert(uri, values);
	}

	private void sendGroupchatMessage(final Context context, final Intent intent) {
		final String body = intent.getStringExtra("body");
		final BareJID account = BareJID.bareJIDInstance(intent.getStringExtra("account"));
		final BareJID roomJID = BareJID.bareJIDInstance(intent.getStringExtra("roomJID"));
		final Jaxmpp jaxmpp = service.getJaxmpp(account);
		Room room = jaxmpp.getModule(MucModule.class).getRoom(roomJID);

		int state;
		Message msg;
		String stanzaId = null;
		try {
			msg = room.createMessage(body);

			stanzaId = msg.getId();
			if (jaxmpp.isConnected() && room.getState() == Room.State.joined) {
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
		values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
				   DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE);

		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, room.getSessionObject().getUserBareJid().toString());

		Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + room.getSessionObject().getUserBareJid() + "/" +
									Uri.encode(room.getRoomJid().toString()));
		Uri x = context.getContentResolver().insert(uri, values);
		if (x != null) {
			context.getContentResolver().notifyChange(x, null);
		}
	}

}
