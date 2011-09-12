package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ChatHistoryFragment extends Fragment {

	public static Fragment newInstance(long chatId) {
		ChatHistoryFragment f = new ChatHistoryFragment();

		Bundle args = new Bundle();
		args.putLong("chatId", chatId);
		f.setArguments(args);

		return f;
	}

	private Cursor c;

	private Chat chat;

	private ChatView layout;

	private final Listener<PresenceEvent> presenceListener;

	public ChatHistoryFragment() {
		super();
		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Received presence " + be.getJid() + " :: " + be.getPresence());
				if (ChatHistoryFragment.this.chat != null
						&& ChatHistoryFragment.this.chat.getJid().getBareJid().equals(be.getJid().getBareJid()))
					updatePresence();
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			setChatId(getArguments().getLong("chatId"));
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();
		layout.setChat(chat);

		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "onActivityCreated ChatFragment " + savedInstanceState);
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Arguments: " + getArguments());
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Activity: " + getActivity());

		if (savedInstanceState != null) {
			long ci = savedInstanceState.getLong("chatId", -1);
			if (ci != -1) {
				setChatId(ci);
			}
		}

		if (chat == null) {
			throw new RuntimeException("Chat not specified!");
		}

		this.c = getActivity().getApplicationContext().getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid()), null, null, null, null);

		final ListView lv = (ListView) layout.findViewById(R.id.chat_conversation_history);

		ChatAdapter ad = new ChatAdapter(getActivity().getApplicationContext(), R.layout.chat_item, c);
		lv.setAdapter(ad);
		ad.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Changed!");
				lv.post(new Runnable() {

					@Override
					public void run() {
						lv.setSelection(Integer.MAX_VALUE);
					}
				});
			}
		});
		return layout;
	}

	@Override
	public void onDestroyView() {
		if (c != null) {
			Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Closing cursor");
			c.close();
		}
		super.onDestroyView();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Save state of ChatFragment");
		if (outState != null)
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
		CPresence cp = RosterProvider.getShowOf(chat.getJid().getBareJid());
		System.out.println("Updating presence to " + cp);
		// layout.setImagePresence(cp);
	}

}
