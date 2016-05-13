/*
 * OpenChatItemFragment.java
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

package org.tigase.messenger.phone.pro.openchats;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.conversations.muc.MucActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the
 * {@link OnListFragmentInteractionListener} interface.
 */
public class OpenChatItemFragment extends Fragment {

	private final ContentObserver contactPresenceChangeObserver = new ContentObserver(new Handler()) {

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			refreshChatlist();
		}
	};
	@Bind(R.id.openchats_list)
	RecyclerView recyclerView;
	private OnAddChatListener mAddChatListener;
	private MyOpenChatItemRecyclerViewAdapter adapter;
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection();
	private OnListFragmentInteractionListener mListener = new OnListFragmentInteractionListener() {
		@Override
		public void onArchiveChat(int chatId, String jid, String account) {
			OpenChatItemFragment.this.onArchiveChat(chatId, jid, account);
		}

		@Override
		public void onEnterToChat(final int type, final int openChatId, final String jid, final String account) {
			Intent intent;
			switch (type) {
				case DatabaseContract.OpenChats.TYPE_CHAT:
					intent = new Intent(OpenChatItemFragment.this.getContext(), ChatActivity.class);
					break;
				case DatabaseContract.OpenChats.TYPE_MUC:
					intent = new Intent(OpenChatItemFragment.this.getContext(), MucActivity.class);
					break;
				default:
					throw new RuntimeException("Unrecognized open_chat type = " + type);
			}
			intent.putExtra("openChatId", openChatId);
			intent.putExtra("jid", jid);
			intent.putExtra("account", account);

			startActivity(intent);
		}

		@Override
		public void onLeaveMucRoom(int chatId, String roomJID, String account) {
			OpenChatItemFragment.this.onLeaveRoom(chatId, roomJID, account);

		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public OpenChatItemFragment() {
	}

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static OpenChatItemFragment newInstance(MainActivity.XMPPServiceConnection mServiceConnection) {
		OpenChatItemFragment fragment = new OpenChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@OnClick(R.id.roster_add_chat)
	void onAddContactClick() {

		mAddChatListener.onAddChatClick();
	}

	private void onArchiveChat(final int chatId, String jid, final String account) {
		XMPPService service = mConnection.getService();

		if (service == null) {
			Log.w("OpenChatItemFragment", "Service is not binded");
			return;
		}

		final Jaxmpp jaxmpp = service.getJaxmpp(account);

		if (jaxmpp == null) {
			Log.w("OpenChatItemFragment", "There is no account " + account);
			return;
		}

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Chat chat = null;
				for (Chat c : jaxmpp.getModule(MessageModule.class).getChats()) {
					if (c.getId() == chatId) {
						chat = c;
						break;
					}
				}
				if (chat != null) {
					try {
						jaxmpp.getModule(MessageModule.class).close(chat);
					} catch (JaxmppException e) {
						Log.e("OpenChat", "Cannot close chat", e);
					}
				}
				return null;
			}
		}.execute();

	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof MainActivity) {

		}
		if (context instanceof OnAddChatListener)
			mAddChatListener = (OnAddChatListener) context;

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);

		getContext().getContentResolver().registerContentObserver(RosterProvider.ROSTER_URI, true,
				contactPresenceChangeObserver);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO
		inflater.inflate(R.menu.openchat_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_openchatitem_list, container, false);
		ButterKnife.bind(this, root);

		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		this.adapter = new MyOpenChatItemRecyclerViewAdapter(getContext(), null, mListener) {
			@Override
			protected void onContentChanged() {
				refreshChatlist();
			}
		};
		recyclerView.setAdapter(adapter);

		refreshChatlist();
		return root;
	}

	@Override
	public void onDetach() {
		getContext().getContentResolver().unregisterContentObserver(contactPresenceChangeObserver);
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		mListener = null;
		mAddChatListener = null;
		super.onDetach();
	}

	private void onLeaveRoom(final int chatId, final String roomJID, final String account) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Jaxmpp jaxmpp = mConnection.getService().getJaxmpp(account);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);
				Room room = mucModule.getRoom(BareJID.bareJIDInstance(roomJID));
				if (room != null) {
					try {
						mucModule.leave(room);
					} catch (JaxmppException e) {
						Log.e("OpenChat", "Cannot leave room", e);
					}
				}
				return null;
			}
		}.execute();
	}

	@Override
	public void onResume() {
		super.onResume();
		MessageNotification.cancelSummaryNotification(getContext());
	}

	private void refreshChatlist() {
		(new DBUpdateTask()).execute();
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnListFragmentInteractionListener {
		void onArchiveChat(int chatId, String jid, String account);

		void onEnterToChat(int type, int openChatId, String jid, String account);

		void onLeaveMucRoom(int chatId, String roomJID, String account);
	}

	public interface OnAddChatListener {
		// TODO: Update argument type and name
		void onAddChatClick();
	}

	private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
				DatabaseContract.OpenChats.FIELD_ACCOUNT, DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME,
				ChatProvider.FIELD_UNREAD_COUNT, DatabaseContract.OpenChats.FIELD_TYPE, ChatProvider.FIELD_CONTACT_PRESENCE,
				ChatProvider.FIELD_LAST_MESSAGE, ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP, ChatProvider.FIELD_LAST_MESSAGE_STATE};

		@Override
		protected Cursor doInBackground(Void... params) {
			if (getContext() == null)
				return null;
			Cursor cursor = getContext().getContentResolver().query(ChatProvider.OPEN_CHATS_URI, cols, null, null,
					ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP + " DESC");

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
		}
	}
}
