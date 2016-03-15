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
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.RosterProviderExt;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.android.roster.AndroidRosterStore;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;

public class XMPPService extends Service {

    private final static String TAG = "XMPPService";
    private static final StanzaExecutor executor = new StanzaExecutor();
    protected final Timer timer = new Timer();
    private final MultiJaxmpp multiJaxmpp = new MultiJaxmpp();
    private DatabaseHelper dbHelper;
    private ConnectivityManager connManager;
    private int usedNetworkType;
    private RosterProviderExt rosterProvider;
    private PresenceHandler presenceHandler;
    private HashSet<SessionObject> locked = new HashSet<SessionObject>();

    public class LocalBinder extends Binder {
        public XMPPService getService() {
            // Return this instance of LocalService so clients can call public methods
            return XMPPService.this;
        }
    }


    public XMPPService() {
        Logger logger = Logger.getLogger("tigase.jaxmpp");
        Handler handler = new AndroidLoggingHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

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

        sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, "XXX");
        sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY, getString(R.string.app_name));
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

        return jaxmpp;
    }

    private int getActiveNetworkType() {
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info == null)
            return -1;
        if (!info.isConnected())
            return -1;
        return info.getType();
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

    private void lock(SessionObject sessionObject, boolean value) {
        synchronized (locked) {
            if (value) {
                locked.add(sessionObject);
            } else {
                locked.remove(sessionObject);
            }
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;

    }

    public MultiJaxmpp getMultiJaxmpp(){
        return this.multiJaxmpp;
    }

    public Jaxmpp getJaxmpp(String account) {
        return this.multiJaxmpp.get(BareJID.bareJIDInstance(account));
    }

    public Jaxmpp getJaxmpp(BareJID account) {
        return this.multiJaxmpp.get(account);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AvatarHelper.initilize(this);
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

        multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class, presenceHandler);
        multiJaxmpp.addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class, presenceHandler);
        multiJaxmpp.addHandler(PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class, presenceHandler);
        multiJaxmpp.addHandler(PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent.class, presenceHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + this.hashCode());

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

        return super.onStartCommand(intent, flags, startId);
    }

    private void retrieveVCard(final SessionObject sessionObject, final BareJID jid) {
        try {
            JaxmppCore jaxmpp = multiJaxmpp.get(sessionObject);
            if (jaxmpp == null)
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

    private class PresenceHandler implements PresenceModule.ContactAvailableHandler, PresenceModule.ContactUnavailableHandler,
            PresenceModule.ContactChangedPresenceHandler, PresenceModule.BeforePresenceSendHandler {

        private final XMPPService jaxmppService;

        public PresenceHandler(XMPPService jaxmppService) {
            this.jaxmppService = jaxmppService;
        }

        @Override
        public void onBeforePresenceSend(SessionObject sessionObject, Presence presence) throws JaxmppException {
            // presence.setStatus(userStatusMessage);
            // if (focused) {
            // presence.setShow(userStatusShow);
            // presence.setPriority(prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY,
            // 5));
            // } else {
            // presence.setShow(Presence.Show.away);
            // presence.setPriority(prefs.getInt(Preferences.AWAY_PRIORITY_KEY,
            // 0));
            // }
            // activityFeature.beforePresenceSend(prefs, presence);
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
