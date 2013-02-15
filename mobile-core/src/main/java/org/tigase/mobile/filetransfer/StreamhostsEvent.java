package org.tigase.mobile.filetransfer;

import java.util.ArrayList;
import java.util.List;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.observer.EventType;

public class StreamhostsEvent extends FileTransferEvent {

	private JID from;
	private List<Streamhost> hosts;
	private String id;
	private String sid;

	public StreamhostsEvent(EventType eventType, SessionObject sessionObject) {
		super(eventType, sessionObject);

		hosts = new ArrayList<Streamhost>();
	}

	public JID getFrom() {
		return from;
	}

	public List<Streamhost> getHosts() {
		return hosts;
	}

	public String getId() {
		return id;
	}

	public String getSid() {
		return sid;
	}

	public void setFrom(JID from) {
		this.from = from;
	}

	public void setHosts(List<Streamhost> hosts) {
		this.hosts = hosts;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}
}