/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.jaxmpp.android.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

public abstract class XmppServiceConnection implements ServiceConnection, XmppModule {

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if (bundle != null) {
				bundle.setClassLoader(this.getClass().getClassLoader());
			}

			switch (msg.what) {
			case XmppService.MSG_RECV_STANZA:
				String account = bundle.getString(XmppService.ACCOUNT_JID_KEY);
				ParcelableElement element = bundle.getParcelable(XmppService.STANZA_KEY);
				try {
					XmppServiceConnection.this.process(element);
				} catch (JaxmppException e) {
					Log.v(TAG, "processing stanza exception", e);
				}
				break;
			case XmppService.MSG_ACCOUNTS_LIST_RESPONSE:
				tigase.jaxmpp.android.service.Callback<List<Account>> callbackAccounts = getCallback(msg.arg1);
				ArrayList<Account> accounts = bundle.getParcelableArrayList(XmppService.ACCOUNTS_LIST_KEY);
				callbackAccounts.onResult(accounts);
				break;
			case XmppService.MSG_AVATAR_RESPONSE:
				tigase.jaxmpp.android.service.Callback<Avatar> callbackAvatar = getCallback(msg.arg1);
				Avatar avatar = bundle.getParcelable(XmppService.AVATAR_KEY);
				callbackAvatar.onResult(avatar);
				break;
			default:
				Log.v(TAG, "unknown message type = " + msg.what);
			}
		}
	}

	private static final String TAG = "XmppServiceConnection";
	private Map<Integer, Callback> callbacks = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) ? new ConcurrentHashMap<Integer, Callback>()
			: Collections.synchronizedMap(new HashMap<Integer, Callback>());
	// used to generate local id for callbacks
	private int id = 1;

	private Messenger mMessenger = new Messenger(new IncomingHandler());

	private Messenger mService = null;

	private String xmppServiceClass = null;

	public XmppServiceConnection(String xmppServiceClass) {
		this.xmppServiceClass = xmppServiceClass;
	}

	public void doBindService(Context context) {
		Log.v(TAG, "requesting bind");
		Intent intent = new Intent(xmppServiceClass);
		// intent.setComponent(xmppServiceClass);
		Log.v(TAG, "requesting bind..");
		boolean result = context.getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.v(TAG, "requesting bind resulted = " + result);

	}

	public void doUnbindService(Context context) {
		if (mService != null) {
			try {
				Message msg = Message.obtain(null, XmppService.MSG_UNREGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				Log.e(TAG, "Remote exception", e);
			}

			context.getApplicationContext().unbindService(this);
			mService = null;
		}
	}

	private int generateNextId() {
		id++;
		if (id == 0)
			return generateNextId();
		return id;
	}

	public void getAccountsList(Callback<List<Account>> callback) throws RemoteException {
		int id = registerCallback(callback);
		Message msg = Message.obtain(null, XmppService.MSG_ACCOUNTS_LIST_REQUEST);
		msg.replyTo = mMessenger;
		msg.arg1 = id;
		mService.send(msg);
	}

	public void getAvatar(BareJID jid, Callback<Avatar> callback) throws RemoteException {
		int id = registerCallback(callback);
		Message msg = Message.obtain(null, XmppService.MSG_AVATAR_REQUEST);
		msg.replyTo = mMessenger;
		msg.arg1 = id;
		Bundle data = new Bundle();
		data.putString(XmppService.AVATAR_JID_KEY, jid.toString());
		msg.setData(data);
		mService.send(msg);
	}

	private <T> Callback<T> getCallback(int id) {
		if (id == 0)
			return null;
		return callbacks.get(id);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = new Messenger(service);
		try {
			Message msg = Message.obtain(null, XmppService.MSG_REGISTER_CLIENT);
			msg.replyTo = mMessenger;

			Bundle bundle = new Bundle();
			bundle.putStringArray(XmppService.FEATURES_KEY, getFeatures());
			bundle.putParcelable(XmppService.CRITERIA_KEY, (Parcelable) getCriteria());
			msg.setData(bundle);

			mService.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		mService = null;
	}

	private int registerCallback(Callback callback) {
		int id = generateNextId();
		callbacks.put(id, callback);
		return id;
	}

	public void send(BareJID account, Element element, boolean requestCallback) throws JaxmppException, RemoteException {
		Message msg = Message.obtain(null, XmppService.MSG_SEND_STANZA);
		msg.replyTo = mMessenger;

		Bundle bundle = new Bundle();
		bundle.putString(XmppService.ACCOUNT_JID_KEY, account.toString());
		bundle.putParcelable(XmppService.STANZA_KEY, ParcelableElement.fromElement(element));
		bundle.putBoolean(XmppService.REQUEST_CALLBACK_KEY, requestCallback);
		msg.setData(bundle);

		mService.send(msg);
	}
}
