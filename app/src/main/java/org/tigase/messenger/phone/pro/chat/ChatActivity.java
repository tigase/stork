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
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.PresenceIconMapper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity {

	private final ContactPresenceChangeObserver contactPresenceChangeObserver = new ContactPresenceChangeObserver();
	@Bind(R.id.contact_display_name)
	TextView mContactName;
	@Bind(R.id.contact_presence)
	ImageView mContactPresence;
	private JID jid;
	private BareJID account;
	private int openChatId;
	private Uri contactUri;
	private Integer rosterId;

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
			if (c.moveToNext()) {
				String displayName = c.getString(c.getColumnIndex(ChatProvider.FIELD_NAME));

				mContactName.setText(displayName);
			} else {
				mContactName.setText(jid.getBareJid().toString());
			}
		} finally {
			c.close();
		}
	}

	private Integer loadRosterID(BareJID account, BareJID jid) {
		Uri u = Uri.parse(RosterProvider.ROSTER_URI + "/" + account + "/" + jid);
		Cursor c = getContentResolver().query(u, new String[] { DatabaseContract.RosterItemsCache.FIELD_ID }, null, null, null);
		try {
			if (c.moveToNext()) {
				return c.getInt(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
			}
		} finally {
			c.close();
		}
		return null;
	}

	private void loadUserPresence() {
		if (contactUri == null)
			return;
		Cursor contactCursor = getContentResolver().query(contactUri,
				new String[] { DatabaseContract.RosterItemsCache.FIELD_STATUS }, null, null, null);
		try {
			if (contactCursor.moveToNext()) {
				final int status = contactCursor.getInt(
						contactCursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_STATUS));
				mContactPresence.setVisibility(View.VISIBLE);
				mContactPresence.setImageResource(PresenceIconMapper.getPresenceResource(status));
			} else {
				mContactPresence.setVisibility(View.INVISIBLE);
			}
		} finally {
			contactCursor.close();
		}
	}

	private void makeMessagesRead() {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				// Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" +
				// getAccount() + "/"
				// + getJid());
				//
				// ContentValues values = new ContentValues();
				// values.put(DatabaseContract.ChatHistory.FIELD_STATE,
				// DatabaseContract.ChatHistory.ST);
				// getContentResolver().update(uri, values, null,
				// null);

				return null;
			}
		}).execute();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.openChatId = getIntent().getIntExtra("openChatId", Integer.MIN_VALUE);
		this.jid = JID.jidInstance(getIntent().getStringExtra("jid"));
		this.account = BareJID.bareJIDInstance(getIntent().getStringExtra("account"));
		this.rosterId = loadRosterID(account, jid.getBareJid());

		this.contactUri = rosterId == null ? null : ContentUris.withAppendedId(RosterProvider.ROSTER_URI, rosterId);

		if (contactUri != null) {
			getContentResolver().registerContentObserver(contactUri, true, contactPresenceChangeObserver);
		}

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
	protected void onDestroy() {
		getContentResolver().unregisterContentObserver(contactPresenceChangeObserver);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(("chat:" + openChatId).hashCode());

		loadContact();
		loadUserPresence();

		makeMessagesRead();
	}

	private class ContactPresenceChangeObserver extends ContentObserver {
		public ContactPresenceChangeObserver() {
			super(new Handler());
		}

		@Override
		public boolean deliverSelfNotifications() {
			Log.i("ChatActivity", "deliverSelfNotifications");
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.i("ChatActivity", "onChange " + selfChange);
			Toast.makeText(getApplicationContext(), "GOT IT!", Toast.LENGTH_LONG);
			loadUserPresence();
		}
	}

}
