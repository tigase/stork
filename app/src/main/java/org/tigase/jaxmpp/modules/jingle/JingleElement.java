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

import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.List;

public class JingleElement
		extends ElementWrapper {

	public JingleElement(Element elem) throws XMLException {
		super(elem);
		if (!"jingle".equals(elem.getName())) {
			throw new XMLException("Invalid jingle element");
		}
	}

	public Element getGroup() throws XMLException {
		return getChildrenNS("group", "urn:xmpp:jingle:apps:grouping:0");
	}

	public List<JingleContent> getContents() throws XMLException {
		return Util.convertAll(getChildren("content"), JingleContent::new);
	}

}
