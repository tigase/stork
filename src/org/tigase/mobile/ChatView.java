package org.tigase.mobile;

import java.util.Date;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChatView extends LinearLayout {

	private Chat chat;

	// private MessengerDatabaseHelper dbHelper;

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
		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Zrobione");

		final EditText ed = (EditText) findViewById(R.id.chat_message_entry);

		final Button b = (Button) findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Klikniete");
				String t = ed.getText().toString();
				ed.setText("");

				sendMessage(t);

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

	protected void sendMessage(String t) {
		if (t == null || t.length() == 0)
			return;
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Send: " + t);
		try {
			chat.sendMessage(t);
		} catch (Exception e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, e.getMessage(), e);
		}

		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
		values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
		values.put(ChatTableMetaData.FIELD_BODY, t);
		values.put(ChatTableMetaData.FIELD_TYPE, 1);
		values.put(ChatTableMetaData.FIELD_STATE, 0);

		getContext().getContentResolver().insert(uri, values);

	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;
		TextView t = (TextView) findViewById(R.id.textView1);
		RosterItem ri = XmppService.jaxmpp().getRoster().get(chat.getJid().getBareJid());
		t.setText("Chat with " + RosterProvider.getDisplayName(ri));
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
