package org.tigase.mobile;

import org.tigase.mobile.db.providers.DBChatManager;
import org.tigase.mobile.db.providers.DBRosterCacheProvider;

import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.factory.UniversalFactory.FactorySpi;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatSelector;
import tigase.jaxmpp.core.client.xmpp.modules.chat.JidOnlyChatSelector;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.DnsResolver;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MessengerApplication extends Application {

	private Jaxmpp jaxmpp;

	private void createJaxmpp() {
		if (jaxmpp != null)
			return;

		Log.i("tigase", "Creating new instance of JaXMPP");

		final Context context = this;
		UniversalFactory.setSpi(ChatSelector.class.getName(), new FactorySpi<ChatSelector>() {

			@Override
			public ChatSelector create() {
				return new JidOnlyChatSelector();
			}
		});

		UniversalFactory.setSpi(AbstractChatManager.class.getName(), new FactorySpi<AbstractChatManager>() {

			@Override
			public AbstractChatManager create() {
				return new DBChatManager(context);
			}
		});
		UniversalFactory.setSpi(RosterCacheProvider.class.getName(), new FactorySpi<RosterCacheProvider>() {

			@Override
			public RosterCacheProvider create() {
				return new DBRosterCacheProvider(context);
			}
		});
		UniversalFactory.setSpi(DnsResolver.class.getName(), new FactorySpi<DnsResolver>() {

			@Override
			public DnsResolver create() {
				return new DNSResolver();
			}
		});

		jaxmpp = new Jaxmpp();
		// Logger logger = Logger.getLogger("tigase.jaxmpp");
		// // create a ConsoleHandler
		// Handler handler = new ConsoleHandler();
		// handler.setLevel(Level.ALL);
		// logger.addHandler(handler);
		// logger.setLevel(Level.ALL);

		jaxmpp.getProperties().setUserProperty(AuthModule.FORCE_NON_SASL, Boolean.FALSE);

		// port value is not necessary. Default is 5222

		// "bosh" and "socket" values available
		jaxmpp.getProperties().setUserProperty(Jaxmpp.CONNECTOR_TYPE, "socket");

	}

	public Jaxmpp getJaxmpp() {
		if (jaxmpp == null)
			createJaxmpp();
		return jaxmpp;
	}

}
