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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.*;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import org.tigase.messenger.jaxmpp.android.caps.CapabilitiesDBCache;
import org.tigase.messenger.jaxmpp.android.chat.AndroidChatManager;
import org.tigase.messenger.jaxmpp.android.chat.MarkAsRead;
import org.tigase.messenger.jaxmpp.android.muc.AndroidRoomsManager;
import org.tigase.messenger.jaxmpp.android.roster.AndroidRosterStore;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.account.LoginActivity;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.conversations.muc.MucActivity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.RosterProviderExt;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.request.SubscriptionRequestActivity;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.EntityTimeModule;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatStateExtension;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.muc.XMucUserElement;
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

import javax.net.ssl.SSLSocketFactory;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XMPPService extends Service {

	public static final String CLIENT_PRESENCE_CHANGED_ACTION = "org.tigase.messenger.phone.pro.PRESENCE_CHANGED";

	private static final String KEEPALIVE_ACTION = "org.tigase.messenger.phone.pro.JaxmppService.KEEP_ALIVE";

	private final static String TAG = "XMPPService";

	private static final StanzaExecutor executor = new StanzaExecutor();

	protected final Timer timer = new Timer();

	final ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();

	private final AutopresenceManager autopresenceManager = new AutopresenceManager(this);

	private final IBinder mBinder = new LocalBinder();

	private final MultiJaxmpp multiJaxmpp = new MultiJaxmpp();

	private final DiscoveryModule.ServerFeaturesReceivedHandler streamHandler = new DiscoveryModule.ServerFeaturesReceivedHandler() {

		@Override
		public void onServerFeaturesReceived(final SessionObject sessionObject, IQ stanza, String[] featuresArr) {
			Set<String> features = new HashSet<String>(Arrays.asList(featuresArr));
			if (features.contains(MessageCarbonsModule.XMLNS_MC)) {
				MessageCarbonsModule mc = multiJaxmpp.get(sessionObject).getModule(MessageCarbonsModule.class);
				// if we decide to disable MessageCarbons for some account we
				// may not create module
				// instance at all, so better be prepared for null here
				if (mc != null) {
					try {
						mc.enable(new AsyncCallback() {
							@Override
							public void onError(Stanza responseStanza,
												XMPPException.ErrorCondition error) throws JaxmppException {
								Log.v(TAG,
										"MessageCarbons for account " + sessionObject.getUserBareJid()
												.toString() + " activation failed = " + error
												.toString());
							}

							@Override
							public void onSuccess(Stanza responseStanza) throws JaxmppException {
								Log.v(TAG,
										"MessageCarbons for account " + sessionObject.getUserBareJid()
												.toString() + " activation succeeded");
							}

							@Override
							public void onTimeout() throws JaxmppException {
								Log.v(TAG,
										"MessageCarbons for account " + sessionObject.getUserBareJid()
												.toString() + " activation timeout");
							}

						});
					} catch (JaxmppException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

	};

	private AccountModifyReceiver accountModifyReceiver = new AccountModifyReceiver();

	private CapabilitiesDBCache capsCache;

	private org.tigase.messenger.jaxmpp.android.chat.ChatProvider chatProvider;

	private ConnectivityManager connManager;

	private DataRemover dataRemover;

	private DatabaseHelper dbHelper;

	private Integer focusedOnChatId = null;

	private Integer focusedOnRoomId = null;

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
			if (activity instanceof MucActivity) {
				XMPPService.this.focusedOnRoomId = null;
			}
			autopresenceManager.start();
		}

		@Override
		public void onActivityResumed(Activity activity) {
			Log.i("ActivityLifecycle", "onActivityResumed " + activity);
			if (activity instanceof ChatActivity) {
				int v = ((ChatActivity) activity).getOpenChatId();
				XMPPService.this.focusedOnChatId = v;


				Log.i("ActivityLifecycle", "focusedOnChatId = " + v + "; " + ((ChatActivity) activity).getAccount());
			}
			if (activity instanceof MucActivity) {
				int v = ((MucActivity) activity).getOpenChatId();
				XMPPService.this.focusedOnRoomId = v;
				Log.i("ActivityLifecycle", "focusedOnRoomId = " + v + "; " + ((MucActivity) activity).getAccount());
			}
			autopresenceManager.stop();
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
			// sendAcks();
		}
	};

	private long keepaliveInterval = 1000 * 60 * 3;

	private HashSet<SessionObject> locked = new HashSet<SessionObject>();

	private MessageHandler messageHandler;

	private MobileModeFeature mobileModeFeature;

	private JaxmppCore.LoggedInHandler jaxmppConnectedHandler = new JaxmppCore.LoggedInHandler() {
		@Override
		public void onLoggedIn(SessionObject sessionObject) {
			Log.i("XMPPService", "JAXMPP connected " + sessionObject.getUserBareJid());

			final Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
			try {
				mobileModeFeature.accountConnected(jaxmpp);
			} catch (JaxmppException e) {
				Log.e(TAG,
						"Exception processing MobileModeFeature on connect for account " + sessionObject.getUserBareJid()
								.toString());
			}

			(new SendUnsentMessages(sessionObject)).execute();
			(new RejoinToMucRooms(sessionObject)).execute();
		}
	};

	private MucHandler mucHandler;

	private MessageNotification notificationHelper = new MessageNotification();

	private OwnPresenceFactoryImpl ownPresenceStanzaFactory;

	private PresenceHandler presenceHandler;

	private RosterProviderExt rosterProvider;

	private final PresenceModule.SubscribeRequestHandler subscribeHandler = new PresenceModule.SubscribeRequestHandler() {
		@Override
		public void onSubscribeRequest(SessionObject sessionObject, Presence stanza, BareJID jid) {
			XMPPService.this.processSubscriptionRequest(sessionObject, stanza, jid);
		}

	};

	private SSLSocketFactory sslSocketFactory;

	private int usedNetworkType;

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
			NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			onNetworkChange(netInfo);
		}

	};

	private JaxmppCore.LoggedOutHandler jaxmppDisconnectedHandler = new JaxmppCore.LoggedOutHandler() {
		@Override
		public void onLoggedOut(SessionObject sessionObject) {
			Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
			Log.i("XMPPService", "JAXMPP disconnected " + sessionObject.getUserBareJid());
			if (getUsedNetworkType() != -1) {
				if (jaxmpp != null) {
					XMPPService.this.connectJaxmpp(jaxmpp, 5 * 1000L);
				}
			}
		}
	};

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
		for (final JaxmppCore jaxmpp : multiJaxmpp.get()) {
			connectJaxmpp((Jaxmpp) jaxmpp, (Long) null);
		}
	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Date date) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		long presenceId = sharedPref.getLong("presence", CPresence.OFFLINE);
		if (presenceId == CPresence.OFFLINE) return;

		if (isLocked(jaxmpp.getSessionObject())) {
			Log.v(TAG,
					"cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() + " because it is locked");
			return;
		}

		final Runnable r = new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, "Trying to connect account  " + jaxmpp.getSessionObject().getUserBareJid());

				lock(jaxmpp.getSessionObject(), false);
				if (isDisabled(jaxmpp.getSessionObject())) {
					Log.v(TAG,
							"cancelling connect for " + jaxmpp.getSessionObject()
									.getUserBareJid() + " because it is disabled");
					return;
				}
				setUsedNetworkType(getActiveNetworkType());
				int tmpNetworkType = getUsedNetworkType();
				Log.v(TAG, "Network state is " + tmpNetworkType);
				if (tmpNetworkType != -1) {
					(new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							try {
								if (jaxmpp.isConnected()) {
									Log.v(TAG,
											"cancelling connect for " + jaxmpp.getSessionObject()
													.getUserBareJid() + " because it is connected already");
									return null;
								}

								final Connector.State state = jaxmpp.getSessionObject()
										.getProperty(Connector.CONNECTOR_STAGE_KEY);
								Log.v(TAG,
										"Account " + jaxmpp.getSessionObject()
												.getUserBareJid() + " is in state " + state);
								if (state != null && state != Connector.State.disconnected) {
									Log.v(TAG,
											"cancelling connect for " + jaxmpp.getSessionObject()
													.getUserBareJid() + " because it state " + state);
									return null;
								}

								jaxmpp.getSessionObject().setProperty("messenger#error", null);
								jaxmpp.login();
							} catch (Exception e) {
								if (e.getCause() instanceof SecureTrustManagerFactory.DataCertificateException) {
									jaxmpp.getSessionObject().setProperty("CC:DISABLED", Boolean.TRUE);
									processCertificateError(jaxmpp,
											(SecureTrustManagerFactory.DataCertificateException) e.getCause());
								} else {
									Log.e(TAG,
											"Can't connect account " + jaxmpp.getSessionObject().getUserBareJid(),
											e);
								}

							}

							return null;
						}
					}).execute();


				}
			}
		};
		lock(jaxmpp.getSessionObject(), true);

		if (date == null) {
			Log.d(TAG, "Starting connection NOW");
			r.run();
		} else {
			Log.d(TAG, "Starting connection LATER");
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

	private Jaxmpp createJaxmpp(final BareJID accountJid, final int accountId) {
		final SessionObject sessionObject = new J2SESessionObject();
		sessionObject.setUserProperty(SessionObject.USER_BARE_JID, accountJid);

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = pinfo.versionName;
			sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, versionName);
		} catch (Exception e) {
		}

		PresenceModule.setOwnPresenceStanzaFactory(sessionObject, this.ownPresenceStanzaFactory);
		sessionObject.setUserProperty(Connector.TRUST_MANAGERS_KEY,
				SecureTrustManagerFactory.getTrustManagers(getBaseContext()));

		sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY, getString(R.string.about_application_name));
		sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY, "Android " + android.os.Build.VERSION.RELEASE);

		sessionObject.setUserProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY, "client");
		sessionObject.setUserProperty(DiscoveryModule.IDENTITY_TYPE_KEY, "phone");
		sessionObject.setUserProperty(CapabilitiesModule.NODE_NAME_KEY, "http://tigase.org/messenger");

		sessionObject.setUserProperty("ID", (long) accountId);
		sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
		sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
		sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

		sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
		sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
		sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

		sessionObject.setUserProperty(JaxmppCore.AUTOADD_STANZA_ID_KEY, Boolean.TRUE);

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
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					// geolocationFeature.accountDisconnect(jaxmpp);
					if (jaxmpp.isConnected()) jaxmpp.disconnect(false);
					// is this needed any more??
					if (cleaning || !StreamManagementModule.isResumptionEnabled(jaxmpp.getSessionObject())) {
						XMPPService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
					}
				} catch (Exception e) {
					Log.e(TAG, "cant; disconnect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
				}

				return null;
			}
		}).execute();
	}

	private int getActiveNetworkType() {
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info == null) return -1;
		if (!info.isConnected()) return -1;
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

	protected final Connector.State getState(SessionObject object) {
		Connector.State state = multiJaxmpp.get(object).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state == null ? Connector.State.disconnected : state;
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
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
					try {
						if (jaxmpp.isConnected()) {
							Log.i("XMPPService", "Sending keepAlive for " + jaxmpp.getSessionObject().getUserBareJid());
							jaxmpp.getConnector().keepalive();
							// GeolocationFeature.sendQueuedGeolocation(jaxmpp,
							// JaxmppService.this);
						} else if (Connector.State.disconnecting == jaxmpp.getSessionObject()
								.getProperty(Connector.CONNECTOR_STAGE_KEY)) {
							// if jaxmpp hangs on 'disconnecting' state for more
							// than 45 seconds, stop Connector.
							final Date x = jaxmpp.getSessionObject()
									.getProperty(Connector.CONNECTOR_STAGE_TIMESTAMP_KEY);
							if (x != null && x.getTime() < System.currentTimeMillis() - 45 * 1000) {
								jaxmpp.getConnector().stop(true);
							}
						}
					} catch (JaxmppException ex) {
						Log.e(TAG,
								"error sending keep alive for = " + jaxmpp.getSessionObject().getUserBareJid().toString(),
								ex);
					}
				}
				return null;
			}
		}).execute();
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

		this.ownPresenceStanzaFactory = new OwnPresenceFactoryImpl();
		this.dbHelper = DatabaseHelper.getInstance(this);
		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		this.dataRemover = new DataRemover(this.dbHelper);

		SSLSessionCache sslSessionCache = new SSLSessionCache(this);
		this.sslSocketFactory = SSLCertificateSocketFactory.getDefault(0, sslSessionCache);

		this.rosterProvider = new RosterProviderExt(this, dbHelper, new RosterProviderExt.Listener() {
			@Override
			public void onChange(Long rosterItemId) {
				Uri uri = rosterItemId != null ? ContentUris.withAppendedId(RosterProvider.ROSTER_URI,
						rosterItemId) : RosterProvider.ROSTER_URI;

				Log.i(TAG, "Content change: " + uri);
				getApplicationContext().getContentResolver().notifyChange(uri, null);

			}
		}, "roster_version");
		rosterProvider.resetStatus();

		this.mobileModeFeature = new MobileModeFeature(this);

		this.presenceHandler = new PresenceHandler(this);
		this.messageHandler = new MessageHandler(this);
		this.chatProvider = new org.tigase.messenger.jaxmpp.android.chat.ChatProvider(this,
				dbHelper,
				new org.tigase.messenger.jaxmpp.android.chat.ChatProvider.Listener() {
					@Override
					public void onChange(Long chatId) {
						Uri uri = chatId != null ? ContentUris
								.withAppendedId(
										ChatProvider.OPEN_CHATS_URI,
										chatId) : ChatProvider.OPEN_CHATS_URI;
						getApplicationContext().getContentResolver()
								.notifyChange(
										uri,
										null);
					}
				});
		chatProvider.resetRoomState(CPresence.OFFLINE);
		this.mucHandler = new MucHandler();
		this.capsCache = new CapabilitiesDBCache(dbHelper);

		IntentFilter screenStateReceiverFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		screenStateReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenStateReceiver, screenStateReceiverFilter);

		registerReceiver(presenceChangedReceiver, new IntentFilter(CLIENT_PRESENCE_CHANGED_ACTION));

		registerReceiver(connReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

		multiJaxmpp.addHandler(StreamManagementModule.StreamManagementFailedHandler.StreamManagementFailedEvent.class,
				new StreamManagementModule.StreamManagementFailedHandler() {
					@Override
					public void onStreamManagementFailed(final SessionObject sessionObject,
														 XMPPException.ErrorCondition condition) {
						if (condition != null && condition.getElementName().equals("item-not-found")) {
							XMPPService.this.rosterProvider.resetStatus(sessionObject);
						}
					}
				});
		multiJaxmpp.addHandler(DiscoveryModule.ServerFeaturesReceivedHandler.ServerFeaturesReceivedEvent.class,
				streamHandler);
		multiJaxmpp.addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, jaxmppConnectedHandler);
		multiJaxmpp.addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, jaxmppDisconnectedHandler);
		// multiJaxmpp.addHandler(SocketConnector.ErrorHandler.ErrorEvent.class,
		// );
		//
		// this.connectorListener = new Connector.ErrorHandler() {
		//
		// @Override
		// public void onError(SessionObject sessionObject, StreamError
		// condition, Throwable caught) throws JaxmppException {
		// AbstractSocketXmppSessionLogic.this.processConnectorErrors(condition,
		// caught);
		// }
		// };
		multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class,
				presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.SubscribeRequestHandler.SubscribeRequestEvent.class, subscribeHandler);

		multiJaxmpp.addHandler(MessageModule.MessageReceivedHandler.MessageReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(MessageCarbonsModule.CarbonReceivedHandler.CarbonReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(ChatStateExtension.ChatStateChangedHandler.ChatStateChangedEvent.class, messageHandler);
		multiJaxmpp.addHandler(AuthModule.AuthFailedHandler.AuthFailedEvent.class, new AuthModule.AuthFailedHandler() {

			@Override
			public void onAuthFailed(SessionObject sessionObject, SaslModule.SaslError error) throws JaxmppException {
				processAuthenticationError((Jaxmpp) multiJaxmpp.get(sessionObject));
			}
		});

		multiJaxmpp.addHandler(MucModule.MucMessageReceivedHandler.MucMessageReceivedEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.OccupantLeavedHandler.OccupantLeavedEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.MessageErrorHandler.MessageErrorEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.YouJoinedHandler.YouJoinedEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.StateChangeHandler.StateChangeEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.PresenceErrorHandler.PresenceErrorEvent.class, mucHandler);

		IntentFilter filterAccountModifyReceiver = new IntentFilter();
		filterAccountModifyReceiver.addAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
		filterAccountModifyReceiver.addAction(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
		registerReceiver(accountModifyReceiver, filterAccountModifyReceiver);

		startKeepAlive();

		updateJaxmppInstances();
		// connectAllJaxmpp();
	}

	@Override
	public void onDestroy() {
		Log.i("XMPPService", "Service destroyed");

		unregisterReceiver(screenStateReceiver);
		unregisterReceiver(connReceiver);
		unregisterReceiver(presenceChangedReceiver);
		unregisterReceiver(accountModifyReceiver);

		getApplication().unregisterActivityLifecycleCallbacks(mActivityCallbacks);

		disconnectAllJaxmpp(true);

		super.onDestroy();
		mobileModeFeature = null;

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

	private void processAuthenticationError(final Jaxmpp jaxmpp) {
		Log.e(TAG, "Invalid credentials of account " + jaxmpp.getSessionObject().getUserBareJid());
		jaxmpp.getSessionObject().setUserProperty("CC:DISABLED", true);

		String title = getString(R.string.notification_credentials_error_title,
				jaxmpp.getSessionObject().getUserBareJid().toString());
		String text = getString(R.string.notification_certificate_error_text);

		Intent resultIntent = new Intent(this, LoginActivity.class);
		resultIntent.putExtra("account_name", jaxmpp.getSessionObject().getUserBareJid().toString());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ChatActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent editServerSettingsPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				// .setSmallIcon(R.drawable.ic_messenger_icon)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setTicker(title)
				.setContentTitle(title)
				.setContentText(text)
				.setContentIntent(editServerSettingsPendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		builder.setLights(0xffff0000, 100, 100);

		// getNotificationManager().notify(notificationId, builder.build());

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(("error:" + jaxmpp.getSessionObject().getUserBareJid().toString()).hashCode(),
				builder.build());
	}

	private void processCertificateError(final Jaxmpp jaxmpp,
										 final SecureTrustManagerFactory.DataCertificateException cause) {
		Log.e(TAG,
				"Invalid certificate of account " + jaxmpp.getSessionObject()
						.getUserBareJid() + ": " + cause.getMessage());
		jaxmpp.getSessionObject().setUserProperty("CC:DISABLED", true);

		String title = getString(R.string.notification_certificate_error_title,
				jaxmpp.getSessionObject().getUserBareJid().toString());
		String text = getString(R.string.notification_certificate_error_text);

		Intent resultIntent = new Intent(this, LoginActivity.class);
		resultIntent.putExtra("account_name", jaxmpp.getSessionObject().getUserBareJid().toString());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ChatActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent editServerSettingsPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				// .setSmallIcon(R.drawable.ic_messenger_icon)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setTicker(title)
				.setContentTitle(title)
				.setContentText(text)
				.setContentIntent(editServerSettingsPendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		builder.setLights(0xffff0000, 100, 100);

		// getNotificationManager().notify(notificationId, builder.build());

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(("error:" + jaxmpp.getSessionObject().getUserBareJid().toString()).hashCode(),
				builder.build());
	}

	void processPresenceUpdate() {
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

	private void processSubscriptionRequest(final SessionObject sessionObject,
											final Presence stanza,
											final BareJID jid) {
		Log.e(TAG, "Subscription request from  " + jid);

		retrieveVCard(sessionObject, jid);

		String title = getString(R.string.notification_subscription_request_title, jid.toString());
		String text = getString(R.string.notification_subscription_request_text);

		Intent resultIntent = new Intent(this, SubscriptionRequestActivity.class);
		resultIntent.putExtra("account_name", sessionObject.getUserBareJid().toString());
		resultIntent.putExtra("jid", jid.toString());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ChatActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent editServerSettingsPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				// .setSmallIcon(R.drawable.ic_messenger_icon)
				.setSmallIcon(R.drawable.ic_messenger_icon)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setTicker(title)
				.setContentTitle(title)
				.setContentText(text)
				.setContentIntent(editServerSettingsPendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		builder.setLights(0xff0000ff, 100, 100);

		// getNotificationManager().notify(notificationId, builder.build());

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(("request:" + sessionObject.getUserBareJid().toString() + ":" + jid).hashCode(),
				builder.build());
	}

	private void retrieveVCard(final SessionObject sessionObject, final BareJID jid) {
		try {
			JaxmppCore jaxmpp = multiJaxmpp.get(sessionObject);
			if (jaxmpp == null || !jaxmpp.isConnected()) return;
			// final RosterItem rosterItem = jaxmpp.getRoster().get(jid);
			VCardModule vcardModule = jaxmpp.getModule(VCardModule.class);
			if (vcardModule != null) {
				vcardModule.retrieveVCard(JID.jidInstance(jid),
						(long) 3 * 60 * 1000,
						new VCardModule.VCardAsyncCallback() {

							@Override
							public void onError(Stanza responseStanza,
												XMPPException.ErrorCondition error) throws JaxmppException {
							}

							@Override
							public void onTimeout() throws JaxmppException {
							}

							@Override
							protected void onVCardReceived(VCard vcard) throws XMLException {
								try {
									if (vcard.getPhotoVal() != null && vcard.getPhotoVal()
											.length() > 0) {
										byte[] buffer = tigase.jaxmpp.core.client.Base64.decode(vcard.getPhotoVal());

										rosterProvider.updateVCardHash(sessionObject, jid, buffer);
										Intent intent = new Intent(
												"org.tigase.messenger.phone.pro.AvatarUpdated");
										intent.putExtra("jid", jid.toString());
										XMPPService.this.sendBroadcast(intent);
									}
								} catch (Exception e) {
									Log.e("tigase", "WTF?", e);
								}
							}
						});
			}
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
						Log.e(TAG,
								"error sending ACK for = " + jaxmpp.getSessionObject().getUserBareJid().toString(),
								ex);
					}
				}
				return null;
			}
		}).execute();
	}

	private void sendNotification(SessionObject sessionObject, Chat chat, Message msg) throws JaxmppException {
		Log.i("ActivityLifecycle", "focused=" + focusedOnChatId + "; chatId=" + chat.getId());

		if (this.focusedOnChatId != null && chat.getId() == this.focusedOnChatId) return;

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		if (!sharedPref.getBoolean("notifications_new_message", true)) return;

		notificationHelper.show(this, chat, msg);
	}

	private void sendNotification(SessionObject sessionObject, Room room, Message msg) throws JaxmppException {
		Log.i("ActivityLifecycle", "focused=" + focusedOnRoomId + "; chatId=" + room.getId());

		if (this.focusedOnRoomId != null && room.getId() == this.focusedOnRoomId) return;

		String body = msg.getBody();
		boolean mentioned = body != null && body.toLowerCase().contains(room.getNickname().toLowerCase());

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		if (!mentioned && !sharedPref.getBoolean("notifications_new_groupmessage", true)) return;

		notificationHelper.show(this, room, msg);
	}

	private void startKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, XMPPService.class);
		i.setAction(KEEPALIVE_ACTION);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + keepaliveInterval,
				keepaliveInterval,
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

	private boolean storeMessage(SessionObject sessionObject,
								 Chat chat,
								 tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) throws XMLException {
		// for now let's ignore messages without body element
		if (msg.getBody() == null && msg.getType() != StanzaType.error) return false;
		BareJID authorJid = msg.getFrom() == null ? sessionObject.getUserBareJid() : msg.getFrom().getBareJid();
		String author = authorJid.toString();
		String jid = null;
		if (chat != null) {
			jid = chat.getJid().getBareJid().toString();
		} else {
			jid = (sessionObject.getUserBareJid().equals(authorJid) ? msg.getTo().getBareJid() : authorJid).toString();
		}

		ContentValues values = new ContentValues();
		values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, author);
		values.put(DatabaseContract.ChatHistory.FIELD_JID, jid);

		XmppDelay delay = XmppDelay.extract(msg);
		values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP,
				((delay == null || delay.getStamp() == null) ? new Date() : delay.getStamp()).getTime());

		if (msg.getType() == StanzaType.error) {

			if (chat == null) {
				Log.e(TAG, "Error message from " + jid + " has no Chat. Skipping store.");
				return false;
			}

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
				body += "\n ------ \n";
				body += msg.getBody();
			}
			values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
			values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);
		} else {
			values.put(DatabaseContract.ChatHistory.FIELD_BODY, msg.getBody());

			Element geoloc = msg.getChildrenNS("geoloc", "http://jabber.org/protocol/geoloc");
			if (geoloc != null) {
				values.put(DatabaseContract.ChatHistory.FIELD_DATA, geoloc.getAsString());
				values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
						DatabaseContract.ChatHistory.ITEM_TYPE_LOCALITY);
			} else {
				values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
						DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
			}
		}

		values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, msg.getId());
		if (chat != null) {
			values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
		}
		values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

		// DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;

		if (sessionObject.getUserBareJid().equals(authorJid)) {
			values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_OUT_SENT);
		} else {
			values.put(DatabaseContract.ChatHistory.FIELD_STATE,
					focusedOnChatId != null && chat.getId() == focusedOnChatId ? DatabaseContract.ChatHistory.STATE_INCOMING : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
		}


		Uri uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + jid);
		uri = getContentResolver().insert(uri, values);

		getApplicationContext().getContentResolver()
				.notifyChange(ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI, chat.getId()),
						null);

//		getApplicationContext().getContentResolver().notifyChange(uri, null);
		// context.getContentResolver().insert(uri, values);

		// if (!sessionObject.getUserBareJid().equals(authorJid) &&
		// showNotification
		// && (this.activeChatJid == null ||
		// !this.activeChatJid.getBareJid().equals(authorJid))) {
		// notificationHelper.notifyNewChatMessage(sessionObject, msg);
		// }

		return true;
	}

	private void storeMucSysMsg(SessionObject sessionObject,
								Room room,
								String body) {
		try {
			if (body == null || body == null || room == null) return;

			ContentValues values = new ContentValues();
			values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
			values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, (new Date()).getTime());


			values.put(DatabaseContract.ChatHistory.FIELD_STATE,
					focusedOnRoomId != null && room.getId() == focusedOnRoomId ? DatabaseContract.ChatHistory.STATE_INCOMING : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
			values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
					DatabaseContract.ChatHistory.ITEM_TYPE_SYSTEM_MESSAGE);
			values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);


			values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

			Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + Uri.encode(
					room.getRoomJid().toString()));
			Uri x = getContentResolver().insert(uri, values);
		} catch (Exception ex) {
			Log.e(TAG, "Exception handling received MUC message", ex);
		}

	}

	private final void updateJaxmppInstances() {
		final HashSet<BareJID> accountsJids = new HashSet<BareJID>();
		for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
			accountsJids.add(jaxmpp.getSessionObject().getUserBareJid());
		}

		final AccountManager am = AccountManager.get(this);
		for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
			BareJID accountJid = BareJID.bareJIDInstance(account.name);
			Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
			if (jaxmpp == null) {
				jaxmpp = createJaxmpp(accountJid, account.hashCode());
				multiJaxmpp.add(jaxmpp);
			}

			// workaround for unknown certificate error
			jaxmpp.getSessionObject().setProperty("jaxmpp#ThrowedException", null);

			String password = am.getPassword(account);
			String nickname = am.getUserData(account, AccountsConstants.FIELD_NICKNAME);
			String hostname = am.getUserData(account, AccountsConstants.FIELD_HOSTNAME);
			String resource = am.getUserData(account, AccountsConstants.FIELD_RESOURCE);
			hostname = hostname == null ? null : hostname.trim();

			jaxmpp.getSessionObject().setUserProperty(SessionObject.PASSWORD, password);
			jaxmpp.getSessionObject().setUserProperty(SessionObject.NICKNAME, nickname);
			if (hostname != null && TextUtils.isEmpty(hostname)) hostname = null;
			// sessionObject.setUserProperty(SessionObject.DOMAIN_NAME,
			// hostname);
			if (TextUtils.isEmpty(resource)) resource = null;
			jaxmpp.getSessionObject().setUserProperty(SessionObject.RESOURCE, resource);

			MobileModeFeature.updateSettings(account, jaxmpp, this);

			boolean disabled = !Boolean.parseBoolean(am.getUserData(account, AccountsConstants.FIELD_ACTIVE));
			jaxmpp.getSessionObject().setUserProperty("CC:DISABLED", disabled);

			if (disabled) {
				if (jaxmpp.isConnected()) {
					this.disconnectJaxmpp(jaxmpp, true);
				}
			} else {
				if (!jaxmpp.isConnected()) {
					this.connectJaxmpp(jaxmpp, 1L);
				}
			}

			accountsJids.remove(accountJid);
		}

		for (BareJID accountJid : accountsJids) {
			final Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
			if (jaxmpp != null) {
				multiJaxmpp.remove(jaxmpp);
				(new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						try {
							jaxmpp.disconnect();
							// clear presences for account?
							// app.clearPresences(jaxmpp.getSessionObject(),
							// false);
							// is this needed any more??
							// JaxmppService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
						} catch (Exception ex) {
							Log.e(TAG, "Can't disconnect", ex);
						}

						return null;
					}
				}).execute();
			}
		}

		dataRemover.removeUnusedData(this);
	}

	protected synchronized void updateRosterItem(final SessionObject sessionObject,
												 final Presence p) throws XMLException {
		if (p != null) {
			Element x = p.getChildrenNS("x", "vcard-temp:x:update");
			if (x != null) {
				for (Element c : x.getChildren()) {
					if (c.getName().equals("photo") && c.getValue() != null) {
						boolean retrieve = false;
						final String sha = c.getValue();
						if (sha == null) continue;
						retrieve = !rosterProvider.checkVCardHash(sessionObject, p.getFrom().getBareJid(), sha);

						if (retrieve) retrieveVCard(sessionObject, p.getFrom().getBareJid());
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

	private class AccountModifyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("XMPPService", "Updating accounts !" + intent.getAction());
			updateJaxmppInstances();
			for (JaxmppCore j : multiJaxmpp.get()) {
				Connector.State st = getState(j.getSessionObject());
				if (st == Connector.State.disconnected || st == null) {
					connectJaxmpp((Jaxmpp) j, (Long) null);
				}
			}
		}

	}

	public class LocalBinder extends Binder {

		public XMPPService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return XMPPService.this;
		}
	}

	private class MessageHandler implements MessageModule.MessageReceivedHandler, MessageCarbonsModule.CarbonReceivedHandler, ChatStateExtension.ChatStateChangedHandler {

		private final Context context;

		private final MarkAsRead markAsRead;

		public MessageHandler(XMPPService xmppService) {
			this.context = xmppService.getApplicationContext();
			this.markAsRead = new MarkAsRead(context);
		}

		@Override
		public void onCarbonReceived(SessionObject sessionObject,
									 MessageCarbonsModule.CarbonEventType carbonType,
									 tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
									 Chat chat) {
			try {
				boolean stored = storeMessage(sessionObject, chat, msg);
				if (stored && carbonType == MessageCarbonsModule.CarbonEventType.sent) {
					markAsRead.markChatAsRead(chat.getId(), sessionObject.getUserBareJid(), chat.getJid());
				}
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received carbon message", ex);
			}
		}

		@Override
		public void onChatStateChanged(SessionObject sessionObject, Chat chat, ChatState state) {
			try {
				Log.v(TAG,
						"received chat state chaged event for " + chat.getJid().toString() + ", new state = " + state);
				Uri uri = chat != null ? ContentUris.withAppendedId(ChatProvider.OPEN_CHATS_URI,
						chat.getId()) : ChatProvider.OPEN_CHATS_URI;
				getApplicationContext().getContentResolver().notifyChange(uri, null);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received chat state change event", ex);
			}
		}

		@Override
		public void onMessageReceived(SessionObject sessionObject,
									  Chat chat,
									  tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) {
			try {
				boolean stored = storeMessage(sessionObject, chat, msg);
				if (stored && msg.getBody() != null && !msg.getBody().isEmpty()) {
					sendNotification(sessionObject, chat, msg);
				}
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received message", ex);
			}
		}
	}

	private class MucHandler implements MucModule.MucMessageReceivedHandler, MucModule.YouJoinedHandler, MucModule.MessageErrorHandler, MucModule.StateChangeHandler, MucModule.PresenceErrorHandler, MucModule.OccupantLeavedHandler {

		@Override
		public void onMessageError(SessionObject sessionObject,
								   tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
								   Room room,
								   String nickname,
								   Date timestamp) {
			try {
				Log.e(TAG, "Error from room " + room.getRoomJid() + ", error = " + msg.getAsString());
			} catch (XMLException e) {
			}
			onMucMessageReceived(sessionObject, msg, room, nickname, timestamp);
		}

		@Override
		public void onMucMessageReceived(SessionObject sessionObject,
										 tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
										 Room room,
										 String nickname,
										 Date timestamp) {
			try {
				Log.d(TAG, "Received groupchat message: " + msg.getBody() + "; room=" + room);

				if (msg == null || msg.getBody() == null || room == null) return;
				String body = msg.getBody();

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, nickname);
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, timestamp.getTime());
				values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, msg.getId());

				boolean notify = false;

				if (msg.getType() == StanzaType.error) {
					values.put(DatabaseContract.ChatHistory.FIELD_STATE, DatabaseContract.ChatHistory.STATE_INCOMING);
					values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
							DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);

					ErrorElement error = ErrorElement.extract(msg);
					body = "Error: ";
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
						body += "\n ------ \n";
						body += msg.getBody();
					}

					values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
				} else if (nickname != null && room.getNickname().equals(nickname)) {
					values.put(DatabaseContract.ChatHistory.FIELD_STATE,
							DatabaseContract.ChatHistory.STATE_OUT_DELIVERED);
					values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
							DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE);
					values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
				} else if (nickname != null) {
					values.put(DatabaseContract.ChatHistory.FIELD_STATE,
							focusedOnRoomId != null && room.getId() == focusedOnRoomId ? DatabaseContract.ChatHistory.STATE_INCOMING : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
					values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
							DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE);
					values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
					notify = true;
				} else {
					values.put(DatabaseContract.ChatHistory.FIELD_STATE,
							focusedOnRoomId != null && room.getId() == focusedOnRoomId ? DatabaseContract.ChatHistory.STATE_INCOMING : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
					values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
							DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);
					values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
				}

				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());

				Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + Uri.encode(
						room.getRoomJid().toString()));
				Uri x = getContentResolver().insert(uri, values);

				if (notify && x != null) sendNotification(sessionObject, room, msg);

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
		public void onOccupantLeaved(SessionObject sessionObject, Room room, Occupant occupant, Presence presence, XMucUserElement xMucUserElement) {
			try {
				if (!occupant.getNickname().equals(room.getNickname()) || xMucUserElement.getStatuses() == null)
					return;
				if (xMucUserElement.getStatuses().contains(301)) {
					storeMucSysMsg(sessionObject, room, "You are banned from the room");
				} else if (xMucUserElement.getStatuses().contains(307)) {
					storeMucSysMsg(sessionObject, room, "You are kicked from room");
				} else if (xMucUserElement.getStatuses().contains(321)) {
					storeMucSysMsg(sessionObject, room, "You are removed from the room because of an affiliation change");
				} else if (xMucUserElement.getStatuses().contains(322)) {
					storeMucSysMsg(sessionObject, room, "You are removed from the room because the room has been changed to members-only and you are not a member");
				} else if (xMucUserElement.getStatuses().contains(332)) {
					storeMucSysMsg(sessionObject, room, "You are removed from the room because the MUC service is being shut down");
				}

			} catch (XMLException e) {
				Log.e(TAG, "Exception handling", e);
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
		public void onStateChange(SessionObject sessionObject,
								  Room room,
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
			(new SendUnsentGroupMessages(room)).execute();

		}

	}

	private class OwnPresenceFactoryImpl implements PresenceModule.OwnPresenceStanzaFactory {

		@Override
		public Presence create(SessionObject sessionObject) {
			try {
				Presence presence = Presence.create();

				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

				int defaultPresence = Long.valueOf(sharedPref.getLong("presence", CPresence.ONLINE)).intValue();
				int presenceId = Long.valueOf(sharedPref.getLong("auto_presence", defaultPresence)).intValue();

				Log.d(TAG, "Before presence send. defaultPresence=" + defaultPresence + "; presenceId=" + presenceId);

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

				return presence;
			} catch (JaxmppException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class PresenceHandler implements PresenceModule.ContactAvailableHandler, PresenceModule.ContactUnavailableHandler, PresenceModule.ContactChangedPresenceHandler, PresenceModule.ContactUnsubscribedHandler {

		private final XMPPService jaxmppService;

		public PresenceHandler(XMPPService jaxmppService) {
			this.jaxmppService = jaxmppService;
		}

		@Override
		public void onContactAvailable(SessionObject sessionObject,
									   Presence stanza,
									   JID jid,
									   Presence.Show show,
									   String status,
									   Integer priority) throws JaxmppException {
			updateRosterItem(sessionObject, stanza);
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onContactChangedPresence(SessionObject sessionObject,
											 Presence stanza,
											 JID jid,
											 Presence.Show show,
											 String status,
											 Integer priority) throws JaxmppException {
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

		@Override
		public void onContactUnsubscribed(SessionObject sessionObject, Presence stanza, BareJID jid) {
			try {
				updateRosterItem(sessionObject, stanza);
			} catch (JaxmppException ex) {
				Log.v(TAG, "Exception updating roster item presence", ex);
			}
			rosterProvider.updateStatus(sessionObject, JID.jidInstance(jid));
		}
	}

	private class RejoinToMucRooms extends AsyncTask<Void, Void, Void> {

		private final SessionObject sessionObject;

		public RejoinToMucRooms(SessionObject sessionObject) {
			this.sessionObject = sessionObject;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.i(TAG, "Rejoining to MUC Rooms. Account=" + sessionObject.getUserBareJid());
			try {
				Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);
				for (Room room : mucModule.getRooms()) {
					Log.d(TAG, "Room " + room.getRoomJid() + " is in state " + room.getState());
					if (room.getState() != Room.State.joined) {
						Log.d(TAG, "Rejoinning to " + room.getRoomJid());


						room.rejoin();
					} else {
						(new SendUnsentGroupMessages(room)).execute();
					}
				}
			} catch (JaxmppException e) {
				Log.e(TAG,
						"Exception while rejoining to rooms on connect for account " + sessionObject.getUserBareJid()
								.toString());
			}

			return null;
		}
	}

	private class ScreenStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Boolean screenOff = null;
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				screenOff = true;
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				screenOff = false;
			}
			if (screenOff != null) {
				sendAcks();
				mobileModeFeature.setMobileMode(screenOff);
			}
		}
	}

	private class SendUnsentGroupMessages extends AsyncTask<Void, Void, Void> {

		private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_STANZA_ID,
				DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		private final Room room;

		public SendUnsentGroupMessages(Room room) {
			this.room = room;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d("SendUnsentGroupMessages", "Sending unsent MUC " + room.getRoomJid() + " messages ");

			Uri u = Uri.parse(ChatProvider.UNSENT_MESSAGES_URI + "/" + room.getSessionObject().getUserBareJid());

			try (Cursor c = getContentResolver().query(u,
					cols,
					DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "==? AND " + DatabaseContract.ChatHistory.FIELD_JID + "=?",
					new String[]{
							"" + DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE,
							room.getRoomJid().toString()},
					DatabaseContract.ChatHistory.FIELD_TIMESTAMP)) {
				while (c.moveToNext()) {
					final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
					final JID toJid = JID.jidInstance(c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID)));
					final String threadId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_THREAD_ID));
					final String body = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
					final String stanzaId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STANZA_ID));

					Log.d("SendUnsentGroupMessages", "Preparing " + id + ": " + body);

					JaxmppCore jaxmpp = getJaxmpp(room.getSessionObject().getUserBareJid());

					if (jaxmpp.isConnected()) {
						try {
							Message msg = Message.create();
							msg.setTo(toJid);
							msg.setType(StanzaType.groupchat);
							msg.setThread(threadId);
							msg.setBody(body);
							msg.setId(stanzaId);

							room.sendMessage(msg);

							ContentValues values = new ContentValues();
							values.put(DatabaseContract.ChatHistory.FIELD_STATE,
									DatabaseContract.ChatHistory.STATE_OUT_SENT);
							values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, System.currentTimeMillis());
							getContentResolver().update(Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + room.getSessionObject()
									.getUserBareJid() + "/" + toJid
									.getBareJid() + "/" + id), values, null, null);
						} catch (JaxmppException e) {
							Log.w("XMPPService", "Cannot send unsent message", e);
						}
					} else {
						Log.w("XMPPService", "Can't find chat object for message");
					}
				}
			}

			return null;
		}
	}

	private class SendUnsentMessages extends AsyncTask<Void, Void, Void> {

		private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_STANZA_ID,
				DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		private final SessionObject sessionObject;

		public SendUnsentMessages(SessionObject sessionObject) {
			this.sessionObject = sessionObject;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Uri u = Uri.parse(ChatProvider.UNSENT_MESSAGES_URI + "/" + sessionObject.getUserBareJid());
			try (Cursor c = getContentResolver().query(u,
					cols,
					DatabaseContract.ChatHistory.FIELD_ITEM_TYPE + "!=?",
					new String[]{
							"" + DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE},
					DatabaseContract.ChatHistory.FIELD_TIMESTAMP)) {
				while (c.moveToNext()) {
					final int id = c.getInt(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
					final JID toJid = JID.jidInstance(c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID)));
					final String threadId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_THREAD_ID));
					final String body = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
					final String stanzaId = c.getString(c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STANZA_ID));

					JaxmppCore jaxmpp = getJaxmpp(sessionObject.getUserBareJid());
					MessageModule messageModule = jaxmpp.getModule(MessageModule.class);
					if (jaxmpp.isConnected()) {
						try {
							Message msg = Message.create();
							msg.setTo(toJid);
							msg.setType(StanzaType.chat);
							msg.setThread(threadId);
							msg.setBody(body);
							msg.setId(stanzaId);
							messageModule.sendMessage(msg);
							ContentValues values = new ContentValues();
							values.put(DatabaseContract.ChatHistory.FIELD_STATE,
									DatabaseContract.ChatHistory.STATE_OUT_SENT);
							getContentResolver().update(Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + sessionObject.getUserBareJid() + "/" + toJid
									.getBareJid() + "/" + id), values, null, null);
						} catch (JaxmppException e) {
							Log.w("XMPPService", "Cannot send unsent message", e);
						}
					} else {
						Log.w("XMPPService", "Can't find chat object for message");
					}
				}
			}

			return null;
		}
	}
}
