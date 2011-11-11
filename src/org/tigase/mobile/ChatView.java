package org.tigase.mobile;

import java.util.Date;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
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

	public ChatView(Context context) {
		super(context);
	}

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Chat getChat() {
		return chat;
	}

	void init() {
		if (DEBUG)
			Log.i(TAG, "Zrobione");

		final SharedPreferences prefs = getContext().getSharedPreferences("org.tigase.mobile_preferences", Context.MODE_PRIVATE);

		this.ed = (EditText) findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = prefs.getBoolean("enter_to_send", true);
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
		int state;
		try {
			chat.sendMessage(t);
			state = ChatTableMetaData.STATE_OUT_SENT;
		} catch (Exception e) {
			state = ChatTableMetaData.STATE_OUT_NOT_SENT;
			Log.e(TAG, e.getMessage(), e);
		}

		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
		values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
		values.put(ChatTableMetaData.FIELD_BODY, t);
		values.put(ChatTableMetaData.FIELD_STATE, state);

		getContext().getContentResolver().insert(uri, values);

	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;
		TextView t = (TextView) findViewById(R.id.textView1);
		RosterItem ri = XmppService.jaxmpp(getContext()).getRoster().get(chat.getJid().getBareJid());
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
					default:
						itemPresence.setImageResource(R.drawable.user_offline);
						break;
					}
			}
		});

	}

}
