package org.tigase.mobile;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.mobile.db.providers.DBChatManager;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatManagerFactory;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatManagerFactory.ChatManagerFactorySpi;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.content.Context;

public class XmppService {

	private static XmppService instance;

	public static Jaxmpp jaxmpp(Context context) {
		if (instance == null)
			instance = new XmppService(context);
		return instance.jaxmpp;
	}

	private Jaxmpp jaxmpp;

	private XmppService(final Context context) {
		ChatManagerFactory.setSpi(new ChatManagerFactorySpi() {

			@Override
			public AbstractChatManager createChatManager() {
				return new DBChatManager(context);
			}
		});

		jaxmpp = new Jaxmpp();
		Logger logger = Logger.getLogger("tigase.jaxmpp");
		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		// for Socket connector
		jaxmpp.getProperties().setUserProperty(AuthModule.FORCE_NON_SASL, Boolean.FALSE);

		// port value is not necessary. Default is 5222
		jaxmpp.getProperties().setUserProperty(SocketConnector.SERVER_PORT, 5222);

		// "bosh" and "socket" values available
		jaxmpp.getProperties().setUserProperty(Jaxmpp.CONNECTOR_TYPE, "socket");

		jaxmpp.getProperties().setUserProperty(SessionObject.RESOURCE, "TigaseMobileMessenger");

	}

}
