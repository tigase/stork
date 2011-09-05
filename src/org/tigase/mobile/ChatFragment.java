package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.db.MessengerDatabaseHelper;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChatFragment extends Fragment {

	public static ChatFragment newInstance(long chatId) {
		ChatFragment f = new ChatFragment();
		f.setChatId(chatId);

		return f;
	}

	private Cursor c;

	private Chat chat;

	private ChatView layout;
	private final Listener<PresenceEvent> presenceListener;

	public ChatFragment() {
		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Received presence " + be.getJid() + " :: " + be.getPresence());
				if (ChatFragment.this.chat != null
						&& ChatFragment.this.chat.getJid().getBareJid().equals(be.getJid().getBareJid()))
					updatePresence();
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "onCreateView ChatFragment");

		if (savedInstanceState != null) {
			long ci = savedInstanceState.getLong("chatId", -1);
			if (ci != -1) {
				setChatId(ci);
			}
		}

		if (chat == null) {
			throw new RuntimeException("Chat not specified!");
		}

		this.c = inflater.getContext().getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid()), null, null, null, null);

		// XXX inflater.get startManagingCursor(c);

		ChatAdapter ad = new ChatAdapter(inflater.getContext(), R.layout.chat_item, c);

		MessengerDatabaseHelper db = new MessengerDatabaseHelper(inflater.getContext());
		db.open();

		this.layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();
		layout.setChat(chat);
		layout.setDbHelper(db);
		layout.setCursorAdapter(ad);

		return layout;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Save state of ChatFragment");

		outState.putLong("chatId", chat.getId());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Start ChatFragment");
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactAvailable,
				this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactUnavailable,
				this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStart();

		updatePresence();
	}

	@Override
	public void onStop() {
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Stop ChatFragment");

		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactAvailable, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactUnavailable, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStop();
	}

	private void setChatId(final long chatId) {
		List<Chat> l = XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChats();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getId() == chatId) {
				this.chat = c;
				return;
			}
		}
		throw new RuntimeException("Chat not found!");
	}

	protected void updatePresence() {
		CPresence cp = MessengerDatabaseHelper.getShowOf(chat.getJid().getBareJid());
		System.out.println("Updating presence to " + cp);
		layout.setImagePresence(cp);
	}

}
