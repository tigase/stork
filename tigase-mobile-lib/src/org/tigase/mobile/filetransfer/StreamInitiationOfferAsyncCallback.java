package org.tigase.mobile.filetransfer;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public abstract class StreamInitiationOfferAsyncCallback implements AsyncCallback {

	private String sid = null;

	public abstract void onAccept(String sid);

	public abstract void onError();

	@Override
	public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
		if (error == ErrorCondition.forbidden) {
			onReject();
		} else {
			onError();
		}
	}

	public abstract void onReject();

	@Override
	public void onSuccess(Stanza stanza) throws JaxmppException {
		boolean ok = false;
		String sid = null;

		Element si = stanza.getChildrenNS("si", FileTransferModule.XMLNS_SI);
		if (si != null) {
			sid = si.getAttribute("id");
			Element feature = si.getChildrenNS("feature", "http://jabber.org/protocol/feature-neg");
			if (feature != null) {
				Element x = feature.getChildrenNS("x", "jabber:x:data");
				if (x != null) {
					Element field = x.getFirstChild();
					if (field != null) {
						Element value = field.getFirstChild();
						if (value != null) {
							ok = FileTransferModule.XMLNS_BS.equals(value.getValue());
						}
					}
				}
			}
		}

		if (sid == null) {
			sid = this.sid;
		}

		if (ok) {
			onAccept(sid);
		} else {
			onError();
		}
	}

	@Override
	public void onTimeout() {
		onError();
	}

	public void setSid(String sid) {
		this.sid = sid;
	}
}