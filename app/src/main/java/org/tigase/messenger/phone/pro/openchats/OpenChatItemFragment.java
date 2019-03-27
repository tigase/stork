/*
 * Tigase Halcyon XMPP Library
 * Copyright (C) 2018 Tigase, Inc. (office@tigase.com)
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

package org.tigase.messenger.phone.pro.openchats;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.searchbar.SearchActionMode;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A fragment representing a list of Items.
 */
public class OpenChatItemFragment
		extends MultiSelectFragment {

	private static final String TAG = "OpenChats";
	RecyclerView recyclerView;
	private MyOpenChatItemRecyclerViewAdapter adapter;
	private DBUpdateTask dbUpdateTask;
	private OnAddChatListener mAddChatListener;
	//
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection();
	private SearchActionMode searchActionMode;
	private final ContentObserver contactPresenceChangeObserver = new ContentObserver(new Handler()) {

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.v(TAG, "Contact presence changed");
			refreshChatlist();
		}
	};

	public static OpenChatItemFragment newInstance(MainActivity.XMPPServiceConnection mServiceConnection) {
		Log.v(TAG, "new instance");
		OpenChatItemFragment fragment = new OpenChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
	 * changes).
	 */
	public OpenChatItemFragment() {
	}

	public Collection<Long> getSelectedIds() {
		ArrayList<Long> result = new ArrayList<>();
		for (int pos : mMultiSelector.getSelectedPositions()) {
			result.add(adapter.getItemId(pos));
		}
		return result;
	}

	@Override
	public void onAttach(Context context) {
		Log.v(TAG, "Attaching to context");
		super.onAttach(context);
		if (context instanceof MainActivity) {

		}
		if (context instanceof OnAddChatListener) {
			mAddChatListener = (OnAddChatListener) context;
		}

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);

		getContext().getContentResolver()
				.registerContentObserver(RosterProvider.ROSTER_URI, true, contactPresenceChangeObserver);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.searchActionMode = new SearchActionMode(getActivity(), txt -> refreshChatlist());
		setHasOptionsMenu(true);
		Log.v(TAG, "Fragment is created");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.openchat_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "Creating view");
		View root = inflater.inflate(R.layout.fragment_openchatitem_list, container, false);

		FloatingActionButton rosterAddChat = root.findViewById(R.id.roster_add_chat);
		rosterAddChat.setOnClickListener(listener -> mAddChatListener.onAddChatClick());

		recyclerView = root.findViewById(R.id.openchats_list);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		this.adapter = new MyOpenChatItemRecyclerViewAdapter(getContext(), null, OpenChatItemFragment.this) {
			@Override
			protected void onContentChanged() {
				Log.v(TAG, "Received content changed.");

				refreshChatlist();
			}
		};
		recyclerView.setAdapter(adapter);

		Log.v(TAG, "View created");

		return root;
	}

	@Override
	public void onDetach() {
		Log.v(TAG, "Detaching from context");
		getContext().getContentResolver().unregisterContentObserver(contactPresenceChangeObserver);
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		mAddChatListener = null;
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_serach: {
				ActionMode am = startActionMode(this.searchActionMode);
				return true;
			}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onResume() {
		Log.v(TAG, "Resume view");
		super.onResume();
		Log.v(TAG, "Resumed view");

		refreshChatlist();

		MessageNotification.cancelSummaryNotification(getContext());
	}

	@Override
	protected boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.delete_chat_history_question)
						.setPositiveButton(R.string.yes, (dialog, which) -> {
							doDeleteChat(getSelectedIds());
							stopActionMode();
						})
						.setNegativeButton(R.string.no, null)
						.show();

				return true;
			case R.id.ac_archive:
				onArchiveChat(getSelectedIds());
				stopActionMode();
				return true;
			case R.id.ac_leavemuc:
				onLeaveRoom(getSelectedIds());
				stopActionMode();
				return true;
			default:
				return false;
		}
	}

	@Override
	protected boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		actionMode.getMenuInflater().inflate(R.menu.openchats_action, menu);
		return true;
	}

	@Override
	protected void updateActionMode(ActionMode actionMode) {
		final int count = mMultiSelector.getSelectedPositions().size();

		actionMode.setTitle(getContext().getResources().getQuantityString(R.plurals.chat_selected, count, count));

		boolean selectedChats = false;
		boolean selectedMuc = false;
		for (int pos : mMultiSelector.getSelectedPositions()) {
			switch (adapter.getItemViewType(pos)) {
				case DatabaseContract.OpenChats.TYPE_CHAT:
					selectedChats = true;
					break;
				case DatabaseContract.OpenChats.TYPE_MUC:
					selectedMuc = true;
					break;
			}
		}

		MenuItem miArchive = actionMode.getMenu().findItem(R.id.ac_archive);
		MenuItem miDelete = actionMode.getMenu().findItem(R.id.ac_delete);
		MenuItem miLeaveMuc = actionMode.getMenu().findItem(R.id.ac_leavemuc);

		miArchive.setEnabled(count > 0);
		miDelete.setEnabled(count > 0);
		miLeaveMuc.setEnabled(count > 0);

		miDelete.setVisible(count == 0 || (selectedChats & !selectedMuc));
		miArchive.setVisible(count == 0 || (selectedChats & !selectedMuc));
		miLeaveMuc.setVisible(count == 0 || (!selectedChats & selectedMuc));
	}

	private void doDeleteChat(final Collection<Long> chatsId) {
		XMPPService service = mConnection.getService();
		if (service == null) {
			Log.w("OpenChatItemFragment", "Service is not binded");
			return;
		}

		final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
										   DatabaseContract.OpenChats.FIELD_ACCOUNT,
										   DatabaseContract.OpenChats.FIELD_JID};
		for (long chatId : chatsId) {
			String account;
			try (Cursor c = getContext().getContentResolver()
					.query(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chatId), cols, null, null, null)) {
				if (c.moveToNext()) {
					account = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
				} else {
					continue;
				}
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
							Uri chatHistoryUri = Uri.parse(
									ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + chat.getJid().getBareJid());
							getContext().getContentResolver().delete(chatHistoryUri, null, null);
						} catch (Exception e) {
							Log.e("OpenChat", "Cannot delete chat", e);
						}

					}
					return null;
				}
			}.execute();
		}
	}

	private void onArchiveChat(Collection<Long> chatsId) {
		XMPPService service = mConnection.getService();

		if (service == null) {
			Log.w("OpenChatItemFragment", "Service is not binded");
			return;
		}

		final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
										   DatabaseContract.OpenChats.FIELD_ACCOUNT,
										   DatabaseContract.OpenChats.FIELD_JID};
		for (long chatId : chatsId) {
			String account;
			try (Cursor c = getContext().getContentResolver()
					.query(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chatId), cols, null, null, null)) {
				if (c.moveToNext()) {
					account = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
				} else {
					continue;
				}
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
	}

	private void onLeaveRoom(Collection<Long> chatsId) {

		final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
										   DatabaseContract.OpenChats.FIELD_ACCOUNT,
										   DatabaseContract.OpenChats.FIELD_JID};
		for (long chatId : chatsId) {
			String account;
			String roomJID;
			try (Cursor c = getContext().getContentResolver()
					.query(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chatId), cols, null, null, null)) {
				if (c.moveToNext()) {
					account = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
					roomJID = c.getString(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
				} else {
					continue;
				}
			}

			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					Log.i(TAG, "Leaving room " + roomJID);
					Jaxmpp jaxmpp = mConnection.getService().getJaxmpp(account);
					MucModule mucModule = jaxmpp.getModule(MucModule.class);
					Room room = mucModule.getRoom(BareJID.bareJIDInstance(roomJID));
					if (room != null) {
						try {
							Log.i(TAG, "Executing Leaving room  " + roomJID);
							mucModule.leave(room);
						} catch (JaxmppException e) {
							Log.e(TAG, "Cannot leave room", e);
						}
					}
					return null;
				}
			}.execute();
		}
	}

	private void refreshChatlist() {
		Log.v(TAG, "Task: " + (dbUpdateTask == null ? "NONE" : dbUpdateTask.getStatus()));
		if (dbUpdateTask == null || dbUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
			String txt = searchActionMode.getSearchText();
			dbUpdateTask = new DBUpdateTask();
			dbUpdateTask.execute(txt);
			Log.v(TAG, "Task executed");
		}
	}

	public interface OnAddChatListener {

		// TODO: Update argument type and name
		void onAddChatClick();
	}

	private class DBUpdateTask
			extends AsyncTask<String, Void, Cursor> {

		private final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
												   DatabaseContract.OpenChats.FIELD_ACCOUNT,
												   DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME,
												   ChatProvider.FIELD_UNREAD_COUNT,
												   DatabaseContract.OpenChats.FIELD_TYPE,
												   ChatProvider.FIELD_CONTACT_PRESENCE, ChatProvider.FIELD_LAST_MESSAGE,
												   ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP,
												   ChatProvider.FIELD_LAST_MESSAGE_STATE};

		@Override
		protected Cursor doInBackground(String... params) {
			Log.d(TAG, "Querying for cursor ctx=" + (getContext() != null));
			if (getContext() == null) {
				return null;
			}
			String searchText = params != null ? params[0] : null;

			String selection;
			String[] args;
			if (searchText == null) {
				selection = null;
				args = null;
			} else {
				selection = ChatProvider.FIELD_NAME + " like ? OR " + DatabaseContract.OpenChats.TABLE_NAME + "." +
						DatabaseContract.OpenChats.FIELD_JID + " like ?";
				args = new String[]{"%" + searchText + "%", "%" + searchText + "%"};

			}
			Cursor cursor = getContext().getContentResolver()
					.query(ChatProvider.OPEN_CHATS_URI, cols, selection, args,
						   ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP + " DESC");
			Log.d(TAG, "Received cursor. size=" + cursor.getCount());

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
		}
	}
}
