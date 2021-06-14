/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.conversations.chat;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.whispersystems.libsignal.SignalProtocolAddress;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.JaXMPPSignalProtocolStore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.XmppOMEMOSession;

public class OMEMOStartService
		extends IntentService
		implements ServiceConnection {

	public static final String ACTION_ENABLE = "OMEMOStartService.ENABLE";
	public static final String ACTION_DISABLE = "OMEMOStartService.DISABLE";
	public static final String EXTRA_ACCOUNT = "OMEMOStartService.EXTRA_ACCOUNT";
	public static final String EXTRA_JID = "OMEMOStartService.EXTRA_JID";
	public static final String EXTRA_CHAT_ID = "OMEMOStartService.EXTRA_CHAT_ID";
	public static final String RESULT_ACTION = "OMEMOStartService.RESULT.BROADCAST";
	public static final String RESULT_CHAT_ID = "OMEMOStartService.RESULT.CHAT_ID";
	public static final String RESULT_TYPE = "OMEMOStartService.RESULT.TYPE";
	public static final int RESULT_TYPE_ENABLED = 1;
	public static final int RESULT_TYPE_DISABLED = 2;
	public static final int RESULT_TYPE_ERROR = 3;
	private static final String TAG = "OMEMOStartService";
	private final Object lock = new Object();
	private XMPPService mService;

	public static void startActionDisable(Context context, String accountName, JID jid, int chatId) {
		Intent intent = new Intent(context, OMEMOStartService.class);
		intent.setAction(ACTION_DISABLE);
		intent.putExtra(EXTRA_ACCOUNT, accountName);
		intent.putExtra(EXTRA_JID, jid.toString());
		intent.putExtra(EXTRA_CHAT_ID, chatId);
		context.startService(intent);
	}

	public static void startActionEnable(Context context, String accountName, JID jid, int chatId) {
		Intent intent = new Intent(context, OMEMOStartService.class);
		intent.setAction(ACTION_ENABLE);
		intent.putExtra(EXTRA_ACCOUNT, accountName);
		intent.putExtra(EXTRA_JID, jid.toString());
		intent.putExtra(EXTRA_CHAT_ID, chatId);
		context.startService(intent);
	}

	public OMEMOStartService() {
		super("OMEMOStartService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, XMPPService.class), this, 0);
	}

	@Override
	public void onDestroy() {
		unbindService(this);
		super.onDestroy();
	}

	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		XMPPService.LocalBinder binder = (XMPPService.LocalBinder) iBinder;
		this.mService = binder.getService();
		Log.w(TAG, "CONNECTED");
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {
		this.mService = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			synchronized (lock) {
				while (mService == null) {
					lock.wait();
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		Log.w(TAG, "HANDLE");

		if (intent != null) {
			final String action = intent.getAction();

			final String accountName = intent.getStringExtra(EXTRA_ACCOUNT);
			final BareJID jid = BareJID.bareJIDInstance(intent.getStringExtra(EXTRA_JID));
			final int chatId = intent.getIntExtra(EXTRA_CHAT_ID, -1);

			if (ACTION_ENABLE.equals(action)) {
				handleActionEnable(accountName, jid, chatId);
			} else if (ACTION_DISABLE.equals(action)) {
				handleActionDisable(accountName, jid, chatId);
			}
		}
	}

	private void sendResultOK(String accountName, BareJID jid, int chatId) {
		Intent intent = new Intent(RESULT_ACTION);
		intent.putExtra(RESULT_TYPE, RESULT_TYPE_ENABLED);
		intent.putExtra(RESULT_CHAT_ID, chatId);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void sendResultError(String accountName, BareJID jid, int chatId, String message) {
		Intent intent = new Intent(RESULT_ACTION);
		intent.putExtra(RESULT_TYPE, RESULT_TYPE_ERROR);
		intent.putExtra(RESULT_CHAT_ID, chatId);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionEnable(String accountName, BareJID jid, int chatId) {
		final JaxmppCore jaxmpp = mService.getJaxmpp(accountName);

		final JaXMPPSignalProtocolStore store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		final XmppOMEMOSession session = store.getSession(jid);

		if (session != null) {
			store.setOMEMORequired(jid, true);
			sendResultOK(accountName, jid, chatId);
		} else {
			createOMEMOSession(jaxmpp, jid, chatId, store);
		}
	}

	private void createOMEMOSession(JaxmppCore jaxmpp, BareJID jid, int chatId, JaXMPPSignalProtocolStore store) {
		try {
			jaxmpp.getModule(OmemoModule.class).createOMEMOSession(jid, new OmemoModule.CreateOMEMOSessionHandler() {
				@Override
				public void onError() {
					Log.w(TAG, "OMEMO Session is not created");
					sendResultError(jaxmpp.getSessionObject().getUserBareJid().toString(), jid, chatId, "");
				}

				@Override
				public void onSessionCreated(XmppOMEMOSession session) {
					Log.i(TAG, "OMEMO session is created");
					addOwnKeys(session, store, jaxmpp);

					store.setOMEMORequired(jid, true);
					sendResultOK(jaxmpp.getSessionObject().getUserBareJid().toString(), jid, chatId);
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "OMEMO Session is not created", e);
			sendResultError(jaxmpp.getSessionObject().getUserBareJid().toString(), jid, chatId, e.getMessage());
		}
	}

	private void addOwnKeys(XmppOMEMOSession session, JaXMPPSignalProtocolStore store, JaxmppCore jaxmpp) {
		final String jid = jaxmpp.getSessionObject().getUserBareJid().toString();
		for (int id : store.getSubDevice(jid)) {
			SignalProtocolAddress addr = new SignalProtocolAddress(jid, id);
			session.addDeviceCipher(store, addr);
		}
	}

	private void handleActionDisable(String accountName, BareJID jid, int chatId) {
		JaxmppCore jaxmpp = mService.getJaxmpp(accountName);
		final JaXMPPSignalProtocolStore store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		store.setOMEMORequired(jid, false);

		Intent intent = new Intent(
				RESULT_ACTION); //put the same message as in the filter you used in the activity when registering the receiver
		intent.putExtra(RESULT_TYPE, RESULT_TYPE_DISABLED);
		intent.putExtra(RESULT_CHAT_ID, chatId);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}