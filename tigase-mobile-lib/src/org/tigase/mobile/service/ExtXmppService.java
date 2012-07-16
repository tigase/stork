package org.tigase.mobile.service;

import java.util.Collection;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.utils.AvatarHelper;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import tigase.jaxmpp.android.service.XmppService;
import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class ExtXmppService extends XmppService {

	private static final String TAG = "ExtXmppService";
	
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
	protected void unregisterModule(XmppModule module) {
		for (JaxmppCore jaxmpp : getMulti().get()) {
			jaxmpp.getModulesManager().unregister(module);
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
					Element element = ParcelableElement.create(stanza);
					String from = element.getAttribute("from");
					String to = element.getAttribute("to");
					element.setAttribute("from", to);
					element.setAttribute("to", from);
					element.setAttribute("type", "error");
					XMPPException.ErrorCondition.remote_server_timeout.getElementName();
					Element error = new DefaultElement("error");
					error.setAttribute("type", XMPPException.ErrorCondition.recipient_unavailable.getType());
					element.addChild(error);
					Element condition = new DefaultElement(XMPPException.ErrorCondition.recipient_unavailable.getElementName());
					error.addChild(condition);
					module.process(element);
					return;
				}
				if (module != null) {
					jaxmpp.send(stanza, new AsyncCallback() {
						
						@Override
						public void onTimeout() throws JaxmppException {
							Element element = ParcelableElement.create(stanza);
							String from = element.getAttribute("from");
							String to = element.getAttribute("to");
							element.setAttribute("from", to);
							element.setAttribute("to", from);
							element.setAttribute("type", "error");
							XMPPException.ErrorCondition.remote_server_timeout.getElementName();
							Element error = new DefaultElement("error");
							error.setAttribute("type", XMPPException.ErrorCondition.remote_server_timeout.getType());
							element.addChild(error);
							Element condition = new DefaultElement(XMPPException.ErrorCondition.remote_server_timeout.getElementName());
							error.addChild(condition);
							module.process(element);
						}
						
						@Override
						public void onSuccess(Stanza responseStanza) throws JaxmppException {
							module.process(responseStanza);
						}
						
						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
							module.process(responseStanza);
						}
					});
				}
				else {
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
	protected Collection<JaxmppCore> getAccounts() {
		return getMulti().get();
	}
	
	@Override
	protected Bitmap getAvatar(BareJID jid) {
		return AvatarHelper.getAvatar(jid);
	}
}
