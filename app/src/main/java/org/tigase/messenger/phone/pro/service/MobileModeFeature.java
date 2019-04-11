/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
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

package org.tigase.messenger.phone.pro.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

import java.util.TimerTask;

public class MobileModeFeature {

	public static final String MOBILE_OPTIMIZATIONS_ENABLED = Features.MOBILE_V1 + "#enabled";

	public static final String MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT = Features.MOBILE_V1 + "#presence_queue_timeout";

	private static final String TAG = "MobileModeFeature";
	private final XMPPService jaxmppService;
	private boolean mobileModeEnabled = false;
	private TimerTask setMobileModeTask;

	public static void updateSettings(Account account, JaxmppCore jaxmpp, Context context) {
		AccountManager accountManager = AccountManager.get(context);
		String valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED);
		boolean mobileOptimizations = (valueStr == null || Boolean.parseBoolean(valueStr));
		SessionObject sessionObject = jaxmpp.getSessionObject();
		sessionObject.setUserProperty(MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED, mobileOptimizations);
	}

	public MobileModeFeature(XMPPService service) {
		jaxmppService = service;
	}

	public void accountConnected(JaxmppCore jaxmpp) throws JaxmppException {
		if (mobileModeEnabled) {
			setMobileMode(jaxmpp, mobileModeEnabled);
		}
	}

	protected void setMobileMode(final boolean enable) {
		if (setMobileModeTask != null) {
			setMobileModeTask.cancel();
			setMobileModeTask = null;
		}

		Log.v(TAG, "setting mobile mode to = " + enable);

		mobileModeEnabled = enable;

		if (enable) {
			setMobileModeTask = new TimerTask() {

				@Override
				public void run() {
					setMobileModeTask = null;
					try {
						for (JaxmppCore jaxmpp : jaxmppService.getMultiJaxmpp().get()) {
							setMobileMode(jaxmpp, enable);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't set mobile mode!", e);
					}
				}
			};
			jaxmppService.timer.schedule(setMobileModeTask, 1000 * 60);
		} else {
			(new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					try {
						for (JaxmppCore jaxmpp : jaxmppService.getMultiJaxmpp().get()) {
							setMobileMode(jaxmpp, enable);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't set mobile mode!", e);
					}
					return null;
				}
			}).execute();
		}
	}

	protected void setMobileMode(JaxmppCore jaxmpp, boolean enable) throws JaxmppException {
		if (jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected) {
			final Element sf = StreamFeaturesModule.getStreamFeatures(jaxmpp.getSessionObject());
			if (sf == null) {
				return;
			}

			String xmlns = null;
			Element m = sf.getChildrenNS("mobile", Features.MOBILE_V3);
			if (m != null) {
				xmlns = Features.MOBILE_V3;
			} else {
				m = sf.getChildrenNS("mobile", Features.MOBILE_V2);
				if (m != null) {
					xmlns = Features.MOBILE_V2;
				} else {
					m = sf.getChildrenNS("mobile", Features.MOBILE_V1);
					if (m != null) {
						xmlns = Features.MOBILE_V1;
					}
				}
			}
			if (xmlns == null ||
					(enable && !((Boolean) jaxmpp.getSessionObject().getProperty(MOBILE_OPTIMIZATIONS_ENABLED)))) {
				return;
			}

			IQ iq = IQ.create();
			iq.setType(StanzaType.set);
			Element mobile = ElementFactory.create("mobile");
			mobile.setXMLNS(xmlns);
			mobile.setAttribute("enable", String.valueOf(enable));
			if (Features.MOBILE_V1.equals(xmlns)) {
				Integer timeout = jaxmpp.getSessionObject().getProperty(MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT);
				if (timeout != null) {
					timeout = timeout * 60 * 1000;
					mobile.setAttribute("timeout", String.valueOf(timeout));
				}
			}
			iq.addChild(mobile);
			jaxmpp.send(iq);
		}
	}

}
