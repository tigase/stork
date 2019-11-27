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
import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.TextSingleField;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.modules.push.PushNotificationModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.concurrent.Executor;

public class PushController {

	public static final String PUSH_NOTIFICATION_CHANGED = "org.tigase.messenger.phone.pro.PUSH_NOTIFICATION_CHANGED";

	public static final JID PUSH_SERVICE_JID = JID.jidInstance("push.tigase.im");

	public static String TAG = "PushController";
	private final AccountManager mAccountManager;
	private final MultiJaxmpp multiJaxmpp;
	private final Executor taskExecutor;

	public static boolean isAvailable() {
		return true;
	}

	public PushController(AccountManager mAccountManager, MultiJaxmpp multiJaxmpp, Executor taskExecutor) {
		this.mAccountManager = mAccountManager;
		this.multiJaxmpp = multiJaxmpp;
		this.taskExecutor = taskExecutor;
	}

	public String getToken() {
		return FirebaseInstanceId.getInstance().getToken();
	}

	public void registerInPushService(final Account account) throws JaxmppException {
		final AdHocCommansModule ah = getJaxmpp(account.name).getModule(AdHocCommansModule.class);

		final String deviceToken = getToken();

		final JabberDataElement fields = new JabberDataElement(XDataType.submit);
		fields.addListSingleField("provider", "fcm-xmpp-api");
		fields.addTextSingleField("device-token", deviceToken);

		ah.execute(PUSH_SERVICE_JID, "register-device", null, fields,
				   new AdHocCommansModule.AdHocCommansAsyncCallback() {
					   @Override
					   public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition)
							   throws JaxmppException {
						   Log.e(TAG, "Error on registration in Push Service: " + errorCondition);
					   }

					   @Override
					   public void onTimeout() throws JaxmppException {
						   Log.e(TAG, "Registration in Push Service timeouted");
					   }

					   @Override
					   protected void onResponseReceived(String sessionid, String node, State status,
														 JabberDataElement data) throws JaxmppException {

						   TextSingleField nodeField = data.getField("node");
						   String pushServiceNode = nodeField.getFieldValue();
						   mAccountManager.setUserData(account, AccountsConstants.PUSH_SERVICE_NODE_KEY,
													   pushServiceNode);

						   enablePushService(account);
					   }
				   });
	}

	public void checkPushNotificationStatus(SessionObject sessionObject) {
		if (isAvailable()) {
			taskExecutor.execute(new CheckPushNotificationStatus(sessionObject));
		}
	}

	public void unregisterInPushService(final Account account) throws JaxmppException {
		disablePushService(account);

		final AdHocCommansModule ah = getJaxmpp(account.name).getModule(AdHocCommansModule.class);

		final String deviceToken = getToken();

		final JabberDataElement fields = new JabberDataElement(XDataType.submit);
		fields.addListSingleField("provider", "fcm-xmpp-api");
		fields.addTextSingleField("device-token", deviceToken);

		ah.execute(PUSH_SERVICE_JID, "unregister-device", null, fields,
				   new AdHocCommansModule.AdHocCommansAsyncCallback() {
					   @Override
					   public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition)
							   throws JaxmppException {
						   Log.e(TAG, "Error during unregistration from Push Service: " + errorCondition);
					   }

					   @Override
					   public void onTimeout() throws JaxmppException {
						   Log.e(TAG, "Error during unregistration from Push Service: timeout");
					   }

					   @Override
					   protected void onResponseReceived(String sessionid, String node, State status,
														 JabberDataElement data) throws JaxmppException {
						   Log.i(TAG, "Device deregistered from Push Service");
					   }
				   });

		mAccountManager.setUserData(account, AccountsConstants.PUSH_SERVICE_NODE_KEY, null);
	}

	private JaxmppCore getJaxmpp(String account) {
		return this.multiJaxmpp.get(BareJID.bareJIDInstance(account));
	}

	private void enablePushService(Account account) throws JaxmppException {
		final PushNotificationModule pm = getJaxmpp(account.name).getModule(PushNotificationModule.class);
		if (!pm.isSupportedByServer()) {
			Log.i(TAG, "Notification Push is not supported by server.");
			return;
		}
		String pushServiceNode = mAccountManager.getUserData(account, AccountsConstants.PUSH_SERVICE_NODE_KEY);

		pm.enable(PUSH_SERVICE_JID, pushServiceNode, new AsyncCallback() {
			@Override
			public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition) throws JaxmppException {
				Log.e(TAG, "Cannot enable Push Service: " + errorCondition);
			}

			@Override
			public void onSuccess(Stanza stanza) throws JaxmppException {
				Log.i(TAG, "Push Service is enabled");
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.e(TAG, "Cannot enable Push Service: timeout");
			}
		});
	}

	private void disablePushService(Account account) throws JaxmppException {
		final PushNotificationModule pm = getJaxmpp(account.name).getModule(PushNotificationModule.class);
		if (!pm.isSupportedByServer()) {
			Log.i(TAG, "Notification Push is not supported by server.");
			return;
		}
		String pushServiceNode = mAccountManager.getUserData(account, AccountsConstants.PUSH_SERVICE_NODE_KEY);

		pm.disable(PUSH_SERVICE_JID, pushServiceNode, new AsyncCallback() {
			@Override
			public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition) throws JaxmppException {
				Log.e(TAG, "Cannot disable Push Service: " + errorCondition);
			}

			@Override
			public void onSuccess(Stanza stanza) throws JaxmppException {
				Log.i(TAG, "Push Service is disabled");
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.e(TAG, "Cannot disable Push Service: timeout");
			}
		});
	}

	private class CheckPushNotificationStatus
			implements Runnable {

		private final SessionObject sessionObject;

		public CheckPushNotificationStatus(SessionObject sessionObject) {
			this.sessionObject = sessionObject;
		}

		@Override
		public void run() {
			try {
				Account account = AccountHelper.getAccount(mAccountManager, sessionObject.getUserBareJid().toString());
				String tmp = mAccountManager.getUserData(account, AccountsConstants.PUSH_NOTIFICATION);
				final String pushNodeKey = mAccountManager.getUserData(account,
																	   AccountsConstants.PUSH_SERVICE_NODE_KEY);
				final boolean pushEnabled = tmp != null && Boolean.parseBoolean(tmp);

				if (pushEnabled && pushNodeKey == null) {
					registerInPushService(account);
				} else if (!pushEnabled && pushNodeKey != null) {
					unregisterInPushService(account);
				}
			} catch (Exception e) {
				Log.e(TAG, "Cannot register/unregister in Push Service", e);
			}
		}

	}
}
