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
