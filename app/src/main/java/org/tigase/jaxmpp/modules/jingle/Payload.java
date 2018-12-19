package org.tigase.jaxmpp.modules.jingle;

import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;

public class Payload
		extends ElementWrapper {

	public Payload(Element element) {
		super(element);
	}

	public String getId() throws XMLException {
		return getAttribute("id");
	}

	public String getPayloadName() throws XMLException {
		return getAttribute("name");
	}

	public String getClockrate() throws XMLException {
		return getAttribute("clockrate");
	}

	public int getChannels() throws XMLException {
		String tmp = getAttribute("channels");
		return tmp == null ? 1 : Integer.parseInt(tmp);
	}
}
