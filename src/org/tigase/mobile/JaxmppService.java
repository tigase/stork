package org.tigase.mobile;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.connector.AbstractBoshConnector;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule.RosterEvent;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class JaxmppService extends Service {

	private class ConnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			String info;
			if (netInfo.isConnected())
				info = "Nawiˆzano po¸ˆczenie z: " + netInfo.getTypeName();
			else
				info = "Zerwano po¸ˆczenie z: " + netInfo.getTypeName();

			Log.i(TigaseMobileMessengerActivity.LOG_TAG, info);

			refreshInfos();

		}

	}

	private static enum NotificationVariant {
		always,
		none,
		only_connected,
		only_disconnected
	}

	public static final int NOTIFICATION_ID = 5398777;

	private ConnectivityManager connManager;

	private MessengerDatabaseHelper dbHelper;

	private final Listener<Connector.ConnectorEvent> disconnectListener;

	private final Jaxmpp jaxmpp = XmppService.jaxmpp();

	private final Listener<MessageModule.MessageEvent> messageListener;

	private ConnReceiver myConnReceiver;

	private NotificationManager notificationManager;

	private NotificationVariant notificationVariant = NotificationVariant.always;

	private OnSharedPreferenceChangeListener prefChangeListener;

	private SharedPreferences prefs;

	private final Listener<PresenceModule.PresenceEvent> presenceListener;

	private boolean reconnect = true;

	private final Listener<RosterModule.RosterEvent> rosterListener;

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

		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "creating");

		this.messageListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				if (be.getChat() != null && be.getMessage().getBody() != null) {
					dbHelper.addChatHistory(0, be.getChat(), be.getMessage().getBody());

					// TODO chat notification? notifyA();

				}

			}
		};

		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				dbHelper.updateRosterItem(be.getPresence());
			}
		};

		this.rosterListener = new Listener<RosterModule.RosterEvent>() {

			@Override
			public synchronized void handleEvent(final RosterEvent be) throws JaxmppException {
				if (be.getType() == RosterModule.ItemAdded)
					dbHelper.insertRosterItem(be.getItem());
				else if (be.getType() == RosterModule.ItemUpdated)
					dbHelper.updateRosterItem(be.getItem());
				else if (be.getType() == RosterModule.ItemRemoved)
					dbHelper.removeRosterItem(be.getItem());
			}
		};

		this.disconnectListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(Connector.ConnectorEvent be) throws JaxmppException {
				notificationUpdate();
				if (getState() == State.disconnected) {
					reconnect();
				}
			}
		};

	}

	protected final State getState() {
		State state = XmppService.jaxmpp().getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state;
	}

	private void notificationCancel() {
		notificationManager.cancel(NOTIFICATION_ID);
	}

	private void notificationUpdate() {
		final State state = getState();

		if (this.notificationVariant == NotificationVariant.none) {
			notificationCancel();
			return;
		}

		int ico = R.drawable.sb_offline;
		String notiticationTitle = null;
		String expandedNotificationText = null;

		if (state == State.connecting) {
			ico = R.drawable.sb_online;
			notiticationTitle = "Connecting";
			expandedNotificationText = "Connecting...";
			if (this.notificationVariant != NotificationVariant.always) {
				notificationCancel();
				return;
			}
		} else if (state == State.connected) {
			ico = R.drawable.sb_online;
			notiticationTitle = "Connected";
			expandedNotificationText = "Online";
			if (this.notificationVariant == NotificationVariant.only_disconnected) {
				notificationCancel();
				return;
			}
		} else if (state == State.disconnecting) {
			ico = R.drawable.sb_offline;
			notiticationTitle = "Disconnecting";
			expandedNotificationText = "Disconnecting...";
			if (this.notificationVariant != NotificationVariant.always) {
				notificationCancel();
				return;
			}
		} else if (state == State.disconnected) {
			ico = R.drawable.sb_offline;
			notiticationTitle = "Disconnected";
			expandedNotificationText = "Offline";
			if (this.notificationVariant == NotificationVariant.only_connected) {
				notificationCancel();
				return;
			}
		}

		long whenNotify = System.currentTimeMillis();
		Notification notification = new Notification(ico, notiticationTitle, whenNotify);

		// notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "onCreate()");

		this.prefChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if ("notification_type".equals(key)) {
					notificationVariant = NotificationVariant.valueOf(sharedPreferences.getString(key, "always"));
					notificationUpdate();
				}
			}
		};

		this.prefs = getSharedPreferences("org.tigase.mobile_preferences", 0);
		this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
		notificationVariant = NotificationVariant.valueOf(prefs.getString("notification_type", "always"));

		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		this.myConnReceiver = new ConnReceiver();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(myConnReceiver, filter);

		this.dbHelper = new MessengerDatabaseHelper(getApplicationContext());
		this.dbHelper.open();

		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemAdded,
				this.rosterListener);
		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemRemoved,
				this.rosterListener);
		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemUpdated,
				this.rosterListener);

		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactAvailable,
				this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);

		XmppService.jaxmpp().addListener(Connector.StateChanged, this.disconnectListener);

		XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).addListener(MessageModule.MessageReceived,
				this.messageListener);

		this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		XmppService.jaxmpp().getSessionObject().setProperty(Connector.CONNECTOR_STAGE_KEY, null);
		notificationUpdate();
	}

	@Override
	public void onDestroy() {
		this.prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener);

		if (myConnReceiver != null)
			unregisterReceiver(myConnReceiver);

		this.reconnect = false;
		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Stopping service");
		try {
			jaxmpp.disconnect();
		} catch (JaxmppException e) {
			Log.e(TigaseMobileMessengerActivity.LOG_TAG, "Can't disconnect", e);
		}

		dbHelper.makeAllOffline();

		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemAdded,
				this.rosterListener);
		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemRemoved,
				this.rosterListener);
		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemUpdated,
				this.rosterListener);

		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactAvailable, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);
		XmppService.jaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(
				PresenceModule.ContactChangedPresence, this.presenceListener);

		XmppService.jaxmpp().removeListener(JaxmppCore.Disconnected, this.disconnectListener);

		XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).removeListener(MessageModule.MessageReceived,
				this.messageListener);

		this.dbHelper.close();

		notificationCancel();

		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "onStart()");
		this.reconnect = true;
		super.onStart(intent, startId);

		JID jid = JID.jidInstance(prefs.getString("user_jid", null));
		String password = prefs.getString("user_password", null);
		String hostname = prefs.getString("hostname", null);

		notificationVariant = NotificationVariant.valueOf(prefs.getString("notification_type", "always"));

		jaxmpp.getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
		jaxmpp.getProperties().setUserProperty(SessionObject.USER_JID, jid);
		jaxmpp.getProperties().setUserProperty(SessionObject.PASSWORD, password);

		reconnect();
	}

	private void reconnect() {
		if (connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()
				|| connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
			try {
				if (reconnect) {
					dbHelper.clearRoster();
					jaxmpp.login(false);
				}
			} catch (JaxmppException e) {
				Log.e(TigaseMobileMessengerActivity.LOG_TAG, "Can't connect", e);
			}
		}
	}

	public void refreshInfos() {
		final State state = getState();
		final boolean networkAvailable = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()
				|| connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
		if (!networkAvailable && (state == State.connected || state == State.connecting)) {
			Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Network disconnected!");
			try {
				jaxmpp.disconnect();
			} catch (JaxmppException e) {
				Log.w(TigaseMobileMessengerActivity.LOG_TAG, "Can't disconnect", e);
			}
		} else if (networkAvailable && (state == State.disconnected)) {
			Log.i(TigaseMobileMessengerActivity.LOG_TAG, "Network available! Reconnecting!");
			reconnect();
		}

	}
}
