package org.tigase.mobile.filetransfer;

import java.util.List;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public abstract class StreamhostsCallback implements AsyncCallback {

	private FileTransferModule ftManager;

	public StreamhostsCallback(FileTransferModule ftManager) {
		this.ftManager = ftManager;
	}

	public abstract void onStreamhosts(List<Streamhost> hosts);

	@Override
	public void onSuccess(Stanza stanza) throws JaxmppException {
		List<Streamhost> hosts = ftManager.processStreamhosts(stanza);
		if (hosts != null) {
			onStreamhosts(hosts);
		}
	}

}