package tigase.jaxmpp.android.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
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

	private static final String TAG = "XmppServiceConnection";
	
	private String xmppServiceClass = null;
	private Messenger mService = null;
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	
	private Map<Integer,Callback> callbacks = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) ? 
			new ConcurrentHashMap<Integer,Callback>() : Collections.synchronizedMap(new HashMap<Integer,Callback>());
	
	// used to generate local id for callbacks
	private int id = 1;
	
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
	
	public XmppServiceConnection(String xmppServiceClass) {
		this.xmppServiceClass = xmppServiceClass;
	}
	
	public void doBindService(Context context) {
		Log.v(TAG, "requesting bind");
		Intent intent = new Intent(xmppServiceClass);
//		intent.setComponent(xmppServiceClass);
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
			}
			catch (RemoteException e) {
				Log.e(TAG, "Remote exception", e);
			}
			
			context.getApplicationContext().unbindService(this);
			mService = null;
		}
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
		}
		catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		mService = null;
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
	
	private int generateNextId() {
		id++;
		if (id == 0) return generateNextId();
		return id;
	}
	
	private int registerCallback(Callback callback) {
		int id = generateNextId();
		callbacks.put(id, callback);
		return id;
	}
	
	private <T> Callback<T> getCallback(int id) {
		if (id == 0) return null;
		return (Callback<T>) callbacks.get(id);
	}
}
