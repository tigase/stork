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

import android.util.Log;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;

public class SubscribeOwnOMEMODevices
		implements Runnable {

	private final static String TAG = "SubscrOwnOMEMODevices";

	private final Jaxmpp jaxmpp;

	public SubscribeOwnOMEMODevices(Jaxmpp jaxmpp) {
		this.jaxmpp = jaxmpp;
	}

	@Override
	public void run() {
		OmemoModule module = jaxmpp.getModule(OmemoModule.class);
		if (jaxmpp.isConnected()) {
			try {
				module.subscribeForDeviceList(jaxmpp.getSessionObject().getUserBareJid());
			} catch (JaxmppException e) {
				Log.e(TAG, "Cannot subscribe own OMEMO devices", e);
			}
		}
	}
}
