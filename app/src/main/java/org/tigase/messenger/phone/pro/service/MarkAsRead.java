package org.tigase.messenger.phone.pro.service;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class MarkAsRead {

	private final Context context;

	public MarkAsRead(Context context) {
		this.context = context.getApplicationContext();
	}


	private void intMarkAsRead(final Uri u, final int chatId, final BareJID account, final JID jid) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final Uri uri = Uri.parse(u + "/" + account + "/" + jid);

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_INCOMING);

				Cursor c = context.getContentResolver().query(uri,
						new String[]{DatabaseContract.ChatHistory.FIELD_ID, DatabaseContract.ChatHistory.FIELD_STATE},
						DatabaseContract.ChatHistory.FIELD_STATE + "=?", new String[]{"" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD}, null);
				try {
					while (c.moveToNext()) {
						final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
						Uri u = ContentUris.withAppendedId(uri, id);

						int x = context.getContentResolver().update(u, values, null, null);

						Log.d("MarkAsRead", "Found unread (" + x + ") " + u);
					}
				} finally {
					c.close();
				}


				return null;
			}
		}).execute();
	}

	public void markChatAsRead(final int chatId, final BareJID account, final JID jid) {
		intMarkAsRead(ChatProvider.CHAT_HISTORY_URI, chatId, account, jid);
	}

	public void markGroupchatAsRead(int openChatId, BareJID account, JID jid) {
		intMarkAsRead(ChatProvider.MUC_HISTORY_URI, openChatId, account, jid);
	}
}
