/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.jaxmpp.modules.jingle;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.connection.ConnectionEndpoint;

/**
 * @author andrzej
 */
public class Candidate
		extends ElementWrapper
		implements ConnectionEndpoint {

	// private String cid;
	// private String host;
	// private Integer port;
	// private JID jid;
	// private Integer priority;
	// private Type type;
	public static final String CID_ATTR = "cid";
	public static final String HOST_ATTR = "host";
	public static final String JID_ATTR = "jid";
	public static final String PORT_ATTR = "port";
	public static final String PRIORITY_ATTR = "priority";
	public static final String TYPE_ATTR = "type";

	public enum Type {

		assisted,
		direct,
		proxy,
		tunnel
	}

	public Candidate(Element elem) throws XMLException {
		super(elem);
		if (!"candidate".equals(elem.getName())) {
			throw new XMLException("Invalid jingle transport candidate element");
		}
	}

	public Candidate(String cid, String host, Integer port, JID jid, Integer priority, Type type)
			throws JaxmppException {
		super(ElementFactory.create("candidate"));
		// this.cid = cid;
		// this.host = host;
		// this.port = port;
		// this.priority = priority;
		// this.jid = jid;
		// this.type = type;

		setAttribute(CID_ATTR, cid);
		setAttribute(HOST_ATTR, host);
		setAttribute(PORT_ATTR, String.valueOf(port));
		setAttribute(JID_ATTR, jid.toString());
		setAttribute(PRIORITY_ATTR, String.valueOf(priority));
		setAttribute(TYPE_ATTR, type.name());
	}

	public String getId() throws XMLException {
		return getAttribute("id");
	}

	public String getCid() throws XMLException {
		return getAttribute(CID_ATTR);
	}

	@Override
	public String getHost() throws XMLException {
		return getAttribute(HOST_ATTR);
	}

	@Override
	public JID getJid() throws XMLException {
		return JID.jidInstance(getAttribute(JID_ATTR));
	}

	@Override
	public Integer getPort() throws XMLException {
		return Integer.parseInt(getAttribute(PORT_ATTR));
	}

	public Integer getPriority() throws XMLException {
		return Integer.parseInt(getAttribute(PRIORITY_ATTR));
	}

	public Type getType() throws XMLException {
		return Type.valueOf(getAttribute(TYPE_ATTR));
	}

	// public static Candidate fromElement(Element elem) throws JaxmppException
	// {
	// return new Candidate(elem.getAttribute(CID_ATTR),
	// elem.getAttribute(HOST_ATTR),
	// Integer.parseInt(elem.getAttribute(PORT_ATTR)),
	// JID.jidInstance(elem.getAttribute(JID_ATTR)),
	// Integer.parseInt(elem.getAttribute(PRIORITY_ATTR)),
	// Type.valueOf(elem.getAttribute(TYPE_ATTR)));
	// }

	// public Element toElement() throws JaxmppException {
	// Element elem = ElementFactory.create("candidate");
	// elem.setAttribute(CID_ATTR, cid);
	// elem.setAttribute(HOST_ATTR, host);
	// elem.setAttribute(PORT_ATTR, String.valueOf(port));
	// elem.setAttribute(JID_ATTR, jid.toString());
	// elem.setAttribute(PRIORITY_ATTR, String.valueOf(priority));
	// elem.setAttribute(TYPE_ATTR, type.name());
	// return elem;
	// }
}
