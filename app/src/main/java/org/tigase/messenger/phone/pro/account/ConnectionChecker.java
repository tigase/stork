package org.tigase.messenger.phone.pro.account;

import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.EventListener;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.content.Context;
import android.util.Log;

public class ConnectionChecker {

	private static final String TAG = "ConnectionChecker";
	private final Jaxmpp contact = new Jaxmpp();
	private final BareJID jid;
	private final String password;
	private final String hostname;
	protected String errorMessage;
	protected Throwable exception;
	private boolean passwordInvalid = false;

	public ConnectionChecker(String mXmppId, String mPassword, String mHostname) {
		this.jid = BareJID.bareJIDInstance(mXmppId);
		this.password = mPassword;
		this.hostname = mHostname;

		contact.getEventBus().addHandler(Connector.ErrorHandler.ErrorEvent.class, new Connector.ErrorHandler() {
			@Override
			public void onError(SessionObject sessionObject, StreamError condition, Throwable e) throws JaxmppException {
				Log.w(TAG, "onError() " + condition + "   " + e);
				if (exception == null)
					exception = e;
			}
		});

		contact.getEventBus().addListener(new EventListener() {

			@Override
			public void onEvent(Event<? extends EventHandler> event) {
				Log.i(TAG, "Event: " + event.getClass());
			}
		});
		contact.getEventBus().addHandler(AuthModule.AuthFailedHandler.AuthFailedEvent.class,
				new AuthModule.AuthFailedHandler() {

					@Override
					public void onAuthFailed(SessionObject sessionObject, SaslModule.SaslError error) throws JaxmppException {
						Log.w(TAG, "AuthFailedEvent() " + error);
						errorMessage = "Invalid username or password";
						passwordInvalid = true;
						// wakeup();
					}
				});
		contact.getEventBus().addHandler(InBandRegistrationModule.NotSupportedErrorHandler.NotSupportedErrorEvent.class,
				new InBandRegistrationModule.NotSupportedErrorHandler() {

					@Override
					public void onNotSupportedError(SessionObject sessionObject) throws JaxmppException {
						Log.w(TAG, "NotSupportedErrorHandler() ");
						errorMessage = "Registration not supported.";
						// wakeup();
					}
				});
		contact.getEventBus().addHandler(InBandRegistrationModule.ReceivedErrorHandler.ReceivedErrorEvent.class,
				new InBandRegistrationModule.ReceivedErrorHandler() {

					@Override
					public void onReceivedError(SessionObject sessionObject, IQ responseStanza,
							XMPPException.ErrorCondition errorCondition) throws JaxmppException {
						errorMessage = "Error during registration.";
						// wakeup();
					}
				});
		contact.getEventBus().addHandler(JaxmppCore.ConnectedHandler.ConnectedEvent.class, new JaxmppCore.ConnectedHandler() {
			@Override
			public void onConnected(SessionObject sessionObject) {
				Log.w(TAG, "Jaxmpp connected");
				wakeup();
			}
		});
		contact.getEventBus().addHandler(JaxmppCore.DisconnectedHandler.DisconnectedEvent.class,
				new JaxmppCore.DisconnectedHandler() {

					@Override
					public void onDisconnected(SessionObject sessionObject) {
						Log.w(TAG, "Jaxmpp disconnected");
						wakeup();
					}
				});
	}

	public boolean check(Context context) {
		Log.i(TAG, "Checking connection " + jid + "...");

		contact.getProperties().setUserProperty(SessionObject.USER_BARE_JID, jid);
		contact.getProperties().setUserProperty(SessionObject.PASSWORD, password);
		contact.getProperties().setUserProperty(Connector.TRUST_MANAGERS_KEY,
				SecureTrustManagerFactory.getTrustManagers(context));
		if (hostname != null && !hostname.trim().isEmpty())
			contact.getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);

		try {

			Log.d(TAG, "Login 1...");
			contact.login(true);
			synchronized (ConnectionChecker.this) {
				if (!contact.isConnected())
					this.wait(1000 * 30);
			}
			Log.d(TAG, "... done 1");

			if (contact.getSessionObject().getProperty(SocketConnector.RECONNECTING_KEY) != null
					&& (Boolean) contact.getSessionObject().getProperty(SocketConnector.RECONNECTING_KEY)) {
				Log.d(TAG, "Login 2... because of see-other-host");
				contact.login(true);
				Log.d(TAG, "... done 2");
			}

			final JID bindedJID = ResourceBinderModule.getBindedJID(contact.getSessionObject());

			Log.d(TAG, "Binded JID: " + bindedJID);
			Log.d(TAG, "Czy jest error? " + contact.getSessionObject().getProperty(tigase.jaxmpp.j2se.Jaxmpp.EXCEPTION_KEY)
					+ "  " + (exception == null));
			return exception == null && errorMessage == null && bindedJID != null;
		} catch (JaxmppException e) {
			Log.w(TAG, "Auth problem", e);
			exception = e;
			return false;
		} catch (Exception e) {
			Log.e(TAG, "Problem on password check2", e);
			return false;
		} finally {
			try {
				contact.disconnect();
			} catch (Exception e) {
				Log.e(TAG, "Disconnect problem on password check", e);
			}
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Throwable getException() {
		return exception;
	}

	public boolean isPasswordInvalid() {
		return passwordInvalid;
	}

	protected void wakeup() {
		synchronized (ConnectionChecker.this) {
			this.notify();
		}
	}

}
