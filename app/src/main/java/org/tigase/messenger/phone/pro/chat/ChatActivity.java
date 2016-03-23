/*
 * ChatActivity.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.chat;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.chat.dummy.DummyContent;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity implements ChatItemFragment.OnListFragmentInteractionListener {

	@Bind(R.id.contact_display_name)
	TextView mContactName;
	private JID jid;
	private BareJID account;
	private int openChatId;

	public BareJID getAccount() {
		return account;
	}

	public JID getJid() {
		return jid;
	}

	public int getOpenChatId() {
		return openChatId;
	}

	private void loadContact() {
		final String[] cols = new String[] { DatabaseContract.OpenChats.FIELD_ID, DatabaseContract.OpenChats.FIELD_ACCOUNT,
				DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME, ChatProvider.FIELD_UNREAD_COUNT,
				DatabaseContract.OpenChats.FIELD_TYPE, ChatProvider.FIELD_STATE, ChatProvider.FIELD_LAST_MESSAGE };
		Cursor c = getContentResolver().query(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, openChatId), cols, null,
				null, null);
		try {
			c.moveToNext();
			String displayName = c.getString(c.getColumnIndex(ChatProvider.FIELD_NAME));

			mContactName.setText(displayName);
		} finally {
			c.close();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.openChatId = getIntent().getIntExtra("openChatId", Integer.MIN_VALUE);
		this.jid = JID.jidInstance(getIntent().getStringExtra("jid"));
		this.account = BareJID.bareJIDInstance(getIntent().getStringExtra("account"));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		ButterKnife.bind(this);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setVisibility(View.GONE);
		// fab.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View view) {
		// Snackbar.make(view, "Replace with your own action",
		// Snackbar.LENGTH_LONG).setAction("Action", null).show();
		// }
		// });
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public void onListFragmentInteraction(DummyContent.DummyItem item) {

	}

	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(("chat:" + openChatId).hashCode());

		loadContact();
	}

}
