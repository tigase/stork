package tigase.jaxmpp.android.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public abstract class XmppService extends Service {

	private static final String TAG = "XmppService";
	
	public static final String FEATURES_KEY = "features#key";
	public static final String CRITERIA_KEY = "criteria#key";
	
	public static final String ACCOUNT_JID_KEY = "accountJid#key";
	public static final String STANZA_KEY = "stanza#key";
	public static final String REQUEST_CALLBACK_KEY = "requestCallback#key";
	
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SEND_STANZA = 3;
	public static final int MSG_RECV_STANZA = 4;
	
	
	private Map<Messenger,ExtXmppModule> clients = Collections.synchronizedMap(new HashMap<Messenger,ExtXmppModule>());
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
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
				default:
					Log.v(TAG, "unknown message type = " + msg.what);
			}
		}		
	}
	
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	@Override
	public IBinder onBind(Intent intent) {
		init();
		return mMessenger.getBinder();
	}
	
	protected abstract void init();
	protected abstract void registerModule(XmppModule module);
	protected abstract void unregisterModule(XmppModule module);
	protected abstract void sendStanza(BareJID account, Element element, boolean requestCallback, XmppModule module);
	
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
	
	private void unregisterClient(Message msg) {
		Messenger messenger = msg.replyTo;
		ExtXmppModule module = clients.remove(messenger);
		if (module != null) {
			unregisterModule(module);
		}
	}

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
	
	private class ExtXmppModule implements XmppModule {
		
		private final Messenger messenger;
		private final String[] features;
		private final Criteria criteria;
		
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
}
