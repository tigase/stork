/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package org.tigase.messenger.jaxmpp.android.chat;

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

	public void markChatAsRead(final long chatId, final BareJID account, final JID jid) {
		intMarkAsRead(ChatProvider.CHAT_HISTORY_URI, chatId, account, jid);
	}

	public void markGroupchatAsRead(long openChatId, BareJID account, JID jid) {
		intMarkAsRead(ChatProvider.MUC_HISTORY_URI, openChatId, account, jid);
	}

	private void intMarkAsRead(final Uri u, final long chatId, final BareJID account, final JID jid) {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final Uri uri = Uri.parse(u + "/" + account + "/" + jid.getBareJid());

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_INCOMING);

				try (Cursor c = context.getContentResolver()
						.query(uri, new String[]{DatabaseContract.ChatHistory.FIELD_ID,
												 DatabaseContract.ChatHistory.FIELD_STATE},
							   DatabaseContract.ChatHistory.FIELD_STATE + "=?",
							   new String[]{"" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD}, null)) {
					while (c.moveToNext()) {
						final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
						Uri u = ContentUris.withAppendedId(uri, id);

						int x = context.getContentResolver().update(u, values, null, null);
						Log.d("MarkAsRead", "Found unread (" + x + ") " + u);
					}
				} catch (Exception e) {
					Log.e("MarkAsRead", "Can't mark as read acc=" + account + ", jid=" + jid.getBareJid(), e);
				}

				context.getContentResolver()
						.notifyChange(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chatId), null);

				return null;
			}
		}).execute();
	}
}
