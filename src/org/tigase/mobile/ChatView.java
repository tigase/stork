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
		Log.i("s", "Zrobione");

		final EditText ed = (EditText) findViewById(R.id.chat_message_entry);

		final Button b = (Button) findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i("s", "Klikniete");
				String t = ed.getText().toString();
				ed.setText("");

				sendMessage(t);

			}
		});

	}

	protected void sendMessage(String t) {
		Log.d("", "Send: " + t);
		try {
			chat.sendMessage(t);
		} catch (Exception e) {
			Log.e("", e.getMessage(), e);
		}
		dbHelper.addChatHistory(1, chat, t);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
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
			}
		});

	}

	public void setDbHelper(MessengerDatabaseHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

}
