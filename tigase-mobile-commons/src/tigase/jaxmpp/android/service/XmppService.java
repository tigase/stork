package tigase.jaxmpp.android.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class XmppService extends Service {

	private class ExtXmppModule implements XmppModule {

		private final Criteria criteria;
		private final String[] features;
		private final Messenger messenger;

		public ExtXmppModule(Messenger messenger, String[] features, Criteria criteria) {
			this.messenger = messenger;
			this.features = features;
			this.criteria = criteria;
		}

		@Override
		public Criteria getCriteria() {
			return criteria;
		}

		@Override
		public String[] getFeatures() {
			return features;
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {
			ParcelableElement parcelable = ParcelableElement.fromElement(element);

			Bundle bundle = new Bundle();
			bundle.putString(ACCOUNT_JID_KEY, element.getAttribute("to"));
			bundle.putParcelable(STANZA_KEY, parcelable);

			Message msg = Message.obtain(null, MSG_RECV_STANZA);
			msg.setData(bundle);
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
				Log.e(TAG, "Remote exception", e);
			}
		}

	}

	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case MSG_REGISTER_CLIENT:
					registerClient(msg);
					break;
				case MSG_UNREGISTER_CLIENT:
					unregisterClient(msg);
					break;
				case MSG_SEND_STANZA:
					sendStanza(msg);
					break;
				case MSG_ACCOUNTS_LIST_REQUEST:
					listAccounts(msg);
				case MSG_AVATAR_REQUEST:
					getAvatar(msg);
				default:
					Log.v(TAG, "unknown message type = " + msg.what);
				}
			} catch (Exception ex) {
				Log.e(TAG, "exception processing IPC message", ex);
			}
		}
	}

	public static final String ACCOUNT_JID_KEY = "accountJid#key";

	public static final String ACCOUNTS_LIST_KEY = "accountsList#key";
	public static final String AVATAR_JID_KEY = "avatarJid#key";
	public static final String AVATAR_KEY = "avatar#key";
	public static final String CRITERIA_KEY = "criteria#key";
	public static final String FEATURES_KEY = "features#key";
	public static final int MSG_ACCOUNTS_LIST_REQUEST = 5;

	public static final int MSG_ACCOUNTS_LIST_RESPONSE = 6;
	public static final int MSG_AVATAR_REQUEST = 7;
	public static final int MSG_AVATAR_RESPONSE = 8;
	public static final int MSG_RECV_STANZA = 4;
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_SEND_STANZA = 3;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final String REQUEST_CALLBACK_KEY = "requestCallback#key";

	public static final String STANZA_KEY = "stanza#key";

	private static final String TAG = "XmppService";

	private Map<Messenger, ExtXmppModule> clients = Collections.synchronizedMap(new HashMap<Messenger, ExtXmppModule>());

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	protected abstract Collection<JaxmppCore> getAccounts();

	protected abstract Bitmap getAvatar(BareJID jid);

	private void getAvatar(Message msg) {
		int id = msg.arg1;
		Bundle data = msg.getData();
		data.setClassLoader(this.getClassLoader());
		BareJID jid = BareJID.bareJIDInstance(data.getString(AVATAR_JID_KEY));
		Bitmap image = getAvatar(jid);
		Avatar avatar = new Avatar(jid, image);

		Messenger messenger = msg.replyTo;
		msg = Message.obtain(null, MSG_AVATAR_RESPONSE);
		msg.arg1 = id;
		data = new Bundle();
		data.putParcelable(AVATAR_KEY, avatar);
		msg.setData(data);
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}
	}

	protected abstract void init();

	private void listAccounts(Message msg) {
		int id = msg.arg1;
		Collection<JaxmppCore> accounts = getAccounts();
		ArrayList<Account> list = new ArrayList<Account>();
		for (JaxmppCore jaxmpp : accounts) {
			Account acc = new Account(jaxmpp.getSessionObject().getUserBareJid(), jaxmpp.isConnected());
			list.add(acc);
		}
		Messenger messenger = msg.replyTo;
		msg = Message.obtain(null, MSG_ACCOUNTS_LIST_RESPONSE);
		msg.arg1 = id;
		Bundle data = new Bundle();
		data.putParcelableArrayList(ACCOUNTS_LIST_KEY, list);
		msg.setData(data);
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		init();
		return mMessenger.getBinder();
	}

	private void registerClient(Message msg) {
		Messenger messenger = msg.replyTo;
		Bundle bundle = msg.getData();
		if (bundle != null) {
			bundle.setClassLoader(getClassLoader());
			String[] features = bundle.getStringArray(FEATURES_KEY);
			Criteria criteria = bundle.getParcelable(CRITERIA_KEY);
			ExtXmppModule module = new ExtXmppModule(messenger, features, criteria);
			clients.put(messenger, module);
			registerModule(module);
		}
	}

	protected abstract void registerModule(XmppModule module);

	protected abstract void sendStanza(BareJID account, Element element, boolean requestCallback, XmppModule module);

	private void sendStanza(Message msg) {
		Bundle bundle = msg.getData();
		if (bundle != null) {
			bundle.setClassLoader(this.getClassLoader());

			XmppModule module = null;

			String accountStr = bundle.getString(ACCOUNT_JID_KEY);
			BareJID account = BareJID.bareJIDInstance(accountStr);
			Element element = bundle.getParcelable(STANZA_KEY);
			Boolean requestCallback = bundle.getBoolean(REQUEST_CALLBACK_KEY);
			if (requestCallback == true) {
				module = clients.get(msg.replyTo);
			}
			sendStanza(account, element, requestCallback, module);
		}
	}

	private void unregisterClient(Message msg) {
		Messenger messenger = msg.replyTo;
		ExtXmppModule module = clients.remove(messenger);
		if (module != null) {
			unregisterModule(module);
		}
	}

	protected abstract void unregisterModule(XmppModule module);
}
