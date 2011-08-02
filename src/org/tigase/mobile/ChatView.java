package org.tigase.mobile;

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
import android.widget.SimpleCursorAdapter;

public class ChatView extends LinearLayout {

	private Chat chat;

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
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

	public ChatView(Context context) {
		super(context);
	}

	protected void sendMessage(String t) {
		Log.d("", "Send: " + t);
		// chat.sendMessage(t);

	}

	public Chat getChat() {
		return chat;
	}

	public void setChat(Chat chat) {
		this.chat = chat;
	}

	public void setCursorAdapter(ListAdapter adapter) {
		ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.setAdapter(adapter);
	}

}
