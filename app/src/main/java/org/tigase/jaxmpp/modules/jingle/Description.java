package org.tigase.jaxmpp.modules.jingle;

import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Description
		extends ElementWrapper {

	public Description(Element element) {
		super(element);
	}

	public String getMedia() throws XMLException {
		return getAttribute("media");
	}

	public List<Payload> getPayloads() throws XMLException {
		ArrayList<Payload> result = new ArrayList<>();
		for (Element e : getChildren("payload-type")) {
			result.add(new Payload(e));
		}
		return result;
	}

	public List<Element> getEncryption() throws XMLException {
		return getChildren("crypto");
	}
}
