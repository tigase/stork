package org.tigase.messenger.phone.pro.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.database.Cursor;
import android.util.Log;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.Criteria;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.MessageArchivingModule;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.ResultSet;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.utils.RSM;

import java.util.Calendar;
import java.util.Date;

public class FetchMessageArchiveMAM
		implements Runnable {

	private final SessionObject sessionObject;
	private final Date startDate;
	private final XMPPService xmppService;

	public FetchMessageArchiveMAM(XMPPService xmppService, SessionObject sessionObject) {
		this.xmppService = xmppService;
		this.sessionObject = sessionObject;
		this.startDate = getLastMessageDate();
	}

	private void fetchHistory(final Jaxmpp jaxmpp, final Chat chat, Date date) {
		try {
			final MessageModule mm = jaxmpp.getModule(MessageModule.class);
			final MessageArchiveManagementModule mam = jaxmpp.getModule(MessageArchiveManagementModule.class);

			MessageArchiveManagementModule.Query q = new MessageArchiveManagementModule.Query();
			q.setStart(date);
			q.setWith(chat.getWithJid());

			final RSM rsm = new RSM();
			final String queryId = UIDGenerator.next() + UIDGenerator.next() + UIDGenerator.next();

			mam.queryItems(q, queryId, rsm, new MessageArchiveManagementModule.ResultCallback() {
				@Override
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
					Log.i("MAM", "ERROR on retrieve " + error);
				}

				@Override
				public void onSuccess(String queryid, boolean complete, RSM rsm) throws JaxmppException {
					Log.i("MAM", "Done");
				}

				@Override
				public void onTimeout() throws JaxmppException {
					Log.i("MAM", "TIMEOUT on retrieve ");

				}
			});

//
//			mam.retrieveCollection(cr, new MessageArchivingModule.ItemsAsyncCallback() {
//				@Override
//				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
//					Log.i("MAM", "ERROR on retrieve " + error);
//
//				}
//
//				@Override
//				protected void onItemsReceived(final ChatResultSet chatItems) throws XMLException {
//					Iterator<ChatItem> it = chatItems.getItems().iterator();
//					if (it.hasNext()) {
//						it.next();
//					}
//					try {
//						final tigase.jaxmpp.core.client.xmpp.modules.chat.Chat co = getOrCreate(mm, chat);
//						while (it.hasNext()) {
//							final ChatItem chatItem = it.next();
//							try {
//								String author;
//								final ContentValues values = new ContentValues();
//								if (chatItem.getType() == ChatItem.Type.FROM) {
//									author = chat.getWithJid().toString();
//									values.put(DatabaseContract.ChatHistory.FIELD_STATE,
//											   DatabaseContract.ChatHistory.STATE_INCOMING);
//								} else {
//									author = sessionObject.getUserBareJid().toString();
//									values.put(DatabaseContract.ChatHistory.FIELD_STATE,
//											   DatabaseContract.ChatHistory.STATE_OUT_SENT);
//								}
//
//								values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, author);
//								values.put(DatabaseContract.ChatHistory.FIELD_JID, chat.getWithJid().toString());
//								values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, chatItem.getDate().getTime());
//								values.put(DatabaseContract.ChatHistory.FIELD_BODY, chatItem.getBody());
//								values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
//										   sessionObject.getUserBareJid().toString());
//
//								Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + chat
//										.getWithJid()
//										.getBareJid());
//
//								uri = xmppService.getContentResolver().insert(uri, values);
//								xmppService.getApplicationContext()
//										   .getContentResolver()
//										   .notifyChange(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI,
//																					co.getId()), null);
//							} catch (Exception e) {
//								Log.e("MAM", "Cannot insert message to history?", e);
//							}
//						}
//					} catch (Exception e) {
//						Log.e("MAM", "Cannot fetch?", e);
//					}
//				}
//
//				@Override
//				public void onTimeout() throws JaxmppException {
//					Log.i("MAM", "TIMEOUT on retrieve");
//				}
//			});
		} catch (Exception e) {
			Log.e(XMPPService.TAG, "Exception while Fetching Messages Archive on connect for account " +
					sessionObject.getUserBareJid().toString());
		}
	}

	private Date getLastMessageDate() {
		long d1 = getLastMessageDateFromDB();
		long d2 = getLastMessageDateFromAccount();

		long d = Math.max(d1, d2);

		if (d == -1) {
			Calendar now = Calendar.getInstance();
			now.add(Calendar.DATE, -14);
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

	@Override
	public void run() {
		Log.i(XMPPService.TAG, "Fetching Messages Archive. Account=" + sessionObject.getUserBareJid());
		try {
			final Jaxmpp jaxmpp = xmppService.multiJaxmpp.get(sessionObject);
			final MessageArchivingModule mam = jaxmpp.getModule(MessageArchivingModule.class);

			Criteria cr = new Criteria().setStart(startDate);

			mam.listCollections(cr, new MessageArchivingModule.CollectionAsyncCallback() {
				@Override
				protected void onCollectionReceived(ResultSet<Chat> chats) throws XMLException {
					Log.i("MAM", "SUCCESS");
					for (Chat chat : chats.getItems()) {
						fetchHistory(jaxmpp, chat, startDate);
					}
				}

				@Override
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
					Log.i("MAM", "ERROR " + error);
				}

				@Override
				public void onTimeout() throws JaxmppException {
					Log.i("MAM", "TIMEOUT");
				}
			});

//			mam.retrieveCollection(cr, new MessageArchivingModule.ItemsAsyncCallback() {
//				@Override
//				protected void onItemsReceived(ChatResultSet chat) throws XMLException {
//					Log.i("MAM", "SUCCESS");
//
//					chat.
//
//				}
//
//				@Override
//				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
//					Log.i("MAM", "ERROR " + error);
//				}
//
//				@Override
//				public void onTimeout() throws JaxmppException {
//					Log.i("MAM", "TIMEOUT");
//				}
//			});

//			try (Cursor cursor = xmppService.getContentResolver()
//					.query(ChatProvider.OPEN_CHATS_URI, cols, null, null, null)) {
//				while (cursor.moveToNext()) {
//					String acc = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
//					if (!acc.equals(sessionObject.getUserBareJid().toString())) {
//						continue;
//					}
//					String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
//					System.out.println(acc + ":  " + cursor);
//
//					Criteria crit = new Criteria().setWith(JID.jidInstance(jid));
//
//					mam.listCollections(crit, new MessageArchivingModule.CollectionAsyncCallback() {
//						@Override
//						protected void onCollectionReceived(ResultSet<Chat> vcard) throws XMLException {
//							Log.i("MAM", "SUCCESS");
//						}
//
//						@Override
//						public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
//								throws JaxmppException {
//							Log.i("MAM", "ERROR " + error);
//						}
//
//						@Override
//						public void onTimeout() throws JaxmppException {
//							Log.i("MAM", "TIMEOUT");
//						}
//					});
//
//				}
//			}

			// get archive for each

		} catch (Exception e) {
			Log.e(XMPPService.TAG, "Exception while Fetching Messages Archive on connect for account " +
					sessionObject.getUserBareJid().toString());
		}
	}
}
