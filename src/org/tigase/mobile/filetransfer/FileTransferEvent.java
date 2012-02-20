package org.tigase.mobile.filetransfer;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.EventType;

public class FileTransferEvent extends BaseEvent {

	public FileTransferEvent(EventType type, SessionObject sessionObject) {
		super(type, sessionObject);
	}

}