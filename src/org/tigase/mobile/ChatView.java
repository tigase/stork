package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ChatView extends LinearLayout {

	private Chat chat;
	private MessengerDatabaseHelper dbHelper;

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
		Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Send: " + t);
		try {
			chat.sendMessage(t);
		} catch (Exception e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, e.getMessage(), e);
		}
		dbHelper.addChatHistory(1, chat, t);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;
		TextView t = (TextView) findViewById(R.id.textView1);
		RosterItem ri = XmppService.jaxmpp().getRoster().get(chat.getJid().getBareJid());
		t.setText("Chat with " + MessengerDatabaseHelper.getDisplayName(ri));
	}

	public void setCursorAdapter(ListAdapter adapter) {
		final ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.setAdapter(adapter);
		adapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				// lv.sc(1, lv.getMeasuredHeight());
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

	}

	public void setDbHelper(MessengerDatabaseHelper dbHelper) {
		this.dbHelper = dbHelper;
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
