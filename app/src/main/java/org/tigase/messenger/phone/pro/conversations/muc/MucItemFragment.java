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

package org.tigase.messenger.phone.pro.conversations.muc;

import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.conversations.DaysInformCursor;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

import java.util.List;

public class MucItemFragment
		extends MultiSelectFragment {

	EditText message;
	RecyclerView recyclerView;
	ImageView sendButton;
	private MucItemRecyclerViewAdapter adapter;

	private Room room;
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			BareJID account = ((AbstractConversationActivity) getContext()).getAccount();
			Jaxmpp jaxmpp = getService().getJaxmpp(account);

			final BareJID roomJID = ((AbstractConversationActivity) getContext()).getJid().getBareJid();

			if (jaxmpp == null) {

			}
			Log.d("MucItemFragment", "RoomJID=" + roomJID);
			setRoom(jaxmpp.getModule(MucModule.class).getRoom(roomJID));
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			setRoom(null);
			super.onServiceDisconnected(name);
		}
	};
	private Uri uri;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		this.uri = Uri.parse(
				ChatProvider.MUC_HISTORY_URI + "/" + ((AbstractConversationActivity) getContext()).getAccount() + "/" +
						((AbstractConversationActivity) getContext()).getJid());

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chatitem_fragment, menu);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);

		recyclerView = root.findViewById(R.id.chat_list);
		message = root.findViewById(R.id.messageText);
		sendButton = root.findViewById(R.id.send_button);
		sendButton.setOnClickListener(view -> send());

		final FloatingActionButton floatingActionButton = root.findViewById(R.id.scroll_down);
		floatingActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recyclerView.smoothScrollToPosition(0);
			}
		});
		floatingActionButton.hide();

		recyclerView = root.findViewById(R.id.chat_list);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
				@Override
				public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
					LinearLayoutManager myLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
					final boolean shouldBeVisible = myLayoutManager.findFirstVisibleItemPosition() > 0;

					if (shouldBeVisible) {
						floatingActionButton.show();
					} else {
						floatingActionButton.hide();
					}
				}
			});
		}

		message.setEnabled(false);

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MucItemRecyclerViewAdapter(getContext(), null, this) {
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
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.send_file:
				if (!MainActivity.hasPermissions(getContext(), MainActivity.STORAGE_PERMISSIONS)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

					builder.setTitle(R.string.warning).setMessage(R.string.no_permissions).create().show();

					return true;
				}
				Intent intent = new Intent();
				if (Build.VERSION.SDK_INT < 19) {
					intent.setAction(Intent.ACTION_GET_CONTENT);
				} else {
					//KitKat 4.4 o superior
					intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
				}
				intent.setType("image/*");
				getActivity().startActivityForResult(intent, ChatActivity.FILE_UPLOAD_REQUEST_CODE);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty()) {
			return;
		}

		Intent intent = new Intent(getContext(), XMPPService.class);
		intent.setAction(MessageSender.SEND_GROUPCHAT_MESSAGE_ACTION);
		intent.putExtra(MessageSender.BODY, body);
		intent.putExtra(MessageSender.ROOM_JID, room.getRoomJid().toString());
		intent.putExtra(MessageSender.ACCOUNT, room.getSessionObject().getUserBareJid().toString());

		this.message.getText().clear();
		getContext().startService(intent);
	}

	@Override
	protected boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_delete:
				android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
						getActivity());
				builder.setMessage(R.string.delete_chat_item_question)
						.setPositiveButton(R.string.yes, (dialog, which) -> {
							for (Integer pos : getMultiSelector().getSelectedPositions()) {
								long id = adapter.getItemId(pos);
								getContext().getContentResolver()
										.delete(ChatProvider.CHAT_HISTORY_URI,
												DatabaseContract.ChatHistory.FIELD_ID + "=?",
												new String[]{String.valueOf(id)});
							}
							getContext().getContentResolver().notifyChange(uri, null);
							mode.finish();
						})
						.setNegativeButton(R.string.no, null)
						.show();

				return true;
			case R.id.ac_copy:
				String body = grabContent(getMultiSelector().getSelectedPositions());
				ClipboardManager clipboard = (ClipboardManager) MucItemFragment.this.getContext()
						.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Messages from room " + room.getRoomJid(), body);

				clipboard.setPrimaryClip(clip);

				mode.finish();
				return true;
			default:
				return false;
		}
	}

	@Override
	protected boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		actionMode.getMenuInflater().inflate(R.menu.chathistory_action, menu);
		return true;
	}

	@Override
	protected void updateActionMode(ActionMode actionMode) {
		final int count = mMultiSelector.getSelectedPositions().size();
		actionMode.setTitle(getContext().getResources().getQuantityString(R.plurals.message_selected, count, count));
	}

	private String grabContent(List<Integer> selectedPositions) {
		StringBuilder sb = new StringBuilder();

		final Cursor cursor = adapter.getCursor();
		for (Integer position : selectedPositions) {
			if (!cursor.moveToPosition(position)) {
				throw new IllegalStateException("couldn't move cursor to position " + position);
			}
			final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
			final String nickname = cursor.getString(
					cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME));
			final long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
			final String timeStr = DateUtils.formatDateTime(getContext(), time,
															DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
																	DateUtils.FORMAT_SHOW_TIME);
			final int state = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
			if (selectedPositions.size() == 1) {
				sb.append(body).append('\n');
			} else {
				sb.append("[").append(timeStr).append("] ");

				if (nickname != null) {
					sb.append(nickname).append(": ");
				}
				sb.append(body);
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	private void refreshChatHistory() {
		(new MucItemFragment.DBUpdateTask()).execute();
	}

	private void setRoom(Room room) {
		this.room = room;
		message.setEnabled(room != null);
		if (adapter != null) {
			adapter.setOwnNickname(room == null ? null : room.getNickname());
		}
	}

	private class DBUpdateTask
			extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
												   DatabaseContract.ChatHistory.FIELD_ACCOUNT,
												   DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
												   DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
												   DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
												   DatabaseContract.ChatHistory.FIELD_BODY,
												   DatabaseContract.ChatHistory.FIELD_DATA,
												   DatabaseContract.ChatHistory.FIELD_JID,
												   DatabaseContract.ChatHistory.FIELD_STATE,
												   DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
												   DatabaseContract.ChatHistory.FIELD_THREAD_ID,
												   DatabaseContract.ChatHistory.FIELD_ENCRYPTION,
												   DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		@Override
		protected Cursor doInBackground(Void... params) {
			if (getContext() == null) {
				return null;
			}
			try (Cursor cursor = getContext().getContentResolver()
					.query(uri, cols, null, null, DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC")) {
				DaysInformCursor c = new DaysInformCursor(getContext().getContentResolver(), cursor,
														  DatabaseContract.ChatHistory.FIELD_TIMESTAMP);
				return c;
			}
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
			recyclerView.smoothScrollToPosition(0);
		}
	}
}
