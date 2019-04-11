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
