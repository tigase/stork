/*
 * ChatItemFragment.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.chat;

import java.util.Date;

import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the
 * {@link OnListFragmentInteractionListener} interface.
 */
public class ChatItemFragment extends Fragment {

	@Bind(R.id.chat_list)
	RecyclerView recyclerView;
	@Bind(R.id.messageText)
	EditText message;
	@Bind(R.id.send_button)
	ImageView sendButton;
	private Chat chat;
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			BareJID account = ((ChatActivity) getContext()).getAccount();
			Jaxmpp jaxmpp = getService().getJaxmpp(account);
			final long chatId = ((ChatActivity) getContext()).getOpenChatId();

			if (jaxmpp == null) {

			}

			for (Chat chat : jaxmpp.getModule(MessageModule.class).getChatManager().getChats()) {
				if (chat.getId() == chatId) {
					setChat(chat);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			setChat(null);
			super.onServiceDisconnected(name);
		}
	};
	private OnListFragmentInteractionListener mListener;
	private MyChatItemRecyclerViewAdapter adapter;
	private Uri uri;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ChatItemFragment() {
	}

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static ChatItemFragment newInstance(int columnCount) {
		ChatItemFragment fragment = new ChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		this.uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + ((ChatActivity) getContext()).getAccount() + "/"
				+ ((ChatActivity) getContext()).getJid());

		if (context instanceof OnListFragmentInteractionListener) {
			mListener = (OnListFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener");
		}

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setHasOptionsMenu(true);

		// if (getArguments() != null) {
		// mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
		// }
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO
		inflater.inflate(R.menu.openchat_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);
		ButterKnife.bind(this, root);

		message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.send || id == EditorInfo.IME_NULL) {
					send();
					return true;
				}
				return false;
			}
		});

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});

		// recyclerView.addItemDecoration(new
		// DividerItemDecoration(getActivity(),
		// DividerItemDecoration.VERTICAL_LIST));

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MyChatItemRecyclerViewAdapter(getContext(), null, mListener) {
			@Override
			protected void onContentChanged() {
				refreshChatHistory();
			}
		};
		recyclerView.setAdapter(adapter);

		refreshChatHistory();
		return root;
	}

	@Override
	public void onDetach() {
		mListener = null;
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	private void refreshChatHistory() {
		(new DBUpdateTask()).execute();
	}

	private void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty())
			return;

		this.message.getText().clear();
		(new SendMessageTask()).execute(body);
	}

	private void setChat(Chat chat) {
		this.chat = chat;
	}

	public interface OnListFragmentInteractionListener {
		// TODO: Update argument type and name
		void onListFragmentInteraction();
	}

	private class SendMessageTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			for (String param : params) {
				int state;
				String stanzaId = null;

				try {
					MessageModule m = mConnection.getService().getJaxmpp(chat.getSessionObject().getUserBareJid()).getModule(
							MessageModule.class);
					Message msg = m.sendMessage(chat, param);
					if (msg != null)
						stanzaId = msg.getId();
					state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
				} catch (Exception e) {
					state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					Log.w("ChatItemFragment", "Cannot send message", e);
				}

				final JID recipient = chat.getJid();

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_JID, recipient.getBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, new Date().getTime());
				values.put(DatabaseContract.ChatHistory.FIELD_BODY, param);
				values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
				if (stanzaId != null)
					values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

				Uri u = getContext().getContentResolver().insert(uri, values);
				getContext().getContentResolver().notifyChange(u, null);

			}

			return null;
		}
	}

	private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[] { DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_TIMESTAMP };

		@Override
		protected Cursor doInBackground(Void... params) {
			if (getContext() == null)
				return null;
			Cursor cursor = getContext().getContentResolver().query(uri, cols, null, null,
					DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC");

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
			recyclerView.smoothScrollToPosition(0);
		}
	}
}
