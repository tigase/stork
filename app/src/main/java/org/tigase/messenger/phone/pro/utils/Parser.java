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

package org.tigase.messenger.phone.pro.utils;

import android.util.Log;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.j2se.connectors.socket.StreamListener;
import tigase.jaxmpp.j2se.connectors.socket.XMPPDomBuilderHandler;
import tigase.jaxmpp.j2se.xml.J2seElement;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import java.util.Map;

public class Parser {

	private final static String TAG = "ElementParser";

	public static void parseElement(String dataStr, final ElementCallback callback) {
		XMPPDomBuilderHandler handler = new XMPPDomBuilderHandler(new StreamListener() {
			@Override
			public void nextElement(tigase.xml.Element element) {
				try {
					Log.v(TAG, "parsed, now decoding address..");
					callback.parsed(new J2seElement(element));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			@Override
			public void xmppStreamClosed() {
			}

			@Override
			public void xmppStreamOpened(Map<String, String> attribs) {
			}

		});
		SimpleParser parser = SingletonFactory.getParserInstance();
		char[] data = dataStr.toCharArray();
		Log.v(TAG, "parsing..");
		parser.parse(handler, data, 0, data.length);
	}

	public static Element parseElement(String dataStr) {
		final Element[] result = new Element[]{null};
		parseElement(dataStr, new ElementCallback() {
			@Override
			public void parsed(Element element) throws JaxmppException {
				result[0] = element;
			}
		});
		try {
			return ElementFactory.create(result[0]);
		} catch (XMLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Parser() {
	}

	public interface ElementCallback {

		void parsed(Element element) throws JaxmppException;
	}

}
