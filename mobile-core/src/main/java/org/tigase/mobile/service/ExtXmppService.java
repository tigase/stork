/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.service;

import java.util.Collection;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.android.service.XmppService;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import android.graphics.Bitmap;
import android.util.Log;

public class ExtXmppService extends XmppService {

	private static final String TAG = "ExtXmppService";

	@Override
	protected Collection<JaxmppCore> getAccounts() {
		return getMulti().get();
	}

	@Override
	protected Bitmap getAvatar(BareJID jid) {
		return AvatarHelper.getAvatar(jid);
	}

	private final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
	}

	@Override
	protected void init() {
	}

	@Override
	protected void registerModule(XmppModule module) {
		for (JaxmppCore jaxmpp : getMulti().get()) {
			jaxmpp.getModulesManager().register(module);
		}
	}

	@Override
	protected void sendStanza(BareJID account, Element element, boolean requestCallback, final XmppModule module) {
		final JaxmppCore jaxmpp = getMulti().get(account);
		if (jaxmpp != null) {
			try {
				final Stanza stanza = Stanza.create(element);
				new Thread() {
					@Override
					public void run() {
						try {
							if (!jaxmpp.isConnected()) {
								Element element = DefaultElement.create(stanza);
								String from = element.getAttribute("from");
								String to = element.getAttribute("to");
								element.setAttribute("from", to);
								element.setAttribute("to", from);
								element.setAttribute("type", "error");
								XMPPException.ErrorCondition.remote_server_timeout.getElementName();
								Element error = new DefaultElement("error");
								error.setAttribute("type", XMPPException.ErrorCondition.recipient_unavailable.getType());
								element.addChild(error);
								Element condition = new DefaultElement(
										XMPPException.ErrorCondition.recipient_unavailable.getElementName());
								error.addChild(condition);
								module.process(element);
								return;
							}
							if (module != null) {
								jaxmpp.send(stanza, new AsyncCallback() {

									@Override
									public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
										module.process(responseStanza);
									}

									@Override
									public void onSuccess(Stanza responseStanza) throws JaxmppException {
										module.process(responseStanza);
									}

									@Override
									public void onTimeout() throws JaxmppException {
										Element element = DefaultElement.create(stanza);
										String from = element.getAttribute("from");
										String to = element.getAttribute("to");
										element.setAttribute("from", to);
										element.setAttribute("to", from);
										element.setAttribute("type", "error");
										XMPPException.ErrorCondition.remote_server_timeout.getElementName();
										Element error = new DefaultElement("error");
										error.setAttribute("type", XMPPException.ErrorCondition.remote_server_timeout.getType());
										element.addChild(error);
										Element condition = new DefaultElement(
												XMPPException.ErrorCondition.remote_server_timeout.getElementName());
										error.addChild(condition);
										module.process(element);
									}
								});
							} else {
								jaxmpp.send(stanza);
							}
						} catch (JaxmppException e) {
							Log.e(TAG, "exception sending stanza", e);
						}
					}
				}.start();
			} catch (JaxmppException e) {
				Log.e(TAG, "exception creating stanza from element", e);
			}
		}
	}

	@Override
	protected void unregisterModule(XmppModule module) {
		for (JaxmppCore jaxmpp : getMulti().get()) {
			jaxmpp.getModulesManager().unregister(module);
		}
	}
}
