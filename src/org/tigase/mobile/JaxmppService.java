package org.tigase.mobile;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.connector.AbstractBoshConnector;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule.RosterEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class JaxmppService extends Service {

	private class ClientFocusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final int page = intent.getIntExtra("page", -1);
			final long chatId = intent.getLongExtra("chatId", -1);

			currentChatIdFocus = chatId;

			notificationManager.cancel("chatId-" + chatId, CHAT_NOTIFICATION_ID);
		}
	}

	private class ConnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			String info;
			if (netInfo.isConnected())
				info = "Nawiˆzano po¸ˆczenie z: " + netInfo.getTypeName();
			else
				info = "Zerwano po¸ˆczenie z: " + netInfo.getTypeName();

			if (DEBUG)
				Log.i(TAG, info);

			refreshInfos();

		}

	}

	private static enum NotificationVariant {
		always,
		none,
		only_connected,
		only_disconnected
	}

	public static final int CHAT_NOTIFICATION_ID = 132008;

	private static final boolean DEBUG = false;

	public static final int NOTIFICATION_ID = 5398777;

	private static final String TAG = "tigase";

	private ConnectivityManager connManager;

	private long currentChatIdFocus = -1;

	// private MessengerDatabaseHelper dbHelper;

	private final Listener<Connector.ConnectorEvent> disconnectListener;

	private ClientFocusReceiver focusChangeReceiver;

	private final Jaxmpp jaxmpp = XmppService.jaxmpp();

	private final Listener<MessageModule.MessageEvent> messageListener;

	private ConnReceiver myConnReceiver;

	private NotificationManager notificationManager;

	private NotificationVariant notificationVariant = NotificationVariant.always;

	private OnSharedPreferenceChangeListener prefChangeListener;

	private SharedPreferences prefs;

	private final Listener<PresenceModule.PresenceEvent> presenceListener;

	private boolean reconnect = true;

	private final Listener<ResourceBindEvent> resourceBindListener;

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

		if (DEBUG)
			Log.i(TAG, "creating");

		this.messageListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				if (be.getChat() != null && be.getMessage().getBody() != null) {

					Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + be.getChat().getJid().getBareJid().toString());

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_JID, be.getChat().getJid().getBareJid().toString());
					values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
					values.put(ChatTableMetaData.FIELD_BODY, be.getMessage().getBody());
					values.put(ChatTableMetaData.FIELD_TYPE, 0);
					values.put(ChatTableMetaData.FIELD_STATE, 0);

					getContentResolver().insert(uri, values);

					showChatNotification(be);
				}

			}
		};

		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				updateRosterItem(be.getPresence());
			}
		};

		this.rosterListener = new Listener<RosterModule.RosterEvent>() {

			@Override
			public synchronized void handleEvent(final RosterEvent be) throws JaxmppException {
				if (be.getType() == RosterModule.ItemAdded)
					insertRosterItem(be.getItem());
				else if (be.getType() == RosterModule.ItemUpdated)
					updateRosterItem(be.getItem());
				else if (be.getType() == RosterModule.ItemRemoved)
					removeRosterItem(be.getItem());
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

		this.resourceBindListener = new Listener<ResourceBinderModule.ResourceBindEvent>() {

			@Override
			public void handleEvent(ResourceBindEvent be) throws JaxmppException {
				sendUnsentMessages();
			}
		};

	}

	protected final State getState() {
		State state = XmppService.jaxmpp().getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state;
	}

	protected void insertRosterItem(RosterItem item) {
		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);
		values.put(RosterTableMetaData.FIELD_PRESENCE, RosterProvider.getShowOf(item.getJid()).getId());
		values.put(RosterTableMetaData.FIELD_DISPLAY_NAME, RosterProvider.getDisplayName(item));

		getContentResolver().insert(Uri.parse(RosterProvider.CONTENT_URI), values);
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
		return null;
	}

	@Override
	public void onCreate() {
		if (DEBUG)
			Log.i(TAG, "onCreate()");

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

		this.focusChangeReceiver = new ClientFocusReceiver();
		filter = new IntentFilter(TigaseMobileMessengerActivity.CLIENT_FOCUS_MSG);
		registerReceiver(focusChangeReceiver, filter);

		XmppService.jaxmpp().getModulesManager().getModule(ResourceBinderModule.class).addListener(
				ResourceBinderModule.ResourceBindSuccess, this.resourceBindListener);

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

		if (focusChangeReceiver != null)
			unregisterReceiver(focusChangeReceiver);

		this.reconnect = false;
		Log.i(TAG, "Stopping service");
		try {
			jaxmpp.disconnect();
		} catch (JaxmppException e) {
			Log.e(TAG, "Can't disconnect", e);
		}

		getContentResolver().delete(Uri.parse(RosterProvider.PRESENCE_URI), null, null);

		XmppService.jaxmpp().getModulesManager().getModule(ResourceBinderModule.class).removeListener(
				ResourceBinderModule.ResourceBindSuccess, this.resourceBindListener);

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

		notificationCancel();

		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (DEBUG)
			Log.i(TAG, "onStart()");
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
					getContentResolver().delete(Uri.parse(RosterProvider.CONTENT_URI), null, null);
					jaxmpp.login(false);
				}
			} catch (JaxmppException e) {
				Log.e(TAG, "Can't connect", e);
			}
		}
	}

	public void refreshInfos() {
		final State state = getState();
		final boolean networkAvailable = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()
				|| connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

		if (DEBUG)
			Log.d(TAG, "State=" + state + "; network=" + networkAvailable);

		if (!networkAvailable && (state == State.connected || state == State.connecting || state == State.disconnecting)) {
			if (DEBUG)
				Log.i(TAG, "Network disconnected!");
			try {
				jaxmpp.disconnect();
			} catch (JaxmppException e) {
				Log.w(TAG, "Can't disconnect", e);
			}
		} else if (networkAvailable && (state == State.disconnected)) {
			if (DEBUG)
				Log.i(TAG, "Network available! Reconnecting!");
			reconnect();
		}

	}

	protected void removeRosterItem(RosterItem item) {
		getContentResolver().delete(Uri.parse(RosterProvider.CONTENT_URI + "/" + item.getJid()), null, null);
	}

	protected void sendUnsentMessages() {
		final Cursor c = getApplication().getContentResolver().query(Uri.parse(ChatHistoryProvider.UNSENT_MESSAGES_URI), null,
				null, null, null);
		try {
			final int columnId = c.getColumnIndex(ChatTableMetaData.FIELD_ID);
			final int columnJid = c.getColumnIndex(ChatTableMetaData.FIELD_JID);
			final int columnMsg = c.getColumnIndex(ChatTableMetaData.FIELD_BODY);
			final int columnThd = c.getColumnIndex(ChatTableMetaData.FIELD_THREAD_ID);

			c.moveToFirst();
			if (c.isAfterLast())
				return;
			do {
				long id = c.getLong(columnId);
				String jid = c.getString(columnJid);
				String body = c.getString(columnMsg);
				String threadId = c.getString(columnThd);

				Message msg = Message.create();
				msg.setType(StanzaType.chat);
				msg.setTo(JID.jidInstance(jid));
				msg.setBody(body);
				if (threadId != null && threadId.length() > 0)
					msg.setThread(threadId);
				if (DEBUG)
					Log.i(TAG, "Found unsetn message: " + jid + " :: " + body);

				try {
					jaxmpp.send(msg);

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_ID, id);
					values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);

					getContentResolver().update(Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + jid + "/" + id), values, null,
							null);
				} catch (JaxmppException e) {
					if (DEBUG)
						Log.d(TAG, "Can't send message");
				}

				c.moveToNext();
			} while (!c.isAfterLast());
		} catch (XMLException e) {
			Log.e(TAG, "WTF??", e);
		} finally {
			c.close();
		}
	}

	protected void showChatNotification(final MessageEvent event) throws XMLException {
		int ico = R.drawable.new_message;
		String notiticationTitle = "Message from " + event.getMessage().getFrom();
		String expandedNotificationText = notiticationTitle;

		long whenNotify = System.currentTimeMillis();
		Notification notification = new Notification(ico, notiticationTitle, whenNotify);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.defaults |= Notification.DEFAULT_SOUND;

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.GREEN;
		notification.ledOffMS = 500;
		notification.ledOnMS = 500;

		final Context context = getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		intent.setAction("messageFrom-" + event.getMessage().getFrom());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("jid", "" + event.getMessage().getFrom());
		if (event.getChat() != null)
			intent.putExtra("chatId", event.getChat().getId());

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		if (currentChatIdFocus != event.getChat().getId())
			notificationManager.notify("chatId-" + event.getChat().getId(), CHAT_NOTIFICATION_ID, notification);

	}

	protected void updateRosterItem(final Presence presence) throws XMLException {
		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_PRESENCE, RosterProvider.getShowOf(presence.getFrom().getBareJid()).getId());
		getContentResolver().update(Uri.parse(RosterProvider.CONTENT_URI + "/" + presence.getFrom().getBareJid()), values,
				null, null);
	}

	protected void updateRosterItem(RosterItem item) {
		ContentValues values = new ContentValues();
		values.put(RosterTableMetaData.FIELD_JID, item.getJid().toString());
		values.put(RosterTableMetaData.FIELD_NAME, item.getName());
		values.put(RosterTableMetaData.FIELD_SUBSCRIPTION, item.getSubscription().name());
		values.put(RosterTableMetaData.FIELD_ASK, item.isAsk() ? 1 : 0);
		values.put(RosterTableMetaData.FIELD_PRESENCE, RosterProvider.getShowOf(item.getJid()).getId());
		values.put(RosterTableMetaData.FIELD_DISPLAY_NAME, RosterProvider.getDisplayName(item));

		getContentResolver().update(Uri.parse(RosterProvider.CONTENT_URI + "/" + item.getJid()), values, null, null);
	}
}
