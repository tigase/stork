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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import org.tigase.messenger.phone.pro.video.VideoChatActivity;
import org.tigase.messenger.phone.pro.video.WebRTCClient;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

import java.util.List;

public class ChatItemFragment
		extends MultiSelectFragment {

	private static final String TAG = "ChatFragment";
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			super.onServiceDisconnected(name);
		}
	};
	EditText message;
	RecyclerView recyclerView;
	ImageView sendButton;
	private MyChatItemRecyclerViewAdapter adapter;
	private DBUpdateTask dbUpdateTask;
	private BareJID mAccount;
	private SwipeRefreshLayout swipeRefresh;
	private Uri uri;

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static ChatItemFragment newInstance(int columnCount) {
		ChatItemFragment fragment = new ChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
	 * changes).
	 */
	public ChatItemFragment() {
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		this.mAccount = ((ChatActivity) context).getAccount();

		this.uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + ((ChatActivity) getContext()).getAccount() + "/" +
									 ((ChatActivity) getContext()).getJid());

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean available;
		try {
			Jaxmpp jaxmpp = ((ChatActivity) getActivity()).getServiceConnection().getService().getJaxmpp(this.mAccount);
			available = WebRTCClient.isVideoAvailable(jaxmpp, ((ChatActivity) getContext()).getJid().getBareJid());
		} catch (Exception e) {
			available = false;
		}
		menu.findItem(R.id.video_chat).setVisible(available);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chatitem_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);

		this.swipeRefresh = root.findViewById(R.id.swipeRefreshLayout);
		if (swipeRefresh != null) {
			swipeRefresh.setOnRefreshListener(this::loadMoreHistory);
		}

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

		message = root.findViewById(R.id.messageText);
		sendButton = root.findViewById(R.id.send_button);

		message.setOnEditorActionListener((textView, id, keyEvent) -> {
			if (id == R.id.send || id == EditorInfo.IME_NULL) {
				send();
				return true;
			}
			return false;
		});

		sendButton.setOnClickListener(v -> send());

		// recyclerView.addItemDecoration(new
		// DividerItemDecoration(getActivity(),
		// DividerItemDecoration.VERTICAL_LIST));

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MyChatItemRecyclerViewAdapter(getContext(), null, this) {
			@Override
			protected void onContentChanged() {
				refreshChatHistory();
			}
		};
		recyclerView.setAdapter(adapter);

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
			case R.id.video_chat:
				Intent i = new Intent(getContext(), VideoChatActivity.class);
				i.putExtra(VideoChatActivity.ACCOUNT_KEY, mAccount.toString());
				i.putExtra(VideoChatActivity.JID_KEY, ((ChatActivity) getContext()).getJid().toString());
				i.putExtra(VideoChatActivity.INITIATOR_KEY, true);

				startActivity(i);
				return true;
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

	@Override
	public void onResume() {
		Log.v(TAG, "Resume view");
		super.onResume();
		Log.v(TAG, "Resumed view");

		refreshChatHistory();
	}

	protected void loadMoreHistory() {
		Jaxmpp jaxmpp = ((ChatActivity) getActivity()).getServiceConnection().getService().getJaxmpp(this.mAccount);
		(new FetchMoreHistoryTask(getActivity(), this.swipeRefresh, jaxmpp, ((ChatActivity) getContext()).getJid(),
								  uri)).execute();
	}

	@Override
	protected boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.delete_chat_item_question)
						.setPositiveButton(R.string.yes, (dialog, which) -> {
							for (Integer pos : getMultiSelector().getSelectedPositions()) {
								long id = adapter.getItemId(pos);
								getContext().getContentResolver()
										.delete(ChatProvider.CHAT_HISTORY_URI,
												DatabaseContract.ChatHistory.FIELD_ID + "=?",
												new String[]{String.valueOf(id)});
							}
							mode.finish();
						})
						.setNegativeButton(R.string.no, null)
						.show();

				return true;
			case R.id.ac_copy:
				final JID jid = ((ChatActivity) getActivity()).getJid();
				String body = grabContent(getMultiSelector().getSelectedPositions());

				ClipboardManager clipboard = (ClipboardManager) ChatItemFragment.this.getContext()
						.getSystemService(Context.CLIPBOARD_SERVICE);

				ClipData clip = ClipData.newPlainText("Message from " + jid, body);

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

		AccountManager accountManager = AccountManager.get(getContext());
		Account account = AccountHelper.getAccount(accountManager,
												   ((ChatActivity) getActivity()).getAccount().toString());

		final String theirName = ((ChatActivity) getActivity()).getContactName();
		String myNickname = accountManager.getUserData(account, AccountsConstants.FIELD_NICKNAME);

		if (myNickname == null || myNickname.trim().isEmpty()) {
			myNickname = "Me";
		}

		final Cursor cursor = adapter.getCursor();
		for (Integer position : selectedPositions) {
			if (!cursor.moveToPosition(position)) {
				throw new IllegalStateException("couldn't move cursor to position " + position);
			}
			final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
			final long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
			final String timeStr = DateUtils.formatDateTime(getContext(), time,
															DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
																	DateUtils.FORMAT_SHOW_TIME);
			final int state = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
			if (selectedPositions.size() == 1) {
				sb.append(body).append('\n');
			} else {
				sb.append("[").append(timeStr).append("] ");
				switch (state) {
					case DatabaseContract.ChatHistory.STATE_INCOMING:
					case DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD:
						sb.append(theirName).append(": ");
						break;

					case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
					case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
					case DatabaseContract.ChatHistory.STATE_OUT_SENT:
						sb.append(myNickname).append(": ");
				}
				sb.append(body);
				sb.append('\n');
			}

		}

		return sb.toString();
	}

	private void refreshChatHistory() {
		Log.v(TAG, "Task: " + (dbUpdateTask == null ? "NONE" : dbUpdateTask.getStatus()));
		if (dbUpdateTask == null || dbUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
			dbUpdateTask = new DBUpdateTask();
			dbUpdateTask.execute();
			Log.v(TAG, "Task executed");
		}
	}

//	private void setVisibility(final FloatingActionButton floatingActionButton, boolean showing) {
//		this.scrollVisible = showing;
//		floatingActionButton.setVisibility(View.VISIBLE);
//		if (!showing) {
////			Animation animation = new AlphaAnimation(1.0f, 0.0f);
////			animation.setFillAfter(true);
////			animation.setDuration(300);
////			floatingActionButton.startAnimation(animation);
//
//			floatingActionButton.hide();
//		} else {
//			floatingActionButton.show();
////			Animation animation = new AlphaAnimation(0.0f, 1.0f);
////			animation.setFillAfter(true);
////			animation.setDuration(300);
////			floatingActionButton.setAlpha(0.0f);
////			floatingActionButton.startAnimation(animation);
//		}
//
////		if (showing) {
////			floatingActionButton.setVisibility(View.VISIBLE);
////			floatingActionButton.setAlpha(0.0f);
////			floatingActionButton.animate().alpha(1f).setDuration(300).setListener(new AnimatorListenerAdapter() {
////				@Override
////				public void onAnimationEnd(Animator animation) {
////					super.onAnimationEnd(animation);
////
////				}
////			});
////		} else {
////			floatingActionButton.setVisibility(View.VISIBLE);
////			floatingActionButton.setAlpha(100.0f);
////			floatingActionButton.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
////				@Override
////				public void onAnimationEnd(Animator animation) {
////					super.onAnimationEnd(animation);
////					floatingActionButton.setVisibility(View.GONE);
////				}
////			});
////		}
//	}

	private void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty()) {
			return;
		}

		Intent intent = new Intent(getContext(), XMPPService.class);
		intent.setAction(MessageSender.SEND_CHAT_MESSAGE_ACTION);
		intent.putExtra(MessageSender.BODY, body);
		intent.putExtra(MessageSender.CHAT_ID, ((ChatActivity) getContext()).getOpenChatId());
		intent.putExtra(MessageSender.ACCOUNT, mAccount.toString());

		this.message.getText().clear();
		getContext().startService(intent);
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
												   DatabaseContract.ChatHistory.FIELD_THREAD_ID,
												   DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI,
												   DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		@Override
		protected Cursor doInBackground(Void... params) {
			Log.d(TAG, "Querying for cursor ctx=" + (getContext() != null));
			if (getContext() == null) {
				return null;
			}
			Cursor cursor = getContext().getContentResolver()
					.query(uri, cols, null, null, DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC");

			Log.d(TAG, "Received cursor. size=" + cursor.getCount());

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
//			recyclerView.smoothScrollToPosition(0);
		}
	}
}
