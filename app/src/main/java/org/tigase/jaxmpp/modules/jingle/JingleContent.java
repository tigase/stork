package org.tigase.jaxmpp.modules.jingle;

import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.List;

public class JingleContent
		extends ElementWrapper {

	public JingleContent(Element content) {
		super(content);
	}

	public List<Transport> getTransports() throws XMLException {
		return Util.convertAll(getChildren("transport"), Transport::new);
	}

	public Description getDescription() throws XMLException {
		List<Element> ds = getChildren("description");
		if (ds == null || ds.isEmpty()) {
			return null;
		}
		return new Description(ds.get(0));
	}

	public String getCreator() throws XMLException {
		return getAttribute("creator");
	}

	public String getContentName() throws XMLException {
		return getAttribute("name");
	}

	public String getSenders() throws XMLException {
		return getAttribute("senders");
	}

}
