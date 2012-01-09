package org.tigase.mobile;

import org.tigase.mobile.db.providers.DBChatManager;
import org.tigase.mobile.db.providers.DBRosterCacheProvider;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.factory.UniversalFactory.FactorySpi;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatSelector;
import tigase.jaxmpp.core.client.xmpp.modules.chat.JidOnlyChatSelector;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.DnsResolver;
import android.app.Application;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class MessengerApplication extends Application {

	private MultiJaxmpp multiJaxmpp;

	public MessengerApplication() {
		super();

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

		// Logger logger = Logger.getLogger("tigase.jaxmpp");
		// // create a ConsoleHandler
		// Handler handler = new ConsoleHandler();
		// handler.setLevel(Level.ALL);
		// logger.addHandler(handler);
		// logger.setLevel(Level.ALL);

	}

	private void createMultiJaxmpp() {
		this.multiJaxmpp = new MultiJaxmpp();

		Listener<PresenceEvent> presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				RosterItem it = multiJaxmpp.get(be.getSessionObject()).getRoster().get(be.getJid().getBareJid());
				if (it != null) {
					Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), it.getId());
					getContentResolver().notifyChange(insertedItem, null);
				}
			}
		};
		multiJaxmpp.addListener(PresenceModule.ContactAvailable, presenceListener);
		multiJaxmpp.addListener(PresenceModule.ContactUnavailable, presenceListener);
		multiJaxmpp.addListener(PresenceModule.ContactChangedPresence, presenceListener);

		multiJaxmpp.addListener(Connector.StateChanged, new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {

				if (getState(be.getSessionObject()) == State.disconnected)
					multiJaxmpp.get(be.getSessionObject()).getPresence().clear(true);
			}
		});
		JaxmppService.updateJaxmppInstances(multiJaxmpp, getContentResolver(), getResources(), this);

	}

	public MultiJaxmpp getMultiJaxmpp() {
		if (multiJaxmpp == null) {
			createMultiJaxmpp();
		}
		return multiJaxmpp;
	}

	protected final State getState(SessionObject object) {
		State state = multiJaxmpp.get(object).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state == null ? State.disconnected : state;
	}

}
