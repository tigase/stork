package org.tigase.mobile;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.connector.AbstractBoshConnector;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class JaxmppService extends Service {

	private final Jaxmpp jaxmpp = XmppService.jaxmpp();

	public JaxmppService() {
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

		display("creating");
	}

	private final void display(String message) {
		Log.i("service", message);

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		display("onCreate()");
	}

	@Override
	public void onDestroy() {
		display("onDestroy()");
		try {
			jaxmpp.disconnect();
		} catch (JaxmppException e) {
			Log.e("messenger", "Can't disconnect", e);
		}
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		display("onStart()");
		super.onStart(intent, startId);
		SharedPreferences prefs = getSharedPreferences("org.tigase.mobile_preferences", 0);

		JID jid = JID.jidInstance(prefs.getString("user_jid", null));
		String password = prefs.getString("user_password", null);
		String hostname = prefs.getString("hostname", null);

		jaxmpp.getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
		jaxmpp.getProperties().setUserProperty(SessionObject.USER_JID, jid);
		jaxmpp.getProperties().setUserProperty(SessionObject.PASSWORD, password);

		super.onCreate();
		try {
			jaxmpp.login(false);
		} catch (JaxmppException e) {
			Log.e("messenger", "Can't connect", e);
		}
	}

}
