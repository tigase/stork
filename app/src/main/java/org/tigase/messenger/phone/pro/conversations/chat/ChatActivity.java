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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.jaxmpp.android.chat.MarkAsRead;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.PresenceIconMapper;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class ChatActivity
		extends AbstractConversationActivity {

	private final ContactPresenceChangeObserver contactPresenceChangeObserver = new ContactPresenceChangeObserver();
	TextView mContactName;
	ImageView mContactPresence;
	private Uri contactUri;
	private MarkAsRead markAsRead;
	private int openChatId;
	private Integer rosterId;

	private void doUploadFile(Intent data, int resultCode) {
		Intent ssIntent = new Intent(getApplicationContext(), XMPPService.class);
		ssIntent.setAction(MessageSender.SEND_CHAT_MESSAGE_ACTION);
		ssIntent.putExtra(MessageSender.ACCOUNT, getAccount().toString());
		ssIntent.putExtra(MessageSender.LOCAL_CONTENT_URI, data.getData());
		ssIntent.putExtra(MessageSender.CHAT_ID, openChatId);

		getApplicationContext().startService(ssIntent);
	}

	public int getOpenChatId() {
		return openChatId;
	}

	private void loadContact() {
		final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
										   DatabaseContract.OpenChats.FIELD_ACCOUNT,
										   DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME,
										   ChatProvider.FIELD_UNREAD_COUNT, DatabaseContract.OpenChats.FIELD_TYPE,
										   ChatProvider.FIELD_CONTACT_PRESENCE, ChatProvider.FIELD_LAST_MESSAGE};
		Cursor c = getContentResolver().query(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, openChatId), cols,
											  null, null, null);
		try {
			if (c.moveToNext()) {
				String displayName = c.getString(c.getColumnIndex(ChatProvider.FIELD_NAME));

				mContactName.setText(displayName);
			} else {
				mContactName.setText(getJid().getBareJid().toString());
			}
		} finally {
			c.close();
		}
	}

	private Integer loadRosterID(BareJID account, BareJID jid) {
		Uri u = Uri.parse(RosterProvider.ROSTER_URI + "/" + account + "/" + jid);
		Cursor c = getContentResolver().query(u, new String[]{DatabaseContract.RosterItemsCache.FIELD_ID}, null, null,
											  null);
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
		if (contactUri == null) {
			return;
		}
		Cursor contactCursor = getContentResolver().query(contactUri,
														  new String[]{DatabaseContract.RosterItemsCache.FIELD_STATUS},
														  null, null, null);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_UPLOAD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			doUploadFile(data, resultCode);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.openChatId = getIntent().getIntExtra("openChatId", Integer.MIN_VALUE);
		setJid(JID.jidInstance(getIntent().getStringExtra("jid")));
		setAccount(BareJID.bareJIDInstance(getIntent().getStringExtra("account")));
		this.rosterId = loadRosterID(getAccount(), getJid().getBareJid());

		this.contactUri = rosterId == null ? null : ContentUris.withAppendedId(RosterProvider.ROSTER_URI, rosterId);

		if (contactUri != null) {
			getContentResolver().registerContentObserver(contactUri, true, contactPresenceChangeObserver);
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		mContactName = (TextView) findViewById(R.id.contact_display_name);
		mContactPresence = (ImageView) findViewById(R.id.contact_presence);

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

		this.markAsRead = new MarkAsRead(this);
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
		MessageNotification.cancelSummaryNotification(this);
		loadContact();
		loadUserPresence();

		markAsRead.markChatAsRead(this.openChatId, this.getAccount(), this.getJid());
	}

	private class ContactPresenceChangeObserver
			extends ContentObserver {

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
			loadUserPresence();
		}
	}

}
