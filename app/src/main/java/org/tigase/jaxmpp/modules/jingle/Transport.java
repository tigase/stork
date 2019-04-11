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

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrzej
 */
public class Transport
		extends ElementWrapper {

	public static final String DSTADDR_ATTR = "dstaddr";
	public static final String MODE_ATTR = "mode";
	public static final String SID_ATTR = "sid";

	public enum Mode {
		tcp,
		udp
	}

	public Transport(Element elem) throws XMLException {
		super(elem);
		if (!"transport".equals(elem.getName())) {
			throw new XMLException("Invalid jingle tranport element");
		}
	}

	public Transport(String xmlns, String sid, Mode mode) throws JaxmppException {
		super(ElementFactory.create("transport", null, xmlns));
		setAttribute(SID_ATTR, sid);

		if (mode != null) {
			setAttribute(MODE_ATTR, mode.name());
		}
	}

	public Transport(String xmlns, String sid, Mode mode, String dstaddr) throws JaxmppException {
		this(xmlns, sid, mode);

		if (dstaddr != null) {
			setAttribute(DSTADDR_ATTR, dstaddr);
		}
	}

	public void addCandidate(Candidate candidate) throws XMLException {
		addChild(candidate);
	}

	public List<Candidate> getCandidates() throws XMLException {
		List<Candidate> candidates = new ArrayList<Candidate>();
		for (Element elem : getChildren()) {
			if ("candidate".equals(elem.getName())) {
				candidates.add(new Candidate(elem));
			}
		}
		return candidates;
	}

	public String getDstAddr() throws XMLException {
		return getAttribute(DSTADDR_ATTR);
	}

	public Mode getMode() throws XMLException {
		String modeStr = getAttribute(MODE_ATTR);
		return modeStr != null ? Mode.valueOf(modeStr) : null;
	}

	public String getSid() throws XMLException {
		return getAttribute(SID_ATTR);
	}

	public String getUfrag() throws XMLException {
		return getAttribute("ufrag");
	}

	public String getPwd() throws XMLException {
		return getAttribute("pwd");
	}

	public Fingerprint getFingerprint() throws XMLException {
		List<Element> fps = getChildren("fingerprint");
		if (fps == null || fps.isEmpty()) {
			return null;
		}
		return new Fingerprint(fps.get(0));
	}

	public static class Fingerprint
			extends ElementWrapper {

		public Fingerprint(Element element) {
			super(element);
		}
	}

}
