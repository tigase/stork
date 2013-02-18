package org.tigase.mobile.chat;

import java.util.Date;
import java.util.List;

import org.tigase.mobile.FragmentWithUID;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.filetransfer.AndroidFileTransferUtility;
import org.tigase.mobile.filetransfer.FileTransferUtility;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class ChatHistoryFragment extends FragmentWithUID implements LoaderCallbacks<Cursor> {

	private static final boolean DEBUG = true;

	private static final String TAG = "tigase-chat";

	public static Fragment newInstance(String account, long chatId) {
		ChatHistoryFragment f = new ChatHistoryFragment();

		Bundle args = new Bundle();
		args.putLong("chatId", chatId);
		args.putString("account", account);
		f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating ChatFragment id=" + chatId);

		return f;
	}

	public static String prepareAdditionalDebug(final MultiJaxmpp multi) {
		try {
			String s = "Known wrappers: [";

			for (ChatWrapper w : multi.getChats()) {
				if (w.isChat()) {
					s += "CHAT=" + w.getChat().getId() + " ";
				} else if (w.isRoom()) {
					s += "ROOM=" + w.getRoom().getId() + " ";
				} else {
					s += "SOMETHINGELSE ";
				}
			}
			s += "] ";

			s += "Known JAXMPP: [";
			for (JaxmppCore j : multi.get()) {
				s += j.getSessionObject().getUserBareJid().toString() + " ";
			}
			s += "] ";

			return s;
		} catch (Exception e) {
			Log.e(TAG, "WTF?", e);
			return "Exception: " + e.getMessage();
		}

	}

	// private Cursor c;

	private Chat chat;

	private ChatAdapter chatAdapter;

	private Listener<MessageEvent> chatUpdateListener;

	private ChatView layout;

	private ListView lv;

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

	private void clearMessageHistory() {
		getActivity().getApplicationContext().getContentResolver().delete(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(chat.getJid().getBareJid().toString())), null, null);
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
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(chat.getJid().getBareJid().toString()) + "/" + id),
				null, null, null, null);
		cursor.moveToNext();
		return cursor;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			long id = getArguments().getLong("chatId");
			MultiJaxmpp multi = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp();
			ChatWrapper ch = multi.getChatById(id);

			if (ch == null) {
				String msg = prepareAdditionalDebug(multi);
				Log.v(TAG, "ChatWrapper is null with id = " + id + '\n' + msg);
				((TigaseMobileMessengerActivity) getActivity()).viewPager.getAdapter().notifyDataSetChanged();
			} else {
				if (ch.getChat() == null) {
					throw new NullPointerException("ChatWrapper.getChat() is null with id = " + id);
				}
				if (ch.getChat().getSessionObject() == null) {
					throw new NullPointerException("ChatWrapper.getChat().getSessionObject() is null with id = " + id);
				}
				setChatId(ch.getChat().getSessionObject().getUserBareJid(), ch.getChat().getId());
			}
		}
		layout.setChat(chat);
		getLoaderManager().initLoader(fragmentUID, null, this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TigaseMobileMessengerActivity.SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			Uri selected = data.getData();
			String mimetype = data.getType();
			RosterItem ri = chat.getSessionObject().getRoster().get(chat.getJid().getBareJid());
			JID jid = chat.getJid();
			if (jid.getResource() == null) {
				final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
						ri.getSessionObject());
				jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
			}
			if (jid != null) {
				AndroidFileTransferUtility.startFileTransfer(getActivity(), ri, chat.getJid(), selected, mimetype);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.detailsMessage) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			showMessageDetails(info.id);
			return true;
		} else if (item.getItemId() == R.id.copyMessage) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			copyMessageBody(info.id);
			return true;
		} else if (item.getItemId() == R.id.clearMessageHistory) {
			clearMessageHistory();
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);

		this.chatAdapter = new ChatAdapter(getActivity(), R.layout.chat_item);
		chatAdapter.registerDataSetObserver(new DataSetObserver() {

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

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater m = new MenuInflater(getActivity());
		m.inflate(R.menu.chat_context_menu, menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(), Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ Uri.encode(chat.getJid().getBareJid().toString())), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();

		inflater.inflate(R.menu.chat_main_menu, menu);

		// Share button support
		MenuItem share = menu.findItem(R.id.shareButton);

		boolean visible = false;
		if (chat != null) {
			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
					chat.getSessionObject());
			try {
				JID jid = chat.getJid();
				if (jid.getResource() == null) {
					jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
				}
				if (jid != null) {
					visible = FileTransferUtility.resourceContainsFeatures(jaxmpp, chat.getJid(), FileTransferUtility.FEATURES);
				}
			} catch (XMLException e) {
			}
		} else {
			Log.v(TAG, "no chat for fragment");
		}
		share.setVisible(visible);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();

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

		// if (chat == null) {
		// throw new RuntimeException("Chat not specified!");
		// }

		this.lv = (ListView) layout.findViewById(R.id.chat_conversation_history);
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

		lv.setAdapter(chatAdapter);

		return layout;
	}

	@Override
	public void onDestroyView() {
		// Cursor c = chatAdapter.getCursor();
		// if (c != null) {
		// if (DEBUG)
		// Log.d(TAG, "Closing cursor");
		// c.close();
		// }
		super.onDestroyView();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		chatAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		chatAdapter.swapCursor(cursor);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.showChatsButton) {
			Intent chatListActivity = new Intent(getActivity(), ChatListActivity.class);
			this.getActivity().startActivityForResult(chatListActivity, TigaseMobileMessengerActivity.REQUEST_CHAT);
		} else if (item.getItemId() == R.id.closeChatButton) {
			layout.cancelEdit();
			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
					chat.getSessionObject());
			ViewPager viewPager = ((TigaseMobileMessengerActivity) this.getActivity()).viewPager;
			final AbstractChatManager cm = jaxmpp.getModule(MessageModule.class).getChatManager();
			try {
				cm.close(chat);
				viewPager.setCurrentItem(1);
				if (DEBUG)
					Log.i(TAG, "Chat with " + chat.getJid() + " (" + chat.getId() + ") closed");
			} catch (JaxmppException e) {
				Log.w(TAG, "Chat close problem!", e);
			}
		} else if (item.getItemId() == R.id.shareImageButton) {
			Log.v(TAG, "share selected for = " + chat.getJid().toString());
			Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			pickerIntent.setType("image/*");
			pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(pickerIntent, TigaseMobileMessengerActivity.SELECT_FOR_SHARE);
		} else if (item.getItemId() == R.id.shareVideoButton) {
			Log.v(TAG, "share selected for = " + chat.getJid().toString());
			Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			pickerIntent.setType("video/*");
			pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(pickerIntent, TigaseMobileMessengerActivity.SELECT_FOR_SHARE);
		}
		return true;
	}

	@Override
	public void onResume() {
		// if (((ChatAdapter) lv.getAdapter()).getCursor().isClosed()) {
		// ((ChatAdapter) lv.getAdapter()).swapCursor(getCursor());
		// }

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

		List<ChatWrapper> l = multi.getChats();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getId() == chatId) {
				this.chat = c.getChat();
				if (DEBUG)
					Log.d(TAG, "Found chat with " + chat.getJid() + " (id=" + chatId + ")");

				return;
			}
		}

		String ids = "";
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			ids += c + " ";
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

			TigaseMobileMessengerActivity activity = ((TigaseMobileMessengerActivity) getActivity());
			if (activity != null && activity.helper != null && chat != null) {
				activity.helper.updateActionBar(chat.hashCode());
			}

		}
	}

}
