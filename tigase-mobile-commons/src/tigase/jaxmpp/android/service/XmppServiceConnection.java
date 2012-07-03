package tigase.jaxmpp.android.service;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
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
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case XmppService.MSG_RECV_STANZA:
					Bundle bundle = msg.getData();
					if (bundle != null) {
						bundle.setClassLoader(this.getClass().getClassLoader());
						String account = bundle.getString(XmppService.ACCOUNT_JID_KEY);
						ParcelableElement element = bundle.getParcelable(XmppService.STANZA_KEY);
						try {
							XmppServiceConnection.this.process(element);
						} catch (JaxmppException e) {
							Log.v(TAG, "processing stanza exception", e);
						}
					}
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

	public void send(String account, Element element, boolean requestCallback) {
		try {
			Message msg = Message.obtain(null, XmppService.MSG_SEND_STANZA);
			msg.replyTo = mMessenger;
	
			Bundle bundle = new Bundle();
			bundle.putString(XmppService.ACCOUNT_JID_KEY, account);
			bundle.putParcelable(XmppService.STANZA_KEY, ParcelableElement.fromElement(element));
			bundle.putBoolean(XmppService.REQUEST_CALLBACK_KEY, requestCallback);
			msg.setData(bundle);
			
			mService.send(msg);
		}
		catch (JaxmppException e) {
			Log.e(TAG, "Jaxmpp exception", e);
		}
		catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}		
	}
}
