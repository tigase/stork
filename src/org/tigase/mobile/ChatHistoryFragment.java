package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.db.providers.ChatHistoryProvider;

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

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public static Fragment newInstance(long chatId) {
		ChatHistoryFragment f = new ChatHistoryFragment();

		Bundle args = new Bundle();
		args.putLong("chatId", chatId);
		f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating ChatFragment id=" + chatId);

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
				if (DEBUG)
					Log.d(TAG, "Received presence " + be.getJid() + " :: " + be.getPresence());
				if (ChatHistoryFragment.this.chat != null
						&& ChatHistoryFragment.this.chat.getJid().getBareJid().equals(be.getJid().getBareJid()))
					updatePresence();
			}
		};
	}

	public Chat getChat() {
		return chat;
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

		if (DEBUG)
			Log.d(TAG, "onActivityCreated ChatFragment " + savedInstanceState);
		if (DEBUG)
			Log.d(TAG, "Arguments: " + getArguments());
		if (DEBUG)
			Log.d(TAG, "Activity: " + getActivity());

		// if (savedInstanceState != null) {
		// long ci = savedInstanceState.getLong("chatId", -1);
		// if (ci != -1) {
		// setChatId(ci);
		// }
		// }

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
				if (DEBUG)
					Log.i(TAG, "Changed!");
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
			if (DEBUG)
				Log.d(TAG, "Closing cursor");
			c.close();
		}
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();

		this.layout.setImagePresence((new RosterDisplayTools(getActivity())).getShowOf(this.chat.getJid().getBareJid()));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (DEBUG)
			Log.d(TAG, "Save state of ChatFragment");
		if (outState != null)
			outState.putLong("chatId", chat.getId());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (DEBUG)
			Log.d(TAG, "Start ChatFragment");
		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactAvailable, this.presenceListener);
		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactUnavailable, this.presenceListener);
		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStart();

		updatePresence();
	}

	@Override
	public void onStop() {
		if (DEBUG)
			Log.d(TAG, "Stop ChatFragment");

		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactAvailable, this.presenceListener);
		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactUnavailable, this.presenceListener);
		XmppService.jaxmpp(getActivity()).getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStop();
	}

	private void setChatId(final long chatId) {
		List<Chat> l = XmppService.jaxmpp(getActivity()).getModulesManager().getModule(MessageModule.class).getChats();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getId() == chatId) {
				this.chat = c;
				if (DEBUG)
					Log.d(TAG, "Found chat with " + c.getJid() + " (id=" + chatId + ")");

				return;
			}
		}
		throw new RuntimeException("Chat not found!");
	}

	protected void updatePresence() {
		CPresence cp = (new RosterDisplayTools(getActivity())).getShowOf(chat.getJid().getBareJid());
		System.out.println("Updating presence to " + cp);
		// layout.setImagePresence(cp);
	}

}
