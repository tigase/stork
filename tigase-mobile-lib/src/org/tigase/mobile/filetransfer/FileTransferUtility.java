package org.tigase.mobile.filetransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.tigase.mobile.Features;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.DiscoInfoAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.DiscoItemsAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.Item;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.util.Log;

public class FileTransferUtility {

	public static interface ProxyDiscoveryAsyncCallback {

		void onError(String errorMessage);

		void onResult(JID jid);

	}

	public static abstract class StreamhostUsedCallback implements AsyncCallback {

		private List<Streamhost> hosts;

		public List<Streamhost> getHosts() {
			return this.hosts;
		}

		public void setHosts(List<Streamhost> hosts) {
			this.hosts = hosts;
		}
	}

	public static final String[] FEATURES = { Features.BYTESTREAMS, Features.FILE_TRANSFER };

	private static final String TAG = "FileTransferUtility";

	public static void discoverProxy(final Jaxmpp jaxmpp, final ProxyDiscoveryAsyncCallback callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final DiscoItemsModule discoItemsModule = jaxmpp.getModule(DiscoItemsModule.class);
				try {
					JID jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
					discoItemsModule.getItems(JID.jidInstance(jid.getDomain()), new DiscoItemsAsyncCallback() {
						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
							callback.onError("not supported by this server");
						}

						@Override
						public void onInfoReceived(String attribute, ArrayList<Item> items) throws XMLException {
							final int all = items.size();
							if (all == 0) {
								callback.onError("not supported by this server");
							} else {
								discoverProxy2(jaxmpp, items, callback);
							}
						}

						@Override
						public void onTimeout() throws JaxmppException {
							callback.onError("proxy discovery timed out");
						}
					});
				} catch (XMLException e) {
					Log.e(TAG, "WTF?", e);
					callback.onError("internal error");
				} catch (JaxmppException e) {
					Log.e(TAG, "WTF?", e);
					callback.onError("internal error");
				}
			}
		}).start();
	}

	private static void discoverProxy2(final Jaxmpp jaxmpp, final ArrayList<Item> items,
			final ProxyDiscoveryAsyncCallback callback) {
		final AtomicInteger counter = new AtomicInteger(0);
		final DiscoInfoModule discoInfoModule = jaxmpp.getModule(DiscoInfoModule.class);
		final List<JID> proxyComponents = Collections.synchronizedList(new ArrayList<JID>());
		for (final Item item : items) {
			try {
				discoInfoModule.getInfo(item.getJid(), new DiscoInfoAsyncCallback(null) {

					protected void checkFinished() {
						int count = counter.addAndGet(1);
						if (count == items.size()) {
							discoverProxy3(proxyComponents, callback);
						}
					}

					@Override
					public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
						// TODO Auto-generated method stub
						checkFinished();
					}

					@Override
					protected void onInfoReceived(String node, Collection<Identity> identities, Collection<String> features)
							throws XMLException {
						if (identities != null) {
							for (Identity identity : identities) {
								if ("proxy".equals(identity.getCategory()) && "bytestreams".equals(identity.getType())) {
									proxyComponents.add(item.getJid());
								}
							}
						}

						checkFinished();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						// TODO Auto-generated method stub
						checkFinished();
					}
				});
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				int count = counter.addAndGet(1);
				if (count == items.size()) {
					discoverProxy3(proxyComponents, callback);
				}
			}
		}
	}

	private static void discoverProxy3(final List<JID> proxyComponents, final ProxyDiscoveryAsyncCallback callback) {
		if (proxyComponents.isEmpty()) {
			callback.onError("not supported by this server");
		} else {
			callback.onResult(proxyComponents.get(0));
		}
	}

	public static JID getBestJidForFeatures(Jaxmpp jaxmpp, BareJID jid, String[] features) {
		try {
			CapabilitiesCache capsCache = jaxmpp.getModule(CapabilitiesModule.class).getCache();
			Set<String> nodes = capsCache.getNodesWithFeature(features[0]);

			for (int i = 1; i < features.length; i++) {
				nodes.retainAll(capsCache.getNodesWithFeature(features[i]));
			}

			Presence current = null;
			Map<String, Presence> allPresences = jaxmpp.getPresence().getPresences(jid);
			if (allPresences != null) {
				for (Presence p : allPresences.values()) {
					Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
					if (c == null)
						continue;

					final String node = c.getAttribute("node");
					final String ver = c.getAttribute("ver");

					if (nodes.contains(node + "#" + ver)) {
						if (current == null || current.getPriority() < p.getPriority())
							current = p;
					}
				}
			}

			return (current != null) ? current.getFrom() : null;
		} catch (XMLException ex) {
			return null;
		}
	}

	// public static void onStreamAccepted(final Jaxmpp jaxmpp, final JID
	// recipient, final Uri uri, final String sid) {
	public static void onStreamAccepted(final FileTransfer ft) {
		final FileTransferModule ftModule = ft.jaxmpp.getModule(FileTransferModule.class);
		FileTransferUtility.discoverProxy(ft.jaxmpp, new ProxyDiscoveryAsyncCallback() {
			@Override
			public void onError(String errorMessage) {
				// ft.transferError(errorMessage);
				AndroidFileTransferUtility.onStreamhostsReceived(ft, new ArrayList<Streamhost>());
			}

			@Override
			public void onResult(final JID proxyJid) {
				try {
					ftModule.requestStreamhosts(proxyJid, new StreamhostsCallback(ftModule) {
						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
							// TODO Auto-generated method stub
							Log.v(TAG, "streamhost request failed for " + proxyJid.toString());
							ft.transferError("connection failed");
						}

						@Override
						public void onStreamhosts(List<Streamhost> hosts) {
							// TODO Auto-generated method stub
							Log.v(TAG, "streamhost request succeeded for " + proxyJid.toString());
							ft.setProxyJid(JID.jidInstance(hosts.get(0).getJid()));
							AndroidFileTransferUtility.onStreamhostsReceived(ft, hosts);
						}

						@Override
						public void onTimeout() throws JaxmppException {
							// TODO Auto-generated method stub
							Log.v(TAG, "streamhost request timedout for " + proxyJid.toString());
							ft.transferError("connection timed out");
						}

					});
				} catch (XMLException e) {
					Log.e(TAG, "WTF?", e);
					ft.transferError("internal error");
				} catch (JaxmppException e) {
					Log.e(TAG, "WTF?", e);
					ft.transferError("internal error");
				}
			}

		});
	}

	// public static void onStreamhostsReceived(final Jaxmpp jaxmpp, final JID
	// recipient, final Uri uri, final String sid, final JID proxyJid, final
	// List<Host> hosts) {
	public static void onStreamhostsReceived(final FileTransfer ft, final List<Streamhost> hosts) {
		final FileTransferModule ftModule = ft.jaxmpp.getModule(FileTransferModule.class);

		final StreamhostUsedCallback streamused = new StreamhostUsedCallback() {

			@Override
			public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
				Log.v(TAG, "streamhost-used resulted in error = " + error.getElementName());
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				IQ iq = (IQ) responseStanza;
				Element query = iq.getChildrenNS("query", FileTransferModule.XMLNS_BS);
				String streamhostUsed = query.getFirstChild().getAttribute("jid");
				boolean connected = false;
				for (Streamhost host : getHosts()) {

					// is it possible that we try to activate same record twice?
					// we get 'connection error' but also 'activation for
					// xxx@sss succeeded'
					// how it is possible?

					if (streamhostUsed.equals(host.getJid())) {
						try {
							Log.v(TAG, "activating stream for = " + host.getJid());
							if (host.getJid().equals(ft.jid.toString())) {
								ft.outgoingConnected();
							} else {
								ft.connectToProxy(host, null);
							}
							Log.v(TAG, "activation of stream completed");
							connected = true;
							Log.v(TAG, "connected set to = " + connected);
							break;
						} catch (Exception ex) {
							Log.e(TAG, "exception connecting to proxy", ex);
							// stop();
						}
					}
				}
				if (!connected) {
					Log.v(TAG, "result = " + connected);
					ft.transferError("connection error");
				}
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.v(TAG, "streamhost-used timed out");
			}

		};

		streamused.setHosts(hosts);
		try {
			ftModule.sendStreamhosts(ft.buddyJid, ft.sid, hosts, streamused);
		} catch (XMLException e) {
			Log.e(TAG, "WTF?", e);
			ft.transferError("internal error");
		} catch (JaxmppException e) {
			Log.e(TAG, "WTF?", e);
			ft.transferError("internal error");
		}
	}

	public static boolean resourceContainsFeatures(Jaxmpp jaxmpp, JID jid, String[] features) throws XMLException {
		CapabilitiesCache capsCache = jaxmpp.getModule(CapabilitiesModule.class).getCache();
		Set<String> nodes = capsCache.getNodesWithFeature(features[0]);

		for (int i = 1; i < features.length; i++) {
			nodes.retainAll(capsCache.getNodesWithFeature(features[i]));
		}

		Presence p = jaxmpp.getPresence().getPresence(jid);
		if (p == null)
			return false;

		Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
		if (c == null)
			return false;

		final String node = c.getAttribute("node");
		final String ver = c.getAttribute("ver");

		return nodes.contains(node + "#" + ver);
	}

}
