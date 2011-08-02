package org.tigase.mobile;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class ChatFragment extends Fragment {

	private Chat chat;
	private SimpleCursorAdapter adapter;

	public static ChatFragment newInstance(Chat chat, SimpleCursorAdapter ad) {
		ChatFragment f = new ChatFragment();
		f.chat = chat;
		f.adapter = ad;
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ChatView layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();
		layout.setChat(chat);
		layout.setCursorAdapter(adapter);

		return layout;
	}

}
