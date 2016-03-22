/*
 * XMPPService.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.RosterProviderExt;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.android.caps.CapabilitiesDBCache;
import tigase.jaxmpp.android.chat.AndroidChatManager;
import tigase.jaxmpp.android.muc.AndroidRoomsManager;
import tigase.jaxmpp.android.roster.AndroidRosterStore;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.EntityTimeModule;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatStateExtension;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.*;
import tigase.jaxmpp.core.client.xmpp.utils.delay.XmppDelay;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class XMPPService extends Service {

	public static final String CLIENT_PRESENCE_CHANGED_ACTION = "org.tigase.messenger.phone.pro.PRESENCE_CHANGED";
	private static final String KEEPALIVE_ACTION = "org.tigase.messenger.phone.pro.JaxmppService.KEEP_ALIVE";
	private final static String TAG = "XMPPService";
	private static final StanzaExecutor executor = new StanzaExecutor();
	protected final Timer timer = new Timer();
	private final MultiJaxmpp multiJaxmpp = new MultiJaxmpp();
	private final IBinder mBinder = new LocalBinder();
	private Integer focusedOnChatId = null;
	private final Application.ActivityLifecycleCallbacks mActivityCallbacks = new Application.ActivityLifecycleCallbacks() {
		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
			Log.i("ActivityLifecycle", "onActivityCreated " + activity);
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityDestroyed " + activity);

		}

		@Override
		public void onActivityPaused(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityPaused " + activity);
			if (activity instanceof ChatActivity) {
				XMPPService.this.focusedOnChatId = null;
			}
		}

		@Override
		public void onActivityResumed(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityResumed " + activity);
			if (activity instanceof ChatActivity) {
				int v = ((ChatActivity) activity).getOpenChatId();
				XMPPService.this.focusedOnChatId = v;
				Log.i("ActivityLifecycle", "focusedOnChatId = " + v + "; " + ((ChatActivity) activity).getAccount());
			}

		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
			Log.i("ActivityLifecycle", "onActivitySaveInstanceState " + activity);

		}

		@Override
		public void onActivityStarted(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityStarted " + activity);

		}

		@Override
		public void onActivityStopped(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityStopped " + activity);
			sendAcks();
		}
	};

	private long keepaliveInterval = 1000 * 60 * 3;
	private DatabaseHelper dbHelper;
	private ConnectivityManager connManager;
	private int usedNetworkType;
	private RosterProviderExt rosterProvider;
	final BroadcastReceiver presenceChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long presenceId = intent.getLongExtra("presence", CPresence.ONLINE);
			if (presenceId == CPresence.OFFLINE) {
				disconnectAllJaxmpp(true);
			} else {
				processPresenceUpdate();
			}
		}
	};
	private final BroadcastReceiver connReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(
					Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			onNetworkChange(netInfo);
		}

	};
	private PresenceHandler presenceHandler;
	private HashSet<SessionObject> locked = new HashSet<SessionObject>();
	private JaxmppCore.ConnectedHandler jaxmppConnectedHandler = new JaxmppCore.ConnectedHandler() {
		@Override
		public void onConnected(SessionObject sessionObject) {
			Log.i("XMPPService", "JAXMPP connected " + sessionObject.getUserBareJid());
			(new SendUnsentMessages(sessionObject)).execute();
		}
	};
	private MessageHandler messageHandler;
	private tigase.jaxmpp.android.chat.ChatProvider chatProvider;
	private MucHandler mucHandler;
	private JaxmppCore.DisconnectedHandler jaxmppDisconnectedHandler = new JaxmppCore.DisconnectedHandler() {
		@Override
		public void onDisconnected(SessionObject sessionObject) {
			Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
			Log.i("XMPPService", "JAXMPP disconnected " + sessionObject.getUserBareJid());
			if (getUsedNetworkType() != -1) {
				if (jaxmpp != null) {
					XMPPService.this.connectJaxmpp(jaxmpp, 5 * 1000L);
				}
			}
		}
	};

	private CapabilitiesDBCache capsCache;

	public XMPPService() {
		Logger logger = Logger.getLogger("tigase.jaxmpp");
		Handler handler = new AndroidLoggingHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

	}

	private void connectAllJaxmpp(Long delay) {
		setUsedNetworkType(getActiveNetworkType());
		// geolocationFeature.registerLocationListener();

		for (final JaxmppCore jaxmpp : multiJaxmpp.get()) {
			Log.v(TAG, "connecting account " + jaxmpp.getSessionObject().getUserBareJid());
			connectJaxmpp((Jaxmpp) jaxmpp, delay);
		}
	}

	private void connectAllJaxmpp() {
		AccountManager am = AccountManager.get(this);
		for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
			BareJID accountJid = BareJID.bareJIDInstance(account.name);
			Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
			if (jaxmpp == null) {

				jaxmpp = createJaxmpp(account, am);
				multiJaxmpp.add(jaxmpp);
				connectJaxmpp(jaxmpp, (Long) null);
			}
		}
	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Date date) {
		if (isLocked(jaxmpp.getSessionObject())) {
			Log.v(TAG, "cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() + " because it is locked");
			return;
		}

		final Runnable r = new Runnable() {
			@Override
			public void run() {
				lock(jaxmpp.getSessionObject(), false);
				if (isDisabled(jaxmpp.getSessionObject())) {
					Log.v(TAG,
							"cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() + " because it is disabled");
					return;
				}
				setUsedNetworkType(getActiveNetworkType());
				if (getUsedNetworkType() != -1) {
					final Connector.State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
					if (state == null || state == Connector.State.disconnected) {
						(new Thread() {
							@Override
							public void run() {
								try {
									if (jaxmpp.isConnected())
										return;

									jaxmpp.getSessionObject().setProperty("messenger#error", null);
									jaxmpp.login();
								} catch (Exception e) {
									// incrementConnectionError(jaxmpp.getSessionObject().getUserBareJid());
									Log.e(TAG, "Can't connect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
								}
							}
						}).start();
					}
				}
			}
		};
		lock(jaxmpp.getSessionObject(), true);

		if (date == null) {
			r.run();
		} else {
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					r.run();
				}
			}, date);
		}
	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Long delay) {
		connectJaxmpp(jaxmpp, delay == null ? null : new Date(delay + System.currentTimeMillis()));
	}

	private Jaxmpp createJaxmpp(final Account account, final AccountManager am) {
		BareJID accountJid = BareJID.bareJIDInstance(account.name);
		String password = am.getPassword(account);
		String nickname = am.getUserData(account, AccountsConstants.FIELD_NICKNAME);
		String hostname = am.getUserData(account, AccountsConstants.FIELD_HOSTNAME);
		String resource = am.getUserData(account, AccountsConstants.FIELD_RESOURCE);
		hostname = hostname == null ? null : hostname.trim();

		final SessionObject sessionObject = new J2SESessionObject();

		sessionObject.setUserProperty(SessionObject.USER_BARE_JID, accountJid);
		sessionObject.setUserProperty(SessionObject.PASSWORD, password);
		sessionObject.setUserProperty(SessionObject.NICKNAME, nickname);
		if (hostname != null && TextUtils.isEmpty(hostname))
			hostname = null;
		// sessionObject.setUserProperty(SessionObject.DOMAIN_NAME, hostname);
		if (TextUtils.isEmpty(resource))
			resource = null;
		sessionObject.setUserProperty(SessionObject.RESOURCE, resource);

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = pinfo.versionName;
			sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, versionName);
		} catch (Exception e) {
		}

		sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY, getString(R.string.about_application_name));
		sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY, "Android " + android.os.Build.VERSION.RELEASE);

		sessionObject.setUserProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY, "client");
		sessionObject.setUserProperty(DiscoveryModule.IDENTITY_TYPE_KEY, "phone");
		sessionObject.setUserProperty(CapabilitiesModule.NODE_NAME_KEY, "http://tigase.org/messenger");

		sessionObject.setUserProperty("ID", (long) account.hashCode());
		sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
		sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
		sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

		sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
		sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
		sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

		// sessionObject.setUserProperty(SocketConnector.SSL_SOCKET_FACTORY_KEY,
		// sslSocketFactory);

		final Jaxmpp jaxmpp = new Jaxmpp(sessionObject);
		jaxmpp.setExecutor(executor);

		RosterModule.setRosterStore(sessionObject, new AndroidRosterStore(this.rosterProvider));
		jaxmpp.getModulesManager().register(new RosterModule(this.rosterProvider));
		PresenceModule.setPresenceStore(sessionObject, new J2SEPresenceStore());
		jaxmpp.getModulesManager().register(new PresenceModule());
		jaxmpp.getModulesManager().register(new VCardModule());

		AndroidChatManager chatManager = new AndroidChatManager(this.chatProvider);
		MessageModule messageModule = new MessageModule(chatManager);
		jaxmpp.getModulesManager().register(messageModule);

		messageModule.addExtension(new ChatStateExtension(chatManager));

		jaxmpp.getModulesManager().register(new MucModule(new AndroidRoomsManager(this.chatProvider)));
		jaxmpp.getModulesManager().register(new PingModule());
		jaxmpp.getModulesManager().register(new EntityTimeModule());

		CapabilitiesModule capsModule = new CapabilitiesModule();
		capsModule.setCache(capsCache);
		jaxmpp.getModulesManager().register(capsModule);

		try {
			jaxmpp.getModulesManager().register(new MessageCarbonsModule());
		} catch (JaxmppException ex) {
			Log.v(TAG, "Exception creating instance of MessageCarbonsModule", ex);
		}

		return jaxmpp;
	}

	private void disconnectAllJaxmpp(final boolean cleaning) {
		setUsedNetworkType(-1);
		// if (geolocationFeature != null) {
		// geolocationFeature.unregisterLocationListener();
		// }
		for (final JaxmppCore j : multiJaxmpp.get()) {
			disconnectJaxmpp((Jaxmpp) j, cleaning);
		}

		// synchronized (connectionErrorsCounter) {
		// connectionErrorsCounter.clear();
		// }
	}

	private void disconnectJaxmpp(final Jaxmpp jaxmpp, final boolean cleaning) {
		(new Thread() {
			@Override
			public void run() {
				try {
					// geolocationFeature.accountDisconnect(jaxmpp);
					jaxmpp.disconnect(false);
					// is this needed any more??
					XMPPService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
				} catch (Exception e) {
					Log.e(TAG, "cant; disconnect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
				}
			}
		}).start();
	}

	private int getActiveNetworkType() {
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info == null)
			return -1;
		if (!info.isConnected())
			return -1;
		return info.getType();
	}

	public Jaxmpp getJaxmpp(String account) {
		return this.multiJaxmpp.get(BareJID.bareJIDInstance(account));
	}

	public Jaxmpp getJaxmpp(BareJID account) {
		return this.multiJaxmpp.get(account);
	}

	public MultiJaxmpp getMultiJaxmpp() {
		return this.multiJaxmpp;
	}

	private int getUsedNetworkType() {
		return this.usedNetworkType;
	}

	private void setUsedNetworkType(int type) {
		this.usedNetworkType = type;
	}

	public boolean isDisabled(SessionObject sessionObject) {
		Boolean x = sessionObject.getProperty("CC:DISABLED");
		return x == null ? false : x;
	}

	private boolean isLocked(SessionObject sessionObject) {
		synchronized (locked) {
			return locked.contains(sessionObject);
		}
	}

	private void keepAlive() {
		new Thread() {
			@Override
			public void run() {
				for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
					try {
						if (jaxmpp.isConnected()) {
							Log.i("XMPPService", "Sending keepAlive for " + jaxmpp.getSessionObject().getUserBareJid());
							jaxmpp.getConnector().keepalive();
							// GeolocationFeature.sendQueuedGeolocation(jaxmpp,
							// JaxmppService.this);
						}
					} catch (JaxmppException ex) {
						Log.e(TAG, "error sending keep alive for = " + jaxmpp.getSessionObject().getUserBareJid().toString(),
								ex);
					}
				}
			}
		}.start();
	}

	private void lock(SessionObject sessionObject, boolean value) {
		synchronized (locked) {
			if (value) {
				locked.add(sessionObject);
			} else {
				locked.remove(sessionObject);
			}
		}
	}

	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i("XMPPService", "Service started");

		getApplication().registerActivityLifecycleCallbacks(mActivityCallbacks);

		this.dbHelper = new DatabaseHelper(this);
		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		this.dbHelper = new DatabaseHelper(this);
		this.rosterProvider = new RosterProviderExt(this, dbHelper, new RosterProviderExt.Listener() {
			@Override
			public void onChange(Long rosterItemId) {
				Uri uri = rosterItemId != null ? ContentUris.withAppendedId(RosterProvider.ROSTER_URI, rosterItemId)
						: RosterProvider.ROSTER_URI;

				Log.i(TAG, "Content change: " + uri);
				getApplicationContext().getContentResolver().notifyChange(uri, null);

			}
		}, "roster_version");
		rosterProvider.resetStatus();

		this.presenceHandler = new PresenceHandler(this);
		this.messageHandler = new MessageHandler();
		this.chatProvider = new tigase.jaxmpp.android.chat.ChatProvider(this, dbHelper,
				new tigase.jaxmpp.android.chat.ChatProvider.Listener() {
					@Override
					public void onChange(Long chatId) {
						Uri uri = chatId != null ? ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chatId)
								: ChatProvider.OPEN_CHATS_URI;
						getApplicationContext().getContentResolver().notifyChange(uri, null);
					}
				});
		chatProvider.resetRoomState(CPresence.OFFLINE);
		this.mucHandler = new MucHandler();
		this.capsCache = new CapabilitiesDBCache(dbHelper);

		registerReceiver(presenceChangedReceiver, new IntentFilter(CLIENT_PRESENCE_CHANGED_ACTION));

		IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(connReceiver, filter);

		multiJaxmpp.addHandler(JaxmppCore.ConnectedHandler.ConnectedEvent.class, jaxmppConnectedHandler);
		multiJaxmpp.addHandler(JaxmppCore.DisconnectedHandler.DisconnectedEvent.class, jaxmppDisconnectedHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent.class, presenceHandler);

		multiJaxmpp.addHandler(MessageModule.MessageReceivedHandler.MessageReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(MessageCarbonsModule.CarbonReceivedHandler.CarbonReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(ChatStateExtension.ChatStateChangedHandler.ChatStateChangedEvent.class, messageHandler);

		startKeepAlive();

		connectAllJaxmpp();
	}

	@Override
	public void onDestroy() {
		Log.i("XMPPService", "Service destroyed");

		unregisterReceiver(connReceiver);
		unregisterReceiver(presenceChangedReceiver);
		getApplication().unregisterActivityLifecycleCallbacks(mActivityCallbacks);

		disconnectAllJaxmpp(true);

		super.onDestroy();
		sendBroadcast(new Intent(ServiceRestarter.ACTION_NAME));
	}

	private void onNetworkChange(final NetworkInfo netInfo) {
		if (netInfo != null && netInfo.isConnected()) {
			connectAllJaxmpp(5000l);
		} else {
			disconnectAllJaxmpp(false);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && "connect-all".equals(intent.getAction())) {
			connectAllJaxmpp();
		} else if (intent != null && KEEPALIVE_ACTION.equals(intent.getAction())) {
			keepAlive();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	private void processPresenceUpdate() {

		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
					try {
						if (!jaxmpp.isConnected()) {
							connectJaxmpp((Jaxmpp) jaxmpp, (Long) null);
						} else {
							jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
						}
					} catch (JaxmppException e) {
						Log.e("TAG", "Can't update presence", e);
					}
				}
				return null;
			}
		}).execute();
	}

	private void retrieveVCard(final SessionObject sessionObject, final BareJID jid) {
		try {
			JaxmppCore jaxmpp = multiJaxmpp.get(sessionObject);
			if (jaxmpp == null || !jaxmpp.isConnected())
				return;
			// final RosterItem rosterItem = jaxmpp.getRoster().get(jid);
			VCardModule vcardModule = jaxmpp.getModule(VCardModule.class);
			if (vcardModule != null)
				vcardModule.retrieveVCard(JID.jidInstance(jid), (long) 3 * 60 * 1000, new VCardModule.VCardAsyncCallback() {

					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
					}

					@Override
					public void onTimeout() throws JaxmppException {
					}

					@Override
					protected void onVCardReceived(VCard vcard) throws XMLException {
						try {
							if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
								byte[] buffer = Base64.decode(vcard.getPhotoVal());

								rosterProvider.updateVCardHash(sessionObject, jid, buffer);
								Intent intent = new Intent("org.tigase.messenger.phone.pro.AvatarUpdated");
								intent.putExtra("jid", jid.toString());
								XMPPService.this.sendBroadcast(intent);
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

	private void sendAcks() {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {

				for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
					try {
						if (jaxmpp.isConnected()) {
							Log.d("XMPPService", "Sending ACK for " + jaxmpp.getSessionObject().getUserBareJid());
							jaxmpp.getModule(StreamManagementModule.class).sendAck();
						}
					} catch (JaxmppException ex) {
						Log.e(TAG, "error sending ACK for = " + jaxmpp.getSessionObject().getUserBareJid().toString(), ex);
					}
				}
				return null;
			}
		}).execute();
	}

	private void sendNotification(SessionObject sessionObject, Chat chat, Message msg) throws XMLException {
		Log.i("ActivityLifecycle", "focused=" + focusedOnChatId + "; chatId=" + chat.getId());

		if (this.focusedOnChatId != null && chat.getId() == this.focusedOnChatId)
			return;

		String title = chat.getJid().getBareJid().toString();
		String text = msg.getBody();

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(
				R.mipmap.ic_launcher).setContentTitle(title).setContentText(text);

		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		mBuilder.setAutoCancel(true);
		mBuilder.setSound(soundUri);
		mBuilder.setCategory(Notification.CATEGORY_MESSAGE);

		Intent resultIntent = new Intent(this, ChatActivity.class);
		resultIntent.putExtra("openChatId", (int) chat.getId());
		resultIntent.putExtra("jid", chat.getJid().getBareJid().toString());
		resultIntent.putExtra("account", sessionObject.getUserBareJid().toString());

		// The stack builder object will contain an artificial back stack for
		// the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ChatActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(("chat:" + chat.getId()).hashCode(), mBuilder.build());

	}

	private void startKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, XMPPService.class);
		i.setAction(KEEPALIVE_ACTION);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + keepaliveInterval, keepaliveInterval,
				pi);
	}

	private void stopKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, XMPPService.class);
		i.setAction(KEEPALIVE_ACTION);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	private void storeMessage(SessionObject sessionObject, Chat chat, tigase.jaxmpp.core.client.xmpp.stanzas.Message msg)
			throws XMLException {
		// for now let's ignore messages without body element
		if (msg.getBody() == null && msg.getType() != StanzaType.error)
			return;
		BareJID authorJid = msg.getFrom() == null ? sessionObject.getUserBareJid() : msg.getFrom().getBareJid();
		String author = authorJid.toString();
		String jid = null;
		if (chat != null) {
			jid = chat.getJid().getBareJid().toString();
		} else {
			jid = (sessionObject.getUserBareJid().equals(authorJid) ? msg.getTo().getBareJid() : authorJid).toString();
		}

		Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + Uri.encode(jid));

		ContentValues values = new ContentValues();
		values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, author);
		values.put(DatabaseContract.ChatHistory.FIELD_JID, jid);

		XmppDelay delay = XmppDelay.extract(msg);
		values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP,
				((delay == null || delay.getStamp() == null) ? new Date() : delay.getStamp()).getTime());
		if (msg.getType() == StanzaType.error) {
			ErrorElement error = ErrorElement.extract(msg);
			String body = "Error: ";
			if (error != null) {
				if (error.getText() != null) {
					body += error.getText();
				} else {
					XMPPException.ErrorCondition errorCondition = error.getCondition();
					if (errorCondition != null) {
						body += errorCondition.getElementName();
					}
				}
			}
			if (msg.getBody() != null) {
				body += " ------ ";
				body += msg.getBody();
			}
			values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
		} else {
			values.put(DatabaseContract.ChatHistory.FIELD_BODY, msg.getBody());
		}
		values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, msg.getId());
		if (chat != null) {
			values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
		}
		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

		int type = DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;
		Element geoloc = msg.getChildrenNS("geoloc", "http://jabber.org/protocol/geoloc");
		if (geoloc != null) {
			values.put(DatabaseContract.ChatHistory.FIELD_DATA, geoloc.getAsString());
			type = DatabaseContract.ChatHistory.ITEM_TYPE_LOCALITY;
		}
		values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, type);
		values.put(DatabaseContract.ChatHistory.FIELD_STATE, sessionObject.getUserBareJid().equals(authorJid)
				? DatabaseContract.ChatHistory.STATE_OUT_SENT : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long id = db.insert(DatabaseContract.ChatHistory.TABLE_NAME, null, values);
		Log.v(TAG, "inserted message - id = " + id);

		getApplicationContext().getContentResolver().notifyChange(
				ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chat.getId()), null);
		getApplicationContext().getContentResolver().notifyChange(uri, null);
		// context.getContentResolver().insert(uri, values);

		// if (!sessionObject.getUserBareJid().equals(authorJid) &&
		// showNotification
		// && (this.activeChatJid == null ||
		// !this.activeChatJid.getBareJid().equals(authorJid))) {
		// notificationHelper.notifyNewChatMessage(sessionObject, msg);
		// }
	}

	protected synchronized void updateRosterItem(final SessionObject sessionObject, final Presence p) throws XMLException {
		if (p != null) {
			Element x = p.getChildrenNS("x", "vcard-temp:x:update");
			if (x != null) {
				for (Element c : x.getChildren()) {
					if (c.getName().equals("photo") && c.getValue() != null) {
						boolean retrieve = false;
						final String sha = c.getValue();
						if (sha == null)
							continue;
						retrieve = !rosterProvider.checkVCardHash(sessionObject, p.getFrom().getBareJid(), sha);

						if (retrieve)
							retrieveVCard(sessionObject, p.getFrom().getBareJid());
					}
				}
			}
		}

		// Synchronize contact status
		BareJID from = p.getFrom().getBareJid();
		PresenceStore store = PresenceModule.getPresenceStore(sessionObject);
		Presence bestPresence = store.getBestPresence(from);
		// SyncAdapter.syncContactStatus(getApplicationContext(),
		// sessionObject.getUserBareJid(), from, bestPresence);
	}

	private class SendUnsentMessages extends AsyncTask<Void, Void, Void> {

		private final String[] cols = new String[] { DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_STANZA_ID,
				DatabaseContract.ChatHistory.FIELD_TIMESTAMP };

		private final SessionObject sessionObject;

		public SendUnsentMessages(SessionObject sessionObject) {
			this.sessionObject = sessionObject;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Uri u = Uri.parse(ChatProvider.UNSENT_MESSAGES_URI + "/" + sessionObject.getUserBareJid());
			Cursor c = getContentResolver().query(u, cols, null, null, DatabaseContract.ChatHistory.FIELD_TIMESTAMP);
			try {
				while (c.moveToNext()) {
					final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
					final JID toJid = JID.jidInstance(c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID)));
					final String threadId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_THREAD_ID));
					final String body = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
					final String stanzaId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STANZA_ID));

					JaxmppCore jaxmpp = getJaxmpp(sessionObject.getUserBareJid());
					if (jaxmpp.isConnected()) {
						try {
							Message msg = Message.create();
							msg.setTo(toJid);
							msg.setType(StanzaType.chat);
							msg.setThread(threadId);
							msg.setBody(body);
							msg.setId(stanzaId);

							jaxmpp.send(msg);

							ContentValues values = new ContentValues();
							values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_OUT_SENT);
							getContentResolver().update(Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/"
									+ sessionObject.getUserBareJid() + "/" + toJid.getBareJid() + "/" + id), values, null,
									null);
						} catch (JaxmppException e) {
							Log.w("XMPPService", "Cannot send unsent message", e);
						}
					} else {
						Log.w("XMPPService", "Can't find chat object for message");
					}
				}
			} finally {
				c.close();
			}

			return null;
		}
	}

	private class MessageHandler implements MessageModule.MessageReceivedHandler, MessageCarbonsModule.CarbonReceivedHandler,
			ChatStateExtension.ChatStateChangedHandler {

		@Override
		public void onCarbonReceived(SessionObject sessionObject, MessageCarbonsModule.CarbonEventType carbonType,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg, Chat chat) {
			try {
				storeMessage(sessionObject, chat, msg);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received carbon message", ex);
			}
		}

		@Override
		public void onChatStateChanged(SessionObject sessionObject, Chat chat, ChatState state) {
			try {
				Log.v(TAG, "received chat state chaged event for " + chat.getJid().toString() + ", new state = " + state);
				Uri uri = chat != null ? ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chat.getId())
						: ChatProvider.OPEN_CHATS_URI;
				getApplicationContext().getContentResolver().notifyChange(uri, null);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received chat state change event", ex);
			}
		}

		@Override
		public void onMessageReceived(SessionObject sessionObject, Chat chat,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) {
			try {

				storeMessage(sessionObject, chat, msg);
				if (msg.getBody() != null && !msg.getBody().isEmpty())
					sendNotification(sessionObject, chat, msg);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received message", ex);
			}
		}
	}

	private class MucHandler implements MucModule.MucMessageReceivedHandler, MucModule.YouJoinedHandler,
			MucModule.MessageErrorHandler, MucModule.StateChangeHandler, MucModule.PresenceErrorHandler {

		@Override
		public void onMessageError(SessionObject sessionObject, tigase.jaxmpp.core.client.xmpp.stanzas.Message message,
				Room room, String nickname, Date timestamp) {
			try {
				Log.e(TAG, "Error from room " + room.getRoomJid() + ", error = " + message.getAsString());
			} catch (XMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onMucMessageReceived(SessionObject sessionObject, tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
				Room room, String nickname, Date timestamp) {
			try {
				if (msg == null || msg.getBody() == null || room == null)
					return;
				String body = msg.getBody();
				Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/"
						+ Uri.encode(room.getRoomJid().toString()));

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, nickname);
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, timestamp.getTime());
				values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
				values.put(DatabaseContract.ChatHistory.FIELD_STATE, 0);
				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

				getContentResolver().insert(uri, values);

				// if (activeChatJid == null ||
				// !activeChatJid.getBareJid().equals(room.getRoomJid())) {
				// if
				// (body.toLowerCase().contains(room.getNickname().toLowerCase()))
				// {
				// notificationHelper.notifyNewMucMessage(sessionObject, msg);
				// }
				// }
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received MUC message", ex);
			}

		}

		@Override
		public void onPresenceError(SessionObject sessionObject, Room room, Presence presence, String nickname) {
			Intent intent = new Intent();

			// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			// intent.setAction(MainActivity.ERROR_ACTION);
			intent.putExtra("account", sessionObject.getUserBareJid().toString());
			intent.putExtra("jid", "" + room.getRoomJid().toString());
			intent.putExtra("type", "muc");

			try {
				XMPPException.ErrorCondition c = presence.getErrorCondition();
				if (c != null) {
					intent.putExtra("errorCondition", c.name());
					intent.putExtra("errorMessage", c.name());
				} else {
					intent.putExtra("errorCondition", "-");
					intent.putExtra("errorMessage", "-");
				}
			} catch (XMLException ex) {
				ex.printStackTrace();
			}

			// if (focused) {
			// intent.setAction(ERROR_MESSAGE);
			// sendBroadcast(intent);
			// } else {
			// intent.setClass(getApplicationContext(), MainActivity.class);
			// notificationHelper.showMucError(room.getRoomJid().toString(),
			// intent);
			// }
		}

		@Override
		public void onStateChange(SessionObject sessionObject, Room room,
				tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State oldState,
				tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State newState) {
			Log.v(TAG, "room " + room.getRoomJid() + " changed state from " + oldState + " to " + newState);
			int state = CPresence.OFFLINE;
			switch (newState) {
			case joined:
				state = CPresence.ONLINE;
				break;
			default:
				state = CPresence.OFFLINE;
			}
			chatProvider.updateRoomState(sessionObject, room.getRoomJid(), state);
		}

		@Override
		public void onYouJoined(SessionObject sessionObject, Room room, String asNickname) {
			// TODO Auto-generated method stub
			Log.v(TAG, "joined room " + room.getRoomJid() + " as " + asNickname);
		}

	}

	public class LocalBinder extends Binder {
		public XMPPService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return XMPPService.this;
		}
	}

	private class PresenceHandler implements PresenceModule.ContactAvailableHandler, PresenceModule.ContactUnavailableHandler,
			PresenceModule.ContactChangedPresenceHandler, PresenceModule.BeforePresenceSendHandler {

		private final XMPPService jaxmppService;

		public PresenceHandler(XMPPService jaxmppService) {
			this.jaxmppService = jaxmppService;
		}

		@Override
		public void onBeforePresenceSend(SessionObject sessionObject, Presence presence) throws JaxmppException {
			SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
			int presenceId = Long.valueOf(sharedPref.getLong("presence", CPresence.ONLINE)).intValue();

			switch (presenceId) {
			case CPresence.OFFLINE:
				presence.setType(StanzaType.unavailable);
				break;
			case CPresence.DND:
				presence.setShow(Presence.Show.dnd);
				break;
			case CPresence.XA:
				presence.setShow(Presence.Show.xa);
				break;
			case CPresence.AWAY:
				presence.setShow(Presence.Show.away);
				break;
			case CPresence.ONLINE:
				presence.setShow(Presence.Show.online);
				break;
			case CPresence.CHAT:
				presence.setShow(Presence.Show.chat);
				break;

			}
		}

		@Override
		public void onContactAvailable(SessionObject sessionObject, Presence stanza, JID jid, Presence.Show show, String status,
				Integer priority) throws JaxmppException {
			updateRosterItem(sessionObject, stanza);
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onContactChangedPresence(SessionObject sessionObject, Presence stanza, JID jid, Presence.Show show,
				String status, Integer priority) throws JaxmppException {
			updateRosterItem(sessionObject, stanza);
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onContactUnavailable(SessionObject sessionObject, Presence stanza, JID jid, String status) {
			try {
				updateRosterItem(sessionObject, stanza);
			} catch (JaxmppException ex) {
				Log.v(TAG, "Exception updating roster item presence", ex);
			}
			rosterProvider.updateStatus(sessionObject, jid);
		}

	}
}
