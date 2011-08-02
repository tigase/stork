package org.tigase.mobile;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ChatView extends LinearLayout {

	private Chat chat;

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChatView(Context context) {
		super(context);
	}

	public Chat getChat() {
		return chat;
	}

	public void setChat(Chat chat) {
		this.chat = chat;
	}

}
