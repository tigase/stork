package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class ChatFragment extends Fragment {

	public static ChatFragment newInstance(Chat chat, SimpleCursorAdapter ad, MessengerDatabaseHelper dbHelper) {
		ChatFragment f = new ChatFragment();
		f.chat = chat;
		f.adapter = ad;
		f.dbHelper = dbHelper;
		return f;
	}

	private SimpleCursorAdapter adapter;
	private Chat chat;

	private MessengerDatabaseHelper dbHelper;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ChatView layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.init();
		layout.setChat(chat);
		layout.setDbHelper(dbHelper);
		layout.setCursorAdapter(adapter);

		return layout;
	}

	@Override
	public void onStart() {
		super.onStart();

		Log.i("CHAT Fragment", "Started");

	}

	@Override
	public void onStop() {
		super.onStop();

		Log.i("CHAT Fragment", "Stopped");

	}

}
