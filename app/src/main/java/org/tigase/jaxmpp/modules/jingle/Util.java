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
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Util {

	public static <T> List<T> convertAll(Collection<Element> elements, Converter<T> converter) throws XMLException {
		if (elements == null) {
			return Collections.emptyList();
		}
		ArrayList<T> result = new ArrayList<>();
		for (Element element : elements) {
			result.add(converter.convert(element));
		}
		return result;
	}

	interface Converter<T> {

		T convert(Element element) throws XMLException;
	}

}
