/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package org.tigase.messenger.phone.pro.omemo;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OMEMOSyncService
		extends IntentService
		implements ServiceConnection {

	public static final String ACTION_CLEAN = "OMEMOSyncService.CLEAN_ALL";
	public static final String ACTION_CHECK_OWN_KEY = "OMEMOSyncService.ACTION_CHECK_OWN_KEY";
	public static final String EXTRA_ACCOUNT = "OMEMOSyncService.EXTRA_ACCOUNT";

	private static final String TAG = "OMEMOSyncService";
	private final static String prefix = "eu.siacs.conversations.axolotl.bundles:";
	private final Object lock = new Object();
	private XMPPService mService;

	public static void startCheckOwnKey(Context context, String accountName) {
		Intent intent = new Intent(context, OMEMOSyncService.class);
		intent.setAction(ACTION_CHECK_OWN_KEY);
		intent.putExtra(EXTRA_ACCOUNT, accountName);
		context.startService(intent);
	}

	public static void startOMEMOClean(Context context, String accountName) {
		Intent intent = new Intent(context, OMEMOSyncService.class);
		intent.setAction(ACTION_CLEAN);
		intent.putExtra(EXTRA_ACCOUNT, accountName);
		context.startService(intent);
	}

	public OMEMOSyncService() {
		super("OMEMOSyncService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, XMPPService.class), this, 0);
	}

	@Override
	public void onDestroy() {
		unbindService(this);
		super.onDestroy();
	}

	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		XMPPService.LocalBinder binder = (XMPPService.LocalBinder) iBinder;
		this.mService = binder.getService();
		Log.w(TAG, "CONNECTED");
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {
		this.mService = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			synchronized (lock) {
				while (mService == null) {
					lock.wait();
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_CHECK_OWN_KEY.equals(action)) {
				final String accountName = intent.getStringExtra(EXTRA_ACCOUNT);
				final Jaxmpp jaxmpp = mService.getJaxmpp(accountName);
				try {
					checkOwnKeyPublished(jaxmpp);
				} catch (Exception e) {
					Log.e(TAG, "Cannot cleanup OMEMO keys", e);
				}
			} else if (ACTION_CLEAN.equals(action)) {
				final String accountName = intent.getStringExtra(EXTRA_ACCOUNT);
				final Jaxmpp jaxmpp = mService.getJaxmpp(accountName);
				try {
					cleanOMEMO(jaxmpp);
				} catch (Exception e) {
					Log.e(TAG, "Cannot cleanup OMEMO keys", e);
				}
			}
		}
	}

	private void checkOwnKeyPublished(final Jaxmpp jaxmpp) throws JaxmppException {
		final BareJID jid = jaxmpp.getSessionObject().getUserBareJid();
		final OmemoModule omemo = jaxmpp.getModule(OmemoModule.class);
		final OMEMOStoreImpl store = (OMEMOStoreImpl) OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		final int localKeyId = store.getLocalRegistrationId();

		final DiscoveryModule discovery = jaxmpp.getModule(DiscoveryModule.class);
		Log.i(TAG, "Checking own key published.");
		discovery.getItems(JID.jidInstance(jid), "eu.siacs.conversations.axolotl.bundles:" + localKeyId,
						   new DiscoveryModule.DiscoItemsAsyncCallback() {
							   @Override
							   public void onInfoReceived(String attribute, ArrayList<DiscoveryModule.Item> items)
									   throws XMLException {
								   Log.d(TAG, "Received Bundle information");
								   boolean found = false;
								   for (DiscoveryModule.Item item : items) {
									   found = found | (item.getName().equals("current") &&
											   item.getJid().getBareJid().equals(jid));
								   }
								   if (!found) {
									   Log.i(TAG, "Bundle has invalid structure?");
									   try {
										   omemo.publishDeviceList();
									   } catch (JaxmppException ignore) {
									   }
								   }
							   }

							   @Override
							   public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									   throws JaxmppException {
								   Log.d(TAG, "Received Bundle error: " + error);
								   if (error == XMPPException.ErrorCondition.item_not_found) {
									   Log.i(TAG, "Local key is not published.");
									   omemo.publishDeviceList();
								   }
							   }

							   @Override
							   public void onTimeout() throws JaxmppException {
							   }

						   });

	}

	private void cleanOMEMO(final Jaxmpp jaxmpp) throws JaxmppException {
		final BareJID jid = jaxmpp.getSessionObject().getUserBareJid();

		final OMEMOStoreImpl store = (OMEMOStoreImpl) OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		final OmemoModule omemo = jaxmpp.getModule(OmemoModule.class);
		final PubSubModule pubsub = jaxmpp.getModule(PubSubModule.class);

		store.reset(jid.toString());

		pubsub.retrieveItem(jid, OmemoModule.DEVICELIST_NODE, new PubSubModule.RetrieveItemsAsyncCallback() {
			@Override
			public void onTimeout() throws JaxmppException {
				Log.w(TAG, "Device list retrieve timeout");
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				if (errorCondition == XMPPException.ErrorCondition.item_not_found) {
					retrievePublishedBoundles(jaxmpp, new ArrayList<>());
				} else {
					Log.w(TAG, "Device list retrieve error: " + errorCondition);
				}
			}

			@Override
			protected void onRetrieve(IQ responseStanza, String nodeName, Collection<Item> items) {
				try {
					for (Item item : items) {
						if (item.getId().equals("current")) {
							final ArrayList<Integer> ids = new ArrayList<>();
							for (Element dev : item.getPayload().getChildren("device")) {
								ids.add(Integer.valueOf(dev.getAttribute("id")));
							}
							retrievePublishedBoundles(jaxmpp, ids);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	private void retrievePublishedBoundles(final Jaxmpp jaxmpp, ArrayList<Integer> publishedDeviceList)
			throws JaxmppException {
		final DiscoveryModule disco = jaxmpp.getModule(DiscoveryModule.class);
		disco.getItems(JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid()),
					   new DiscoveryModule.DiscoItemsAsyncCallback() {
						   @Override
						   public void onInfoReceived(String attribute, ArrayList<DiscoveryModule.Item> items)
								   throws XMLException {
							   final ArrayList<Integer> publishedBoundles = new ArrayList<>();
							   for (DiscoveryModule.Item item : items) {
								   if (item.getNode().startsWith(prefix)) {
									   publishedBoundles.add(
											   Integer.valueOf(item.getNode().substring(prefix.length())));
								   }
							   }
							   try {
								   synchronizeAll(jaxmpp, publishedDeviceList, publishedBoundles);
							   } catch (Exception e) {
								   e.printStackTrace();
							   }
						   }

						   @Override
						   public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
								   throws JaxmppException {
							   Log.w(TAG, "Boundles list retrieve error: " + error);
						   }

						   @Override
						   public void onTimeout() throws JaxmppException {
							   Log.w(TAG, "Boundles list retrieve timeout");
						   }

					   });
	}

	private void synchronizeAll(final Jaxmpp jaxmpp, final List<Integer> publishedDeviceList,
								final List<Integer> publishedBoundles) throws JaxmppException {
		final BareJID jid = jaxmpp.getSessionObject().getUserBareJid();
		final PubSubModule pubsub = jaxmpp.getModule(PubSubModule.class);
		final OmemoModule omemo = jaxmpp.getModule(OmemoModule.class);

		Log.i(TAG, "Found published Bundles: " + publishedBoundles);
		Log.i(TAG, "Found published Devices: " + publishedDeviceList);

		final ArrayList<Integer> newDeviceList = new ArrayList<>();
		for (Integer id : publishedDeviceList) {
			if (publishedBoundles.contains(id)) {
				newDeviceList.add(id);
			}
		}

		for (Integer id : publishedBoundles) {
			if (!newDeviceList.contains(id)) {
				pubsub.deleteNode(jid, prefix + id, new PubSubAsyncCallback() {
					@Override
					public void onSuccess(Stanza responseStanza) throws JaxmppException {
					}

					@Override
					public void onTimeout() throws JaxmppException {
						Log.w(TAG, "Bundles delete timeout");
					}

					@Override
					protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
										  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
						Log.w(TAG, "Bundles delete error: " + errorCondition);

					}
				});
			}
		}

		ElementBuilder bldr = ElementBuilder.create("list", "eu.siacs.conversations.axolotl");
		for (Integer id : newDeviceList) {
			bldr.child("device").setAttribute("id", String.valueOf(id)).up();
		}

		final JabberDataElement options = new JabberDataElement(XDataType.submit);
		options.addTextSingleField("pubsub#access_model", "open");

		pubsub.publishItem(null, OmemoModule.DEVICELIST_NODE, "current", bldr.getElement(), options,
						   new PubSubModule.PublishAsyncCallback() {
							   @Override
							   public void onPublish(String itemId) {
								   Log.i(TAG, "Device list is published.");
								   try {
									   omemo.publishDeviceList();
								   } catch (JaxmppException e) {
									   e.printStackTrace();
								   }
							   }

							   @Override
							   public void onTimeout() throws JaxmppException {
								   Log.w(TAG, "Device list is not published: Timeout");
							   }

							   @Override
							   protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
													 PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
								   Log.w(TAG, "Device list is not published: " + errorCondition + ", " +
										   pubSubErrorCondition);
							   }
						   });

	}

}