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

package org.tigase.messenger.phone.pro.account;

import android.util.Log;
import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventBus;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.EventListener;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.StreamPacket;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;

/**
 * Created by bmalkow on 02.05.2017.
 */
public class AccountCreator {

	private static final String TAG = "AccountCreator";
	private final Jaxmpp contact = new Jaxmpp();
	private final String hostname;
	public boolean stopped = false;
	protected String errorMessage;
	protected Throwable exception;
	private boolean passwordInvalid = false;
	private boolean success = false;

	public AccountCreator(String mHostname) {
		this.hostname = mHostname;

		contact.getModulesManager().register(new InBandRegistrationModule());

		contact.getEventBus().addHandler(Connector.ErrorHandler.ErrorEvent.class, new Connector.ErrorHandler() {
			@Override
			public void onError(SessionObject sessionObject, StreamError condition, Throwable e)
					throws JaxmppException {
				Log.w(TAG, "onError() " + condition + "   " + e);
				if (exception == null) {
					exception = e;
				}
			}
		});

		contact.getEventBus()
				.addHandler(Connector.StanzaReceivedHandler.StanzaReceivedEvent.class,
							new Connector.StanzaReceivedHandler() {

								@Override
								public void onStanzaReceived(SessionObject sessionObject, StreamPacket stanza) {
									try {
										Log.i(TAG, ">> " + stanza.getAsString());
									} catch (XMLException e) {
										Log.e(TAG, "WTF", e);
										e.printStackTrace();
									}
								}
							});
		contact.getEventBus()
				.addHandler(Connector.StanzaSendingHandler.StanzaSendingEvent.class,
							new Connector.StanzaSendingHandler() {

								@Override
								public void onStanzaSending(SessionObject sessionObject, Element stanza)
										throws JaxmppException {
									try {
										Log.i(TAG, "<< " + stanza.getAsString());
									} catch (XMLException e) {
										Log.e(TAG, "WTF", e);
										e.printStackTrace();
									}
								}
							});

		contact.getEventBus().addListener(new EventListener() {

			@Override
			public void onEvent(Event<? extends EventHandler> event) {
				Log.i(TAG, "Event: " + event.getClass());
			}
		});
		contact.getEventBus()
				.addHandler(AuthModule.AuthFailedHandler.AuthFailedEvent.class, new AuthModule.AuthFailedHandler() {

					@Override
					public void onAuthFailed(SessionObject sessionObject, SaslModule.SaslError error)
							throws JaxmppException {
						Log.w(TAG, "AuthFailedEvent() " + error);
						errorMessage = "Invalid username or password";
						passwordInvalid = true;
						// wakeup();
					}
				});
		contact.getEventBus()
				.addHandler(InBandRegistrationModule.NotSupportedErrorHandler.NotSupportedErrorEvent.class,
							new InBandRegistrationModule.NotSupportedErrorHandler() {

								@Override
								public void onNotSupportedError(SessionObject sessionObject) throws JaxmppException {
									Log.w(TAG, "NotSupportedErrorHandler() ");
									errorMessage = "Registration not supported.";
									wakeup();
								}
							});
		contact.getEventBus()
				.addHandler(InBandRegistrationModule.ReceivedErrorHandler.ReceivedErrorEvent.class,
							new InBandRegistrationModule.ReceivedErrorHandler() {

								@Override
								public void onReceivedError(SessionObject sessionObject, IQ responseStanza,
															XMPPException.ErrorCondition errorCondition)
										throws JaxmppException {
									errorMessage = "Error during registration.";
									wakeup();
								}
							});
		contact.getEventBus()
				.addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, new JaxmppCore.LoggedInHandler() {
					@Override
					public void onLoggedIn(SessionObject sessionObject) {
						Log.w(TAG, "Jaxmpp connected");
						wakeup();
					}
				});
		contact.getEventBus()
				.addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, new JaxmppCore.LoggedOutHandler() {

					@Override
					public void onLoggedOut(SessionObject sessionObject) {
						Log.w(TAG, "Jaxmpp disconnected");
						wakeup();
					}
				});

	}

	public Jaxmpp getContact() {
		return contact;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public EventBus getEventBus() {
		return this.getContact().getEventBus();
	}

	public Throwable getException() {
		return exception;
	}

	public Jaxmpp getJaxmpp() {
		return this.contact;
	}

	public boolean isPasswordInvalid() {
		return passwordInvalid;
	}

	public boolean register(android.content.Context context) {
		try {
			stopped = false;
			contact.getSessionObject()
					.setProperty(InBandRegistrationModule.IN_BAND_REGISTRATION_MODE_KEY, Boolean.TRUE);
//		contact.getConnectionConfiguration().setServer("127.0.0.1");
			contact.getConnectionConfiguration().setDomain(hostname);

			contact.getProperties()
					.setUserProperty(Connector.TRUST_MANAGERS_KEY, SecureTrustManagerFactory.getTrustManagers(context));
//		if (hostname != null && !hostname.trim().isEmpty()) {
//			contact.getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
//		}

			Log.d(TAG, "Login 1...");
			contact.login(false);
			synchronized (AccountCreator.this) {
				while (!stopped) {
					if (!contact.isConnected()) {
						this.wait(750);
					}
				}
			}
			Log.d(TAG, "... done 1");

			if (contact.getSessionObject().getProperty(SocketConnector.RECONNECTING_KEY) != null &&
					(Boolean) contact.getSessionObject().getProperty(SocketConnector.RECONNECTING_KEY)) {
				Log.d(TAG, "Login 2... because of see-other-host");
				contact.login(true);
				Log.d(TAG, "... done 2");
			}

			Log.d(TAG,
				  "Czy jest error? " + contact.getSessionObject().getProperty(tigase.jaxmpp.j2se.Jaxmpp.EXCEPTION_KEY) +
						  "  " + (exception == null));
			return exception == null && errorMessage == null && success;
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

	public void success() {
		this.success = true;
		wakeup();
	}

	protected void wakeup() {
		Log.d(TAG, "Waking up...");
		stopped = true;
		synchronized (AccountCreator.this) {
			this.notifyAll();
		}
	}

}
