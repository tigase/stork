package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class ChatFragment extends Fragment {

	public static ChatFragment newInstance(Chat chat, SimpleCursorAdapter ad, MessengerDatabaseHelper dbHelper) {
		ChatFragment f = new ChatFragment();
		f.chat = chat;
		f.adapter = ad;
		f.dbHelper = dbHelper;
		return f;
	}

	private SimpleCursorAdapter adapter;
	private Chat chat;

	private MessengerDatabaseHelper dbHelper;
	private ChatView layout;
	private final Listener<PresenceEvent> presenceListener;

	public ChatFragment() {
		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Received presence " + be.getJid() + " :: " + be.getPresence());
				if (chat != null && chat.getJid().getBareJid().equals(be.getJid().getBareJid()))
					updatePresence();
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();
		layout.setChat(chat);
		layout.setDbHelper(dbHelper);
		layout.setCursorAdapter(adapter);

		return layout;
	}

	@Override
	public void onStart() {
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
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactAvailable, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactUnavailable, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStop();
	}

	protected void updatePresence() {
		CPresence cp = MessengerDatabaseHelper.getShowOf(chat.getJid().getBareJid());
		System.out.println("Updating presence to " + cp);
		layout.setImagePresence(cp);
	}

}
