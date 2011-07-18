package org.tigase.mobile;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ChatActivity extends Activity {

	private Chat chat;

	private EditText messangeEntry;

	private LinearLayout sendSection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);

		this.messangeEntry = (EditText) findViewById(R.id.chat_message_entry);
		this.messangeEntry.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					sendMessage();
					return true;
				}
				return false;
			}
		});

		this.sendSection = (LinearLayout) findViewById(R.id.chat_send_section);
		final Button sendButton = (Button) findViewById(R.id.chat_send_button);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});

	}

	private void sendMessage() {
		messangeEntry.setText("");

	}

	public void setChat(final Chat chat) {
		this.chat = chat;
		sendSection.setEnabled(this.chat != null);
	}
}
