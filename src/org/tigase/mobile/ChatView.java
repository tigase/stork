package org.tigase.mobile;

import java.util.Date;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChatView extends LinearLayout {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	private Chat chat;

	private EditText ed;

	private final Jaxmpp jaxmpp;

	private final SharedPreferences prefs;

	public ChatView(Context context) {
		super(context);
		this.jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getJaxmpp();
		this.prefs = getContext().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
	}

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getJaxmpp();
		this.prefs = getContext().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
	}

	public Chat getChat() {
		return chat;
	}

	private BareJID getCurrentUserJid() {
		JID jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		if (jid == null) {
			String x = prefs.getString(Preferences.USER_JID_KEY, null);
			if (x == null)
				throw new RuntimeException("Users JID is not defined");
			jid = JID.jidInstance(x);
		}
		return jid.getBareJid();
	}

	void init() {
		if (DEBUG)
			Log.i(TAG, "Zrobione");

		this.ed = (EditText) findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = prefs.getBoolean(Preferences.ENTER_TO_SEND_KEY, true);
				if (ets && keyCode == KeyEvent.KEYCODE_ENTER) {
					sendMessage();
					return true;
				}
				return false;
			}
		});

		final Button b = (Button) findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		final ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});
	}

	protected void sendMessage() {
		if (ed == null)
			return;

		String t = ed.getText().toString();
		ed.setText("");

		if (t == null || t.length() == 0)
			return;
		if (DEBUG)
			Log.d(TAG, "Send: " + t);

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			public Void doInBackground(String... ts) {
				String t = ts[0];
				Log.d(TAG, "Send: " + t);
				int state;
				try {
					chat.sendMessage(t);
					state = ChatTableMetaData.STATE_OUT_SENT;
				} catch (Exception e) {
					state = ChatTableMetaData.STATE_OUT_NOT_SENT;
					Log.e(TAG, e.getMessage(), e);
				}
				// dbHelper.addChatHistory(1, chat, t);

				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, getCurrentUserJid().toString());
				values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
				values.put(ChatTableMetaData.FIELD_BODY, t);
				values.put(ChatTableMetaData.FIELD_STATE, state);

				getContext().getContentResolver().insert(uri, values);

				return null;
			}
		};
		task.execute(t);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;
		TextView t = (TextView) findViewById(R.id.textView1);
		RosterItem ri = jaxmpp.getRoster().get(chat.getJid().getBareJid());
		t.setText("Chat with "
				+ (ri == null ? chat.getJid().getBareJid().toString()
						: (new RosterDisplayTools(getContext())).getDisplayName(ri)));
	}

	public void setImagePresence(final CPresence cp) {

		final ImageView itemPresence = (ImageView) findViewById(R.id.user_presence);

		itemPresence.post(new Runnable() {

			@Override
			public void run() {
				if (cp == null)
					itemPresence.setImageResource(R.drawable.user_offline);
				else
					switch (cp) {
					case chat:
						itemPresence.setImageResource(R.drawable.user_free_for_chat);
						break;
					case online:
						itemPresence.setImageResource(R.drawable.user_available);
						break;
					case away:
						itemPresence.setImageResource(R.drawable.user_away);
						break;
					case xa:
						itemPresence.setImageResource(R.drawable.user_extended_away);
						break;
					case dnd:
						itemPresence.setImageResource(R.drawable.user_busy);
						break;
					case requested:
						itemPresence.setImageResource(R.drawable.user_ask);
						break;
					case error:
						itemPresence.setImageResource(R.drawable.user_error);
						break;
					case offline_nonauth:
						itemPresence.setImageResource(R.drawable.user_noauth);
						break;
					default:
						itemPresence.setImageResource(R.drawable.user_offline);
						break;
					}
			}
		});

	}

}
