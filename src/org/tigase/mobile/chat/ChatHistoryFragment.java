package org.tigase.mobile.chat;

import java.util.Date;
import java.util.List;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class ChatHistoryFragment extends Fragment {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public static Fragment newInstance(String account, long chatId, int pageIndex) {
		ChatHistoryFragment f = new ChatHistoryFragment();

		Bundle args = new Bundle();
		args.putLong("chatId", chatId);
		args.putString("account", account);
		args.putInt("page", pageIndex);
		f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating ChatFragment id=" + chatId);

		return f;
	}

	private Cursor c;

	private Chat chat;

	private Listener<MessageEvent> chatUpdateListener;

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
		this.chatUpdateListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				layout.updateClientIndicator();
			}
		};
	}

	private void copyMessageBody(final long id) {
		ClipboardManager clipMan = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		Cursor cc = null;
		try {
			cc = getChatEntry(id);
			String t = cc.getString(cc.getColumnIndex(ChatTableMetaData.FIELD_BODY));
			clipMan.setText(t);
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}

	}

	public Chat getChat() {
		return chat;
	}

	private Cursor getChatEntry(long id) {
		Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid() + "/" + id), null, null, null, null);
		cursor.moveToNext();
		return cursor;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.detailsMessage: {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			showMessageDetails(info.id);
			return true;
		}
		case R.id.copyMessage: {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			copyMessageBody(info.id);
			return true;
		}
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			int idx = getArguments().getInt("page");
			Chat ch = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().getChats().get(idx);

			setChatId(ch.getSessionObject().getUserBareJid(), ch.getId());

			// setChatId(BareJID.bareJIDInstance(getArguments().getString("account")),
			// getArguments().getLong("chatId"));
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater m = new MenuInflater(getActivity());
		m.inflate(R.menu.chat_context_menu, menu);
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
		registerForContextMenu(lv);
		// lv.setOnItemLongClickListener(new OnItemLongClickListener() {
		//
		// @Override
		// public boolean onItemLongClick(AdapterView<?> parent, View view, int
		// position, long id) {
		// Men
		//
		// return true;
		// }
		// });

		ChatAdapter ad = new ChatAdapter(inflater.getContext(), R.layout.chat_item, c);
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

		updatePresence();
		layout.updateClientIndicator();
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
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.addListener(PresenceModule.ContactAvailable, this.presenceListener);
		jaxmpp.addListener(PresenceModule.ContactUnavailable, this.presenceListener);
		jaxmpp.addListener(PresenceModule.ContactChangedPresence, this.presenceListener);

		jaxmpp.addListener(MessageModule.ChatUpdated, this.chatUpdateListener);

		super.onStart();

		updatePresence();
		layout.updateClientIndicator();
	}

	@Override
	public void onStop() {
		if (DEBUG)
			Log.d(TAG, "Stop ChatFragment");
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.removeListener(MessageModule.ChatUpdated, this.chatUpdateListener);

		jaxmpp.removeListener(PresenceModule.ContactAvailable, this.presenceListener);
		jaxmpp.removeListener(PresenceModule.ContactUnavailable, this.presenceListener);
		jaxmpp.removeListener(PresenceModule.ContactChangedPresence, this.presenceListener);
		super.onStop();
	}

	private void setChatId(final BareJID account, final long chatId) {
		MultiJaxmpp multi = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		List<Chat> l = multi.getChats();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getId() == chatId) {
				this.chat = c;
				if (DEBUG)
					Log.d(TAG, "Found chat with " + c.getJid() + " (id=" + chatId + ")");

				return;
			}
		}

		String ids = "";
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			ids += c.getId() + " ";
		}
		throw new RuntimeException("Chat (id:" + chatId + ", account:" + account + ")  not found! Available ids=" + ids);
	}

	private void showMessageDetails(final long id) {
		Cursor cc = null;
		final java.text.DateFormat df = DateFormat.getDateFormat(getActivity());
		final java.text.DateFormat tf = DateFormat.getTimeFormat(getActivity());

		try {
			cc = getChatEntry(id);

			Dialog alertDialog = new Dialog(getActivity());
			alertDialog.setContentView(R.layout.chat_item_details_dialog);
			alertDialog.setCancelable(true);
			alertDialog.setCanceledOnTouchOutside(true);
			alertDialog.setTitle("Message details");

			TextView msgDetSender = (TextView) alertDialog.findViewById(R.id.msgDetSender);
			msgDetSender.setText(cc.getString(cc.getColumnIndex(ChatTableMetaData.FIELD_JID)));

			Date timestamp = new Date(cc.getLong(cc.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
			TextView msgDetReceived = (TextView) alertDialog.findViewById(R.id.msgDetReceived);
			msgDetReceived.setText(df.format(timestamp) + " " + tf.format(timestamp));

			final int state = cc.getInt(cc.getColumnIndex(ChatTableMetaData.FIELD_STATE));
			TextView msgDetState = (TextView) alertDialog.findViewById(R.id.msgDetState);
			switch (state) {
			case ChatTableMetaData.STATE_INCOMING:
				msgDetState.setText("Received");
				break;
			case ChatTableMetaData.STATE_OUT_SENT:
				msgDetState.setText("Sent");
				break;
			case ChatTableMetaData.STATE_OUT_NOT_SENT:
				msgDetState.setText("Not sent");
				break;
			default:
				msgDetState.setText("?");
				break;
			}

			alertDialog.show();
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}
	}

	protected void updatePresence() {
		if (chat != null) {

			CPresence cp = (new RosterDisplayTools(getActivity())).getShowOf(chat.getSessionObject(),
					chat.getJid().getBareJid());
			System.out.println("Updating presence to " + cp);
			// ((MessengerApplication)getActivity().getApplication()).getMultiJaxmpp().get(chat.getSessionObject());
			layout.setImagePresence(cp);
		}
	}

}
