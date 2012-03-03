package org.tigase.mobile.filetransfer;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.observer.EventType;

public class FileTransferProgressEvent extends FileTransferEvent {

	private final FileTransfer fileTransfer;

	public FileTransferProgressEvent(EventType type, SessionObject sessionObject, FileTransfer fileTransfer) {
		super(type, sessionObject);
		this.fileTransfer = fileTransfer;
	}

	public FileTransfer getFileTransfer() {
		return fileTransfer;
	}
}