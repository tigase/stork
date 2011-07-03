package org.tigase.mobile;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.connector.AbstractBoshConnector;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.os.AsyncTask;

public class XmppService {

	private static XmppService instance;

	public static Jaxmpp jaxmpp() {
		if (instance == null)
			instance = new XmppService();
		return instance.jaxmpp;
	}

	private Jaxmpp jaxmpp;

	private final AsyncTask<Runnable, Void, String> x = new AsyncTask<Runnable, Void, String>() {

		@Override
		protected String doInBackground(Runnable... params) {
			for (Runnable x : params) {
				x.run();
			}
			return null;
		}
	};

	private XmppService() {
		jaxmpp = new Jaxmpp();
		Logger logger = Logger.getLogger("tigase.jaxmpp");
		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		// for BOSH connector
		jaxmpp.getProperties().setUserProperty(AbstractBoshConnector.BOSH_SERVICE_URL_KEY, "http://xmpp.tigase.org");
		// jaxmpp.getProperties().setUserProperty(AbstractBoshConnector.BOSH_SERVICE_URL,
		// "http://messenger.tigase.org:80/bosh");

		// for Socket connector
		jaxmpp.getProperties().setUserProperty(AuthModule.FORCE_NON_SASL, Boolean.TRUE);

		// port value is not necessary. Default is 5222
		jaxmpp.getProperties().setUserProperty(SocketConnector.SERVER_PORT, 5222);

		// "bosh" and "socket" values available
		jaxmpp.getProperties().setUserProperty(Jaxmpp.CONNECTOR_TYPE, "socket");

		jaxmpp.getProperties().setUserProperty(SessionObject.RESOURCE, "TigaseMobileMessenger");

	}

}
