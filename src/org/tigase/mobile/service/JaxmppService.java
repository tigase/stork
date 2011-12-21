package org.tigase.mobile.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MessengerPreferenceActivity;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.roster.AuthRequestActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule.RosterEvent;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
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
import android.widget.Toast;

public class JaxmppService extends Service {

	private class ClientFocusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final int page = intent.getIntExtra("page", -1);
			final long chatId = intent.getLongExtra("chatId", -1);

			onPageChanged(page);

			currentChatIdFocus = chatId;

			notificationManager.cancel("chatId:" + chatId, CHAT_NOTIFICATION_ID);
		}
	}

	private class ConnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			String info;
			if (netInfo.isConnected())
				info = "Nawiązano połączenie z: " + netInfo.getTypeName();
			else
				info = "Zerwano połączenie z: " + netInfo.getTypeName();

			if (DEBUG)
				Log.i(TAG, info);

			connectionErrorCounter = 0;

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

	private static boolean serviceActive = false;

	private static final String TAG = "tigase";

	public static boolean isServiceActive() {
		return serviceActive;
	}

	private int connectionErrorCounter = 0;

	private ConnectivityManager connManager;

	private long currentChatIdFocus = -1;

	private final Listener<Connector.ConnectorEvent> disconnectListener;

	private ClientFocusReceiver focusChangeReceiver;

	private boolean focused;

	private final Listener<MessageModule.MessageEvent> messageListener;

	private ConnReceiver myConnReceiver;

	private NotificationManager notificationManager;

	private NotificationVariant notificationVariant = NotificationVariant.always;

	private OnSharedPreferenceChangeListener prefChangeListener;

	private SharedPreferences prefs;

	private final Listener<PresenceModule.PresenceEvent> presenceListener;

	private final Listener<PresenceEvent> presenceSendListener;

	private boolean reconnect = true;

	private TimerTask reconnectTask;

	private final Listener<ResourceBindEvent> resourceBindListener;

	private final Listener<RosterModule.RosterEvent> rosterListener;

	private Listener<PresenceEvent> subscribeRequestListener;

	private final Timer timer = new Timer();

	private Integer usedNetworkType = null;

	private String userStatusMessage = null;

	private Show userStatusShow = Show.online;

	public JaxmppService() {
		super();
		Logger logger = Logger.getLogger("tigase.jaxmpp");
		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		if (DEBUG)
			Log.i(TAG, "creating");

		this.presenceSendListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				be.setStatus(userStatusMessage);
				if (focused) {
					be.setShow(userStatusShow);
					be.setPriority(prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5));
				} else {
					be.setShow(Show.away);
					be.setStatus("Auto away");
					be.setPriority(prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0));
				}
			}
		};

		this.messageListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				if (be.getChat() != null && be.getMessage().getBody() != null) {

					Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + be.getChat().getJid().getBareJid().toString());

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_JID, be.getChat().getJid().getBareJid().toString());
					values.put(ChatTableMetaData.FIELD_AUTHOR_JID, be.getChat().getJid().getBareJid().toString());
					values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
					values.put(ChatTableMetaData.FIELD_BODY, be.getMessage().getBody());
					values.put(ChatTableMetaData.FIELD_STATE, 0);

					getContentResolver().insert(uri, values);

					showChatNotification(be);
				}

			}
		};

		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				updateRosterItem(be);
			}
		};
		this.subscribeRequestListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				onSubscribeRequest(be);
			}
		};

		this.rosterListener = new Listener<RosterModule.RosterEvent>() {

			@Override
			public synchronized void handleEvent(final RosterEvent be) throws JaxmppException {
				if (be.getType() == RosterModule.ItemAdded)
					changeRosterItem(be);
				else if (be.getType() == RosterModule.ItemUpdated)
					changeRosterItem(be);
				else if (be.getType() == RosterModule.ItemRemoved)
					changeRosterItem(be);
			}
		};

		this.disconnectListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(Connector.ConnectorEvent be) throws JaxmppException {
				if (getState() == State.connected)
					connectionErrorCounter = 0;
				if (getState() == State.disconnected)
					getJaxmpp().getPresence().clear(true);
				if (getState() == State.disconnected) {
					if (reconnect) {
						reconnect(true);
					} else
						notificationUpdate();
				} else {
					notificationUpdate();
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

	private void cancelReconnectTask() {
		if (this.reconnectTask != null) {
			this.reconnectTask.cancel();
			this.reconnectTask = null;
		}

	}

	protected synchronized void changeRosterItem(RosterEvent be) {
		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), be.getItem().getId());
		getApplicationContext().getContentResolver().notifyChange(insertedItem, null);

		if (be.getChangedGroups() != null && !be.getChangedGroups().isEmpty()) {
			for (String gr : be.getChangedGroups()) {

				Uri x = ContentUris.withAppendedId(Uri.parse(RosterProvider.GROUP_URI), gr.hashCode());
				if (DEBUG)
					Log.d(TAG, "Group changed: " + gr + ". Sending notification for " + x);
				getApplicationContext().getContentResolver().notifyChange(x, null, true);
			}
		}

	}

	private Integer getActiveNetworkConnectionType() {
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info == null)
			return null;
		if (!info.isConnected())
			return null;
		return info.getType();
	}

	private final Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	protected final State getState() {
		State state = getJaxmpp().getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state == null ? State.disconnected : state;
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

		int ico = R.drawable.ic_stat_disconnected;
		String notiticationTitle = null;
		String expandedNotificationText = null;

		if (state == State.connecting) {
			ico = R.drawable.ic_stat_connected;
			notiticationTitle = "Connecting";
			expandedNotificationText = "Connecting...";
			if (this.notificationVariant != NotificationVariant.always) {
				notificationCancel();
				return;
			}
		} else if (state == State.connected) {
			ico = R.drawable.ic_stat_connected;
			notiticationTitle = "Connected";
			expandedNotificationText = "Online";
			if (this.notificationVariant == NotificationVariant.only_disconnected) {
				notificationCancel();
				return;
			}
		} else if (state == State.disconnecting) {
			ico = R.drawable.ic_stat_disconnected;
			notiticationTitle = "Disconnecting";
			expandedNotificationText = "Disconnecting...";
			if (this.notificationVariant != NotificationVariant.always) {
				notificationCancel();
				return;
			}
		} else if (state == State.disconnected) {
			ico = R.drawable.ic_stat_disconnected;
			notiticationTitle = "Disconnected";
			expandedNotificationText = "Offline";
			if (this.notificationVariant == NotificationVariant.only_connected) {
				notificationCancel();
				return;
			}
		}
		notificationUpdate(ico, notiticationTitle, expandedNotificationText);
	}

	private void notificationUpdate(final int ico, final String notiticationTitle, final String expandedNotificationText) {
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

	private void notificationUpdateFail() {
		notificationUpdate(R.drawable.ic_stat_disconnected, "Disconnected", "Connection impossible");
	}

	private void notificationUpdateReconnect(Date d) {
		if (this.notificationVariant == NotificationVariant.only_connected) {
			notificationCancel();
			return;
		}

		SimpleDateFormat s = new SimpleDateFormat("HH:mm:ss");

		String expandedNotificationText = "Next try on " + s.format(d);
		notificationUpdate(R.drawable.ic_stat_disconnected, "Disconnected", expandedNotificationText);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		if (DEBUG)
			Log.i(TAG, "onCreate()");
		this.prefs = getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
		this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);

		getJaxmpp().getProperties().setUserProperty(AuthModule.FORCE_NON_SASL, Boolean.TRUE);
		getJaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_PORT, 5222);
		getJaxmpp().getProperties().setUserProperty(Jaxmpp.CONNECTOR_TYPE, "socket");
		getJaxmpp().getProperties().setUserProperty(SessionObject.RESOURCE, "TigaseMobileMessenger");

		final String nickname = prefs.getString(Preferences.NICKNAME_KEY, null);
		getJaxmpp().getProperties().setUserProperty(SessionObject.NICKNAME,
				nickname == null || nickname.length() == 0 ? null : nickname);

		this.prefChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (Preferences.NOTIFICATION_TYPE_KEY.equals(key)) {
					notificationVariant = NotificationVariant.valueOf(sharedPreferences.getString(key, "always"));
					notificationUpdate();
				}
			}
		};

		notificationVariant = NotificationVariant.valueOf(prefs.getString(Preferences.NOTIFICATION_TYPE_KEY, "always"));

		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		this.myConnReceiver = new ConnReceiver();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(myConnReceiver, filter);

		this.focusChangeReceiver = new ClientFocusReceiver();
		filter = new IntentFilter(TigaseMobileMessengerActivity.CLIENT_FOCUS_MSG);
		registerReceiver(focusChangeReceiver, filter);

		getJaxmpp().getModulesManager().getModule(ResourceBinderModule.class).addListener(
				ResourceBinderModule.ResourceBindSuccess, this.resourceBindListener);

		getJaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemAdded, this.rosterListener);
		getJaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemRemoved, this.rosterListener);
		getJaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemUpdated, this.rosterListener);

		getJaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactAvailable,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactUnavailable,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.ContactChangedPresence,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.SubscribeRequest,
				this.subscribeRequestListener);

		getJaxmpp().addListener(Connector.StateChanged, this.disconnectListener);

		getJaxmpp().getModulesManager().getModule(MessageModule.class).addListener(MessageModule.MessageReceived,
				this.messageListener);

		getJaxmpp().getModulesManager().getModule(PresenceModule.class).addListener(PresenceModule.BeforeInitialPresence,
				this.presenceSendListener);

		this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		getJaxmpp().getSessionObject().setProperty(Connector.CONNECTOR_STAGE_KEY, null);
		notificationUpdate();
	}

	@Override
	public void onDestroy() {
		serviceActive = false;
		cancelReconnectTask();
		this.prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener);

		if (myConnReceiver != null)
			unregisterReceiver(myConnReceiver);

		if (focusChangeReceiver != null)
			unregisterReceiver(focusChangeReceiver);

		this.reconnect = false;
		Log.i(TAG, "Stopping service");
		try {
			getJaxmpp().disconnect();
			usedNetworkType = null;
		} catch (JaxmppException e) {
			Log.e(TAG, "Can't disconnect", e);
		}

		getJaxmpp().getModulesManager().getModule(ResourceBinderModule.class).removeListener(
				ResourceBinderModule.ResourceBindSuccess, this.resourceBindListener);

		getJaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(PresenceModule.BeforeInitialPresence,
				this.presenceSendListener);
		getJaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemAdded,
				this.rosterListener);
		getJaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemRemoved,
				this.rosterListener);
		getJaxmpp().getModulesManager().getModule(RosterModule.class).removeListener(RosterModule.ItemUpdated,
				this.rosterListener);

		getJaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(PresenceModule.ContactAvailable,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(PresenceModule.ContactUnavailable,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(PresenceModule.ContactChangedPresence,
				this.presenceListener);
		getJaxmpp().getModulesManager().getModule(PresenceModule.class).removeListener(PresenceModule.SubscribeRequest,
				this.subscribeRequestListener);

		getJaxmpp().removeListener(JaxmppCore.Disconnected, this.disconnectListener);

		getJaxmpp().getModulesManager().getModule(MessageModule.class).removeListener(MessageModule.MessageReceived,
				this.messageListener);

		notificationCancel();

		super.onDestroy();
	}

	protected void onPageChanged(int pageIndex) {
		try {
			boolean connected = getState() == State.connected
					&& getJaxmpp().getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
			Log.d(TAG, "onPageChanged(): " + focused + ", " + pageIndex);

			if (!connected) {
				Log.d(TAG, "onPageChanged(): Not connected!");
				return;
			}

			if (!focused && pageIndex >= 0) {
				if (DEBUG)
					Log.d(TAG, "Focused. Sending online presence.");
				focused = true;
				int pr = prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5);

				getJaxmpp().getModulesManager().getModule(PresenceModule.class).setPresence(userStatusShow, userStatusMessage,
						pr);
			} else if (focused && pageIndex == -1) {
				if (DEBUG)
					Log.d(TAG, "Sending auto-away presence");
				focused = false;
				int pr = prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0);

				getJaxmpp().getModulesManager().getModule(PresenceModule.class).setPresence(Show.away, "Auto away", pr);
			}
		} catch (JaxmppException e) {
			Log.e(TAG, "Can't update priority!");
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (DEBUG)
			Log.i(TAG, "onStartCommand()");
		super.onStart(intent, startId);
		if (intent != null) {
			// Log.i(TAG, intent.getExtras().toString());
			if (DEBUG)
				Log.i(TAG, "Found intent! focused=" + intent.getBooleanExtra("focused", false));
			this.focused = intent.getBooleanExtra("focused", false);
		}

		final JID jid = JID.jidInstance(prefs.getString(Preferences.USER_JID_KEY, null));
		final String password = prefs.getString(Preferences.USER_PASSWORD_KEY, null);
		final String hostname = prefs.getString(Preferences.HOSTNAME_KEY, null);

		if (jid == null || password == null || password.length() == 0) {
			Intent x = new Intent().setClass(this, MessengerPreferenceActivity.class);
			x.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			x.putExtra("missingLogin", Boolean.TRUE);
			getApplicationContext().startActivity(x);

			stopSelf();

			return;
		}
		serviceActive = true;
		this.reconnect = true;

		notificationVariant = NotificationVariant.valueOf(prefs.getString(Preferences.NOTIFICATION_TYPE_KEY, "always"));

		getJaxmpp().getProperties().setUserProperty(SoftwareVersionModule.NAME_KEY, "Tigase Mobile Messenger");
		getJaxmpp().getProperties().setUserProperty(SoftwareVersionModule.VERSION_KEY,
				getResources().getString(R.string.app_version));
		getJaxmpp().getProperties().setUserProperty(SoftwareVersionModule.OS_KEY, "Android " + android.os.Build.VERSION.RELEASE);

		if (hostname != null && hostname.trim().length() > 0)
			getJaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
		else
			getJaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, null);
		getJaxmpp().getProperties().setUserProperty(SessionObject.USER_JID, jid);
		getJaxmpp().getProperties().setUserProperty(SessionObject.PASSWORD, password);

		try {
			reconnect();
		} catch (Exception e) {
			Log.e(TAG, "Can't connect. Show error dialog.", e);
			Toast.makeText(this, "Cant connect: " + e.getMessage(), Toast.LENGTH_LONG).show();

			stopSelf();
		}
	}

	protected void onSubscribeRequest(PresenceEvent be) {
		// XXX

		String notiticationTitle = "Authentication request: " + be.getJid();
		String expandedNotificationText = notiticationTitle;

		Notification notification = new Notification(R.drawable.ic_stat_warning, notiticationTitle, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.defaults |= Notification.DEFAULT_SOUND;

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.GREEN;
		notification.ledOffMS = 500;
		notification.ledOnMS = 500;

		final Context context = getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, AuthRequestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("jid", "" + be.getJid());

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify("authRequest:" + be.getJid(), CHAT_NOTIFICATION_ID, notification);
	}

	private void reconnect() {
		reconnect(false);
	}

	private void reconnect(boolean delayed) {
		final Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					NetworkInfo active = connManager.getActiveNetworkInfo();
					if (DEBUG)
						Log.d(TAG, "NetworkInfo: " + active);
					if (active != null && active.isConnected()) {
						if (DEBUG)
							Log.d(TAG, "Logging in with " + active.getTypeName());

						usedNetworkType = active.getType();
						getJaxmpp().login(false);
					}
				} catch (Exception e) {
					++connectionErrorCounter;
					try {
						getJaxmpp().disconnect(true);
						usedNetworkType = null;
					} catch (JaxmppException e1) {
						Log.w(TAG, "Can't disconnect", e1);
					}
					refreshInfos();
					Log.e(TAG, "Can't connect. Counter=" + connectionErrorCounter, e);
				}
			}
		};

		cancelReconnectTask();
		if (!delayed)
			r.run();
		else {
			this.reconnectTask = new TimerTask() {

				@Override
				public void run() {
					reconnectTask = null;
					r.run();
				}
			};
			int timeInSecs = prefs.getInt(Preferences.RECONNECT_TIME_KEY, 5);
			if (connectionErrorCounter > 20) {
				timeInSecs += 60 * 5;
			} else if (connectionErrorCounter > 10) {
				timeInSecs += 120;
			} else if (connectionErrorCounter > 5) {
				timeInSecs += 60;
			}

			Date d = new Date((new Date()).getTime() + 1000 * timeInSecs);

			timer.schedule(reconnectTask, d);
			notificationUpdateReconnect(d);
		}

	}

	public void refreshInfos() {
		final State state = getState();
		final boolean networkSwitched = getActiveNetworkConnectionType() != usedNetworkType;
		final boolean networkAvailable = getActiveNetworkConnectionType() != null;

		if (DEBUG) {
			Log.d(TAG, "State=" + state + "; networkSwitched=" + networkSwitched);
			NetworkInfo ac = connManager.getActiveNetworkInfo();
			Log.d(TAG, "Current network: " + (ac == null ? "none" : ac.getTypeName()));
		}

		if (networkSwitched && (state == State.connected || state == State.connecting)) {
			if (DEBUG)
				Log.i(TAG, "Network disconnected!");
			try {
				getJaxmpp().disconnect(true);
				usedNetworkType = null;
			} catch (JaxmppException e) {
				Log.w(TAG, "Can't disconnect", e);
			}
		} else if (networkAvailable && (state == State.disconnected)) {
			if (DEBUG)
				Log.i(TAG, "Network available! Reconnecting!");
			if (reconnect) {
				if (connectionErrorCounter < 50)
					reconnect(true);
				else
					notificationUpdateFail();

			}
		}

	}

	private void retrieveVCard(final BareJID jid) {
		try {
			getJaxmpp().getModulesManager().getModule(VCardModule.class).retrieveVCard(JID.jidInstance(jid),
					new VCardAsyncCallback() {

						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
						}

						@Override
						public void onTimeout() throws JaxmppException {
						}

						@Override
						protected void onVCardReceived(VCard vcard) throws XMLException {
							try {
								if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
									ContentValues values = new ContentValues();
									byte[] buffer = Base64.decode(vcard.getPhotoVal());

									values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
									getContentResolver().insert(Uri.parse(RosterProvider.VCARD_URI + "/" + jid.toString()),
											values);
								}
							} catch (Exception e) {
								Log.e("tigase", "WTF?", e);
							}
						}
					});
		} catch (Exception e) {
			Log.e("tigase", "WTF?", e);
		}
	}

	protected void sendUnsentMessages() {
		final Cursor c = getApplication().getContentResolver().query(Uri.parse(ChatHistoryProvider.UNSENT_MESSAGES_URI), null,
				null, null, null);
		try {
			final int columnId = c.getColumnIndex(ChatTableMetaData.FIELD_ID);
			final int columnJid = c.getColumnIndex(ChatTableMetaData.FIELD_JID);
			final int columnMsg = c.getColumnIndex(ChatTableMetaData.FIELD_BODY);
			final int columnThd = c.getColumnIndex(ChatTableMetaData.FIELD_THREAD_ID);

			final JID ownJid = getJaxmpp().getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
			final String nickname = ownJid.getLocalpart();

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
					getJaxmpp().send(msg);

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_ID, id);
					values.put(ChatTableMetaData.FIELD_AUTHOR_JID, ownJid.getBareJid().toString());
					values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME, nickname);
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
		int ico = R.drawable.ic_stat_message;

		String n = (new RosterDisplayTools(getApplicationContext())).getDisplayName(event.getMessage().getFrom().getBareJid());
		if (n == null)
			n = event.getMessage().getFrom().toString();

		String notiticationTitle = "Message from " + n;
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
			notificationManager.notify("chatId:" + event.getChat().getId(), CHAT_NOTIFICATION_ID, notification);

	}

	protected synchronized void updateRosterItem(final PresenceEvent be) throws XMLException {
		RosterItem it = getJaxmpp().getRoster().get(be.getJid().getBareJid());
		if (it != null) {
			Log.i(TAG, "Item " + it.getJid() + " has changed presence");
			Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), it.getId());
			getApplicationContext().getContentResolver().notifyChange(insertedItem, null);

			Element x = be != null && be.getPresence() != null ? be.getPresence().getChildrenNS("x", "vcard-temp:x:update")
					: null;
			if (x != null) {
				for (Element c : x.getChildren()) {
					if (c.getName().equals("photo") && c.getValue() != null) {
						String sha = c.getValue();
						String isha = it.getData("photo");
						if (sha != null && (isha == null || !isha.equalsIgnoreCase(sha))) {
							retrieveVCard(it.getJid());
						}
					} else if (c.getName().equals("photo") && c.getValue() == null) {
					}
				}
			}
		}
	}

}
