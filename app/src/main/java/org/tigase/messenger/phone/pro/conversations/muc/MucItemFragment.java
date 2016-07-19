package org.tigase.messenger.phone.pro.conversations.muc;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

public class MucItemFragment extends Fragment {

	RecyclerView recyclerView;
	EditText message;
	ImageView sendButton;
	private Room room;
	private Uri uri;
	private MucItemRecyclerViewAdapter adapter;
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
	private ChatItemIterationListener mListener = new ChatItemIterationListener() {

		@Override
		public void onCopyChatMessage(int id, String jid, String body) {
			ClipboardManager clipboard = (ClipboardManager) MucItemFragment.this.getContext().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Message from " + jid, body);

			clipboard.setPrimaryClip(clip);
		}
	};

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		this.uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + ((AbstractConversationActivity) getContext()).getAccount()
				+ "/" + ((AbstractConversationActivity) getContext()).getJid());

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);

		recyclerView = (RecyclerView) root.findViewById(R.id.chat_list);
		message = (EditText) root.findViewById(R.id.messageText);
		sendButton = (ImageView) root.findViewById(R.id.send_button);
		sendButton.setOnClickListener(view -> send());

		message.setEnabled(false);

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MucItemRecyclerViewAdapter(getContext(), null, mListener) {
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
		(new MucItemFragment.DBUpdateTask()).execute();
	}

	void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty())
			return;

		this.message.getText().clear();
		(new SendMessageTask()).execute(body);
	}

	private void setRoom(Room room) {
		this.room = room;
		message.setEnabled(room != null);
		if (adapter != null) {
			adapter.setOwnNickname(room.getNickname());
		}
	}

	public interface ChatItemIterationListener {
		void onCopyChatMessage(int id, String jid, String body);
	}

	private class SendMessageTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			for (String param : params) {
				int state;
				Message msg;
				String stanzaId = null;
				try {
					msg = room.createMessage(param);
					Jaxmpp jaxmpp = mConnection.getService().getJaxmpp(room.getSessionObject().getUserBareJid());

					stanzaId = msg.getId();
					if (jaxmpp.isConnected() && room.getState() == Room.State.joined) {
						room.sendMessage(msg);
						state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
					} else {
						state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					}
				} catch (Exception e) {
					state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					Log.w("MucItemFragment", "Cannot send message", e);
				}

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, room.getNickname());
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, System.currentTimeMillis());
				values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

				values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
				values.put(DatabaseContract.ChatHistory.FIELD_BODY, param);
				values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
						DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE);

				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, room.getSessionObject().getUserBareJid().toString());

				Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + room.getSessionObject().getUserBareJid() + "/"
						+ Uri.encode(room.getRoomJid().toString()));
				Uri x = getContext().getContentResolver().insert(uri, values);
				if (x != null)
					getContext().getContentResolver().notifyChange(x, null);
			}

			return null;
		}
	}

	private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

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
