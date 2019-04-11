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
