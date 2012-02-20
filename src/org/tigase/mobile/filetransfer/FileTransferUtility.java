package org.tigase.mobile.filetransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.tigase.mobile.filetransfer.FileTransferModule.Host;
import org.tigase.mobile.filetransfer.FileTransferModule.StreamhostsCallback;

import android.util.Log;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.DiscoInfoAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.DiscoItemsAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.Item;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;

public class FileTransferUtility {

	private static final String TAG = "FileTransferUtility";
	
	public static interface ProxyDiscoveryAsyncCallback {
		
		void onResult(JID jid);
		
		void onError(String errorMessage);
		
	}
	
	public static void discoverProxy(final Jaxmpp jaxmpp, final ProxyDiscoveryAsyncCallback callback) {
		new Thread(new Runnable() {
			public void run() {
				final DiscoItemsModule discoItemsModule = jaxmpp.getModulesManager().getModule(DiscoItemsModule.class);
				try {
					JID jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
					discoItemsModule.getItems(JID.jidInstance(jid.getDomain()), new DiscoItemsAsyncCallback() {
						@Override
						public void onError(Stanza responseStanza,
								ErrorCondition error) throws JaxmppException {
							callback.onError("proxy discovery failed");
						}

						@Override
						public void onTimeout() throws JaxmppException {
							callback.onError("proxy discovery timed out");						
						}

						@Override
						public void onInfoReceived(String attribute,
								ArrayList<Item> items) throws XMLException {
							final int all = items.size();
							if (all == 0) {
								callback.onError("proxy component not found");								
							}
							else {
								discoverProxy2(jaxmpp, items, callback);
							}
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
	
	private static void discoverProxy2(final Jaxmpp jaxmpp, final ArrayList<Item> items, final ProxyDiscoveryAsyncCallback callback) {
		final AtomicInteger counter = new AtomicInteger(0);
		final DiscoInfoModule discoInfoModule = jaxmpp.getModulesManager().getModule(DiscoInfoModule.class);
		final List<JID> proxyComponents = Collections.synchronizedList(new ArrayList<JID>());
		for (final Item item : items) {			
			try {
				discoInfoModule.getInfo(item.getJid(), new DiscoInfoAsyncCallback() {
					
					@Override
					public void onError(Stanza responseStanza,
							ErrorCondition error)
							throws JaxmppException {
						// TODO Auto-generated method stub
						checkFinished();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						// TODO Auto-generated method stub
						checkFinished();
					}

					@Override
					protected void onInfoReceived(String node,
							Collection<Identity> identities,
							Collection<String> features)
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
					
					protected void checkFinished() {
						int count = counter.addAndGet(1);
						if (count == items.size()) {
							discoverProxy3(proxyComponents, callback);
						}
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
			callback.onError("proxy not found");
		}
		else {
			callback.onResult(proxyComponents.get(0));
		}		
	}
		
    public static abstract class StreamhostUsedCallback implements AsyncCallback {
        
        private List<Host> hosts;
        
        public void setHosts(List<Host> hosts) {
                this.hosts = hosts;
        }
        
        public List<Host> getHosts() {
                return this.hosts;
        }
    }        
    
//	public static void onStreamAccepted(final Jaxmpp jaxmpp, final JID recipient, final Uri uri, final String sid) {
	public static void onStreamAccepted(final FileTransfer ft) {
		final FileTransferModule ftModule = ft.jaxmpp.getModulesManager().getModule(FileTransferModule.class);
		FileTransferUtility.discoverProxy(ft.jaxmpp, new ProxyDiscoveryAsyncCallback() {
			public void onResult(final JID proxyJid) {
				try {
					ftModule.requestStreamhosts(proxyJid, new StreamhostsCallback(ftModule) {
						@Override
						public void onError(Stanza responseStanza,
								ErrorCondition error)
								throws JaxmppException {
							// TODO Auto-generated method stub
							Log.v(TAG, "streamhost request failed for "+proxyJid.toString());
							ft.transferError("connection failed");
						}

						@Override
						public void onTimeout() throws JaxmppException {
							// TODO Auto-generated method stub							
							Log.v(TAG, "streamhost request timedout for "+proxyJid.toString());
							ft.transferError("connection timed out");
						}

						@Override
						public void onStreamhosts(List<Host> hosts) {
							// TODO Auto-generated method stub
							Log.v(TAG, "streamhost request succeeded for "+proxyJid.toString());
							ft.setProxyJid(JID.jidInstance(hosts.get(0).getJid()));
							FileTransferUtility.onStreamhostsReceived(ft, hosts);
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
			
			public void onError(String errorMessage) {
				ft.transferError(errorMessage);
			}
			
		});		
	}
	
//	public static void onStreamhostsReceived(final Jaxmpp jaxmpp, final JID recipient, final Uri uri, final String sid, final JID proxyJid, final List<Host> hosts) {
	public static void onStreamhostsReceived(final FileTransfer ft, final List<Host> hosts) {
		final FileTransferModule ftModule = ft.jaxmpp.getModulesManager().getModule(FileTransferModule.class);

        final StreamhostUsedCallback streamused = new StreamhostUsedCallback() {            
            
            public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
                    Log.v(TAG, "streamhost-used resulted in error = " + error.getElementName());
            }

            public void onSuccess(Stanza responseStanza) throws JaxmppException {            	
                    IQ iq = (IQ) responseStanza;
                    Element query = iq.getChildrenNS("query", FileTransferModule.XMLNS_BS);
                    String streamhostUsed = query.getFirstChild().getAttribute("jid");
                    for (Host host : getHosts()) {
                            if (streamhostUsed.equals(host.getJid())) {
                                    try {
                                            ft.connectToProxy(host);
                                    }
                                    catch (Exception ex) {
                                            Log.e(TAG, "exception connecting to proxy", ex);
                                            ft.transferError("connection error");
                                            //stop();
                                    }
                            }
                    }
            }

            public void onTimeout() throws JaxmppException {
                    Log.v(TAG, "streamhost-used timed out");
            }
            
        };
				
		streamused.setHosts(hosts);		
		try {
			ftModule.sendStreamhosts(ft.buddyJid,  ft.sid, hosts, streamused);
		} catch (XMLException e) {
			Log.e(TAG, "WTF?", e);
			ft.transferError("internal error");
		} catch (JaxmppException e) {
			Log.e(TAG, "WTF?", e);			
			ft.transferError("internal error");
		}
	}
    
}

