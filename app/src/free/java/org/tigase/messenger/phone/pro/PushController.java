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
package org.tigase.messenger.phone.pro;

import android.accounts.Account;
import android.accounts.AccountManager;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

import java.util.concurrent.Executor;

public class PushController {

	public static final String PUSH_NOTIFICATION_CHANGED = "org.tigase.messenger.phone.pro.PUSH_NOTIFICATION_CHANGED";

	public static final JID PUSH_SERVICE_JID = JID.jidInstance("push.tigase.im");

	public static String TAG = "PushController";
	private final AccountManager mAccountManager;
	private final MultiJaxmpp multiJaxmpp;
	private final Executor taskExecutor;

	public static boolean isAvailable() {
		return false;
	}

	public PushController(AccountManager mAccountManager, MultiJaxmpp multiJaxmpp, Executor taskExecutor) {
		this.mAccountManager = mAccountManager;
		this.multiJaxmpp = multiJaxmpp;
		this.taskExecutor = taskExecutor;
	}

	public String getToken() {
		return null;
	}

	public void registerInPushService(final Account account) throws JaxmppException {
	}

	public void checkPushNotificationStatus(SessionObject sessionObject) {
	}

	public void unregisterInPushService(final Account account) throws JaxmppException {
	}

}
