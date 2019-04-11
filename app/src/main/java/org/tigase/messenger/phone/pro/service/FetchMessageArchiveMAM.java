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

package org.tigase.messenger.phone.pro.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.database.Cursor;
import android.util.Log;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.Chat;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.utils.RSM;

import java.util.Calendar;
import java.util.Date;

public class FetchMessageArchiveMAM
		implements Runnable {

	private final static String TAG = "MAMFetcher";

	private final SessionObject sessionObject;
	private final XMPPService xmppService;
	private Account account;
	private JaxmppCore jaxmpp;
	private AccountManager mAccountManager;
	private MessageArchiveManagementModule mam;
	private Date startDate;

	public FetchMessageArchiveMAM(XMPPService xmppService, SessionObject sessionObject) {
		this.xmppService = xmppService;
		this.sessionObject = sessionObject;
	}

	@Override
	public void run() {
		Log.i(XMPPService.TAG, "Fetching Messages Archive. Account=" + sessionObject.getUserBareJid());
		try {
			this.mAccountManager = AccountManager.get(xmppService);
			this.account = AccountHelper.getAccount(mAccountManager, sessionObject.getUserBareJid().toString());
			this.startDate = getLastMessageDate();

			String autoSync = mAccountManager.getUserData(account, AccountsConstants.MAM_AUTOSYNC);
			if (autoSync != null && !Boolean.parseBoolean(autoSync)) {
				Log.i(XMPPService.TAG, "Autosync disabled. Skip.");
				return;
			}

			if (startDate == null) {
				Log.i(XMPPService.TAG, "Start date set to Never. Skip.");
				return;
			}

			this.jaxmpp = xmppService.multiJaxmpp.get(sessionObject);
			this.mam = jaxmpp.getModule(MessageArchiveManagementModule.class);

			fetch(null);
		} catch (Exception e) {
			Log.e(XMPPService.TAG, "Exception while Fetching Messages Archive on connect for account " +
					sessionObject.getUserBareJid().toString());
		}
	}

	private void fetch(final String beforeId) throws JaxmppException {
		final String queryId = "mam-" + UIDGenerator.next();

		final RSM rsm = new RSM();
		rsm.setMax(100);

		if (beforeId == null) {
			rsm.setLastPage(true);
		} else {
			rsm.setBefore(beforeId);
		}

		final MessageArchiveManagementModule.Query query = new MessageArchiveManagementModule.Query();
		query.setStart(startDate);

		mam.queryItems(query, queryId, rsm, new MessageArchiveManagementModule.ResultCallback() {
			@Override
			public void onSuccess(String queryid, boolean complete, RSM rsm) throws JaxmppException {
				Log.i(TAG, "Query " + queryId + " complete=" + complete + "; first=" + rsm.getFirst() + "; last=" +
						rsm.getLast() + "; count=" + rsm.getCount());

				if (!complete) {
					fetch(rsm.getLast());
				}
			}

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				Log.i(TAG, "Query " + queryId + " error:" + error);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.i(TAG, "Query " + queryId + " timeout.");
			}
		});
	}

	private Date getLastMessageDate() {
//		long d1 = getLastMessageDateFromDB();
		long d = getLastMessageDateFromAccount();

		if (d == -1) {
			String syncTime = mAccountManager.getUserData(account, AccountsConstants.MAM_SYNC_TIME);

			int hours = syncTime == null ? 24 : Integer.valueOf(syncTime);

			if (hours == 0) {
				return null;
			}

			Calendar now = Calendar.getInstance();
			now.add(Calendar.HOUR, -hours);
			return now.getTime();
		} else {
			return new Date(d);
		}
	}

	private long getLastMessageDateFromAccount() {
		AccountManager mAccountManager = AccountManager.get(xmppService);
		Account ac = AccountHelper.getAccount(mAccountManager, sessionObject.getUserBareJid().toString());

		String tmp = mAccountManager.getUserData(ac, AccountsConstants.FIELD_LAST_ACTIVITY);
		if (tmp == null) {
			return -1;
		}
		return Long.parseLong(tmp);
	}

	private long getLastMessageDateFromDB() {
		final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
										   DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		try (Cursor cursor = xmppService.getContentResolver()
				.query(ChatProvider.CHAT_HISTORY_URI, cols, null, null,
					   DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC")) {
			if (cursor.moveToNext()) {
				long t = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
				return t;
			}
		}
		return -1;
	}

	private tigase.jaxmpp.core.client.xmpp.modules.chat.Chat getOrCreate(MessageModule mm, final Chat chat)
			throws JaxmppException {
		tigase.jaxmpp.core.client.xmpp.modules.chat.Chat result = mm.getChatManager().getChat(chat.getWithJid(), null);
		if (result == null) {
			result = mm.createChat(chat.getWithJid());
		}
		return result;
	}
}
