package org.tigase.mobile.filetransfer;

import java.util.List;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.observer.EventType;

public class FileTransferRequestEvent extends FileTransferEvent {

	private final String filename;
	private final Long filesize;
	private final String id;
	private final String mimetype;
	private final JID sender;
	private final String sid;
	private final List<String> streamMethods;

	public FileTransferRequestEvent(EventType type, SessionObject sessionObject, JID sender, String id, String sid,
			String filename, Long filesize, List<String> streamMethods, String mimetype) {
		super(type, sessionObject);

		this.sender = sender;
		this.id = id;
		this.sid = sid;
		this.filename = filename;
		this.filesize = filesize;
		this.streamMethods = streamMethods;
		this.mimetype = mimetype;
	}

	public String getFilename() {
		return filename;
	}

	public Long getFilesize() {
		return filesize;
	}

	public String getId() {
		return id;
	}

	public String getMimetype() {
		return mimetype;
	}

	public JID getSender() {
		return sender;
	}

	public String getSid() {
		return sid;
	}

	public List<String> getStreamMethods() {
		return streamMethods;
	}

}
