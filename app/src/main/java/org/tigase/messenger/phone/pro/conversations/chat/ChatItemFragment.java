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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.jetbrains.annotations.NotNull;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.conversations.DaysInformCursor;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.roster.contact.ContactInfoActivity;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionFragment;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import org.tigase.messenger.phone.pro.video.VideoChatActivity;
import org.tigase.messenger.phone.pro.video.WebRTCClient;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

import static org.tigase.messenger.phone.pro.conversations.chat.OMEMOStartService.RESULT_ACTION;

public class ChatItemFragment
		extends SelectionFragment<MyChatItemRecyclerViewAdapter> {

	private static final String TAG = "ChatFragment";
	public static int START_OMEMO_REQUEST = 1000;
	private final BroadcastReceiver bReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			handleOMEMOStartMessage(context, intent);
		}
	};
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection();
	private DBUpdateTask dbUpdateTask;
	private FloatingActionButton floatingActionButton;
	private BareJID mAccount;
	private EditText message;
	private ImageView sendButton;
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
		super(R.layout.fragment_chatitem_list);
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
		boolean videoChatAvailable;
		try {
			Jaxmpp jaxmpp = ((ChatActivity) getActivity()).getServiceConnection().getService().getJaxmpp(this.mAccount);
			videoChatAvailable = WebRTCClient.isVideoAvailable(jaxmpp,
															   ((ChatActivity) getContext()).getJid().getBareJid());
		} catch (Exception e) {
			videoChatAvailable = false;
		}
		menu.findItem(R.id.video_chat).setVisible(videoChatAvailable);

		if (((ChatActivity) getContext()).isEncryptChat()) {
			menu.findItem(R.id.encrypt_chat).setChecked(true);
			menu.findItem(R.id.encrypt_chat).setIcon(R.drawable.ic_encryption_on);
		} else {
			menu.findItem(R.id.encrypt_chat).setChecked(false);
			menu.findItem(R.id.encrypt_chat).setIcon(R.drawable.ic_encryption_off);
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chatitem_fragment, menu);
	}

	@Override
	public void onViewCreated(@NonNull @NotNull View view,
							  @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		this.swipeRefresh = view.findViewById(R.id.swipeRefreshLayout);
		if (swipeRefresh != null) {
			swipeRefresh.setOnRefreshListener(this::loadMoreHistory);
		}

		this.floatingActionButton = view.findViewById(R.id.scroll_down);
		floatingActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getRecyclerView().smoothScrollToPosition(0);
			}
		});
		floatingActionButton.hide();

		message = view.findViewById(R.id.messageText);
		sendButton = view.findViewById(R.id.send_button);

		message.setOnEditorActionListener((textView, id, keyEvent) -> {
			if (id == R.integer.action_send || id == EditorInfo.IME_NULL) {
				send();
				return true;
			}
			return false;
		});

		sendButton.setOnClickListener(v -> send());

	}

	@Override
	public void onDetach() {
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.contact_info) {
			Intent contactInfoIntent = new Intent(getContext(), ContactInfoActivity.class);
			contactInfoIntent.putExtra(ContactInfoActivity.ACCOUNT_KEY, mAccount.toString());
			contactInfoIntent.putExtra(ContactInfoActivity.JID_KEY, ((ChatActivity) getContext()).getJid().toString());
			startActivity(contactInfoIntent);
			return true;
		} else if (itemId == R.id.encrypt_chat) {
			boolean s = !((ChatActivity) getContext()).isEncryptChat();

			if (s) {
				OMEMOStartService.startActionEnable(getContext(), mAccount.toString(),
													((ChatActivity) getContext()).getJid(),
													((ChatActivity) getContext()).getOpenChatId());
			} else {
				OMEMOStartService.startActionDisable(getContext(), mAccount.toString(),
													 ((ChatActivity) getContext()).getJid(),
													 ((ChatActivity) getContext()).getOpenChatId());
			}
			return true;
		} else if (itemId == R.id.video_chat) {
			Intent i = new Intent(getContext(), VideoChatActivity.class);
			i.putExtra(VideoChatActivity.ACCOUNT_KEY, mAccount.toString());
			i.putExtra(VideoChatActivity.JID_KEY, ((ChatActivity) getContext()).getJid().toString());
			i.putExtra(VideoChatActivity.INITIATOR_KEY, true);

			startActivity(i);
			return true;
		} else if (itemId == R.id.send_file) {
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
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		Log.v(TAG, "Resume view");
		super.onResume();
		Log.v(TAG, "Resumed view");

		LocalBroadcastManager.getInstance(getContext()).registerReceiver(bReceiver, new IntentFilter(RESULT_ACTION));

		refreshChatHistory();
	}

	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(bReceiver);
		super.onPause();
	}

	@Override
	protected @NotNull RecyclerView findRecyclerView(@NotNull View view) {

		RecyclerView recyclerView = view.findViewById(R.id.chat_list);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			recyclerView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
				LinearLayoutManager myLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
				final boolean shouldBeVisible = myLayoutManager.findFirstVisibleItemPosition() > 0;

				if (shouldBeVisible) {
					floatingActionButton.show();
				} else {
					floatingActionButton.hide();
				}
			});
		}
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);

		return recyclerView;
	}

	@Override
	protected @NotNull MyChatItemRecyclerViewAdapter createAdapterInstance() {
		return new MyChatItemRecyclerViewAdapter() {
			@Override
			protected void onContentChanged() {
				refreshChatHistory();
			}
		};
	}

	protected void loadMoreHistory() {
		Jaxmpp jaxmpp = ((ChatActivity) getActivity()).getServiceConnection().getService().getJaxmpp(this.mAccount);
		(new FetchMoreHistoryTask(getActivity(), this.swipeRefresh, jaxmpp, ((ChatActivity) getContext()).getJid(),
								  uri)).execute();
	}

	@Override
	protected ActionMode.Callback getActionModeCallback() {
		return new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.chathistory_action, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				return ChatItemFragment.this.onActionItemClicked(mode, item);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}
		};
	}

	@Override
	protected void doOnSelectionChange() {
		super.doOnSelectionChange();

		final int count = getSelection().size();
		final ActionMode actionMode = getActionMode();
		if (actionMode == null) {
			return;
		}

		actionMode.setTitle(getContext().getResources().getQuantityString(R.plurals.message_selected, count, count));
	}

	private void handleOMEMOStartMessage(Context context, Intent intent) {
		final int chatIt = intent.getIntExtra(OMEMOStartService.RESULT_CHAT_ID, Integer.MIN_VALUE);
		final int result = intent.getIntExtra(OMEMOStartService.RESULT_TYPE, Integer.MIN_VALUE);
		if (chatIt == ((ChatActivity) getContext()).getOpenChatId() &&
				result == OMEMOStartService.RESULT_TYPE_ENABLED) {
			((ChatActivity) getContext()).setEncryptChat(true);
		} else if (chatIt == ((ChatActivity) getContext()).getOpenChatId() &&
				result == OMEMOStartService.RESULT_TYPE_DISABLED) {
			((ChatActivity) getContext()).setEncryptChat(false);
		}
	}

	private boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.delete_chat_item_question)
						.setPositiveButton(R.string.yes, (dialog, which) -> {
							for (long id : getSelection()) {
								requireContext().getContentResolver()
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
				String body = grabContent(getSelection());

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

	private String grabContent(final Selection<Long> selectedIds) {
		StringBuilder sb = new StringBuilder();

		AccountManager accountManager = AccountManager.get(getContext());
		Account account = AccountHelper.getAccount(accountManager,
												   ((ChatActivity) getActivity()).getAccount().toString());

		final String theirName = ((ChatActivity) getActivity()).getContactDisplayName();
		String myNickname = accountManager.getUserData(account, AccountsConstants.FIELD_NICKNAME);

		if (myNickname == null || myNickname.trim().isEmpty()) {
			myNickname = "Me";
		}

		final Cursor cursor = getAdapter().getCursor();
		for (Long id : selectedIds) {
			int position = getStableIdKeyProvider().getPosition(id);
			if (!cursor.moveToPosition(position)) {
				throw new IllegalStateException("couldn't move cursor to position " + position);
			}
			final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
			final long time = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
			final String timeStr = DateUtils.formatDateTime(getContext(), time,
															DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
																	DateUtils.FORMAT_SHOW_TIME);
			final int state = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
			if (selectedIds.size() == 1) {
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
		if (body.trim().isEmpty()) {
			return;
		}

		Intent intent = new Intent(getContext(), XMPPService.class);
		intent.setAction(MessageSender.SEND_CHAT_MESSAGE_ACTION);
		intent.putExtra(MessageSender.BODY, body);
		intent.putExtra(MessageSender.CHAT_ID, ((ChatActivity) getContext()).getOpenChatId());
		intent.putExtra(MessageSender.ACCOUNT, mAccount.toString());
		intent.putExtra(MessageSender.ENCRYPT_MESSAGE, ((ChatActivity) getContext()).isEncryptChat());

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
												   DatabaseContract.ChatHistory.FIELD_ENCRYPTION,
												   DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		@Override
		protected Cursor doInBackground(Void... params) {
			Log.d(TAG, "Querying for cursor ctx=" + (getContext() != null));
			if (getContext() == null) {
				return null;
			}

			try (Cursor cursor = getContext().getContentResolver()
					.query(uri, cols, null, null, DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC")) {
				Log.d(TAG, "Received cursor. size=" + cursor.getCount());

				DaysInformCursor c = new DaysInformCursor(getContext().getContentResolver(), cursor,
														  DatabaseContract.ChatHistory.FIELD_TIMESTAMP);
				return c;
			}
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			getAdapter().swapCursor(cursor);
//			recyclerView.smoothScrollToPosition(0);
		}
	}
}
