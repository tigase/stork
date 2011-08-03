package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.content.Context;
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
		t.setText("Chat with " + chat.getJid());
	}

	public void setCursorAdapter(ListAdapter adapter) {
		ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.setAdapter(adapter);
	}

	public void setDbHelper(MessengerDatabaseHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

}
