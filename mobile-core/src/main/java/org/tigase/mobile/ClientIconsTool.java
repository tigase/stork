/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile;

import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;

public class ClientIconsTool {

	private static boolean check(final String node, final String ver, final Identity id, String expectedNode,
			String expectedVer, String expectedName) {

		boolean result = true;

		if (result && expectedNode != null) {
			result &= node != null && node.startsWith(expectedNode);
		}

		if (result && expectedName != null) {
			final String name = id.getName().toLowerCase();
			result &= name != null && name.startsWith(expectedName);
		}

		if (result && expectedVer != null) {
			result &= ver != null && ver.startsWith(expectedVer);
		}

		return result;
	}

	public static Integer getResourceImage(final Presence presence, CapabilitiesModule capabilitiesModule, String nodeName)
			throws XMLException {

		if (presence == null)
			return null;
		Element c = presence.getChildrenNS("c", "http://jabber.org/protocol/caps");
		if (c == null)
			return null;
		final String node = c.getAttribute("node") == null ? "" : c.getAttribute("node");
		final String ver = c.getAttribute("ver") == null ? "" : c.getAttribute("ver");
		final Identity id = capabilitiesModule.getCache().getIdentity(node + "#" + ver);

		if (id == null)
			return null;

		String cc = id.getCategory() + "/" + id.getType();

		Integer icon;

		if (check(node, ver, id, null, null, "tigase messenger")) {
			icon = R.drawable.client_messenger;
		} else if (check(node, ver, id, null, null, "adium")) {
			icon = R.drawable.client_adium;
		} else if (check(node, ver, id, null, null, "google talk user account")) {
			icon = R.drawable.client_android_gtalk;
		} else if (check(node, ver, id, "http://mail.google.com", null, null)) {
			icon = R.drawable.client_gtalk;
		} else if (check(node, ver, id, "http://talkgadget.google.com", null, null)) {
			icon = R.drawable.client_gtalk;
		} else if (check(node, ver, id, "http://talk.google.com", null, null)) {
			icon = R.drawable.client_gtalk;
		} else if (check(node, ver, id, "http://google.com", "1.0.0.104", null)) {
			icon = R.drawable.client_gtalk;
		} else if (check(node, ver, id, "http://google.com", null, null)) {
			icon = R.drawable.client_gtalk;
		} else if (check(node, ver, id, "http://www.apple.com/ichat", null, null)) {
			icon = R.drawable.client_ichat;
		} else if (check(node, ver, id, null, null, "kopete")) {
			icon = R.drawable.client_kopete;
		} else if (check(node, ver, id, null, null, "gaim")) {
			icon = R.drawable.client_gaim;
		} else if (check(node, ver, id, null, null, "gajim")) {
			icon = R.drawable.client_gajim;
		} else if (check(node, ver, id, null, null, "psi+")) {
			icon = R.drawable.client_psiplus;
		} else if (check(node, ver, id, null, null, "psi-dev")) {
			icon = R.drawable.client_psiplus;
		} else if (check(node, ver, id, "http://psi-im.org", null, null)) {
			icon = R.drawable.client_psi;
		} else if (check(node, ver, id, null, null, "xabber")) {
			icon = R.drawable.client_xabber;
		} else if (check(node, ver, id, null, null, "telepathy")) {
			icon = R.drawable.client_telepathy;
		} else if (check(node, ver, id, null, null, "swift")) {
			icon = R.drawable.client_swift;
		} else
			icon = null;

		if (icon == null && cc.equals("client/phone")) {
			return R.drawable.client_mobile;
		}

		return icon;

	}

	private ClientIconsTool() {
	}
}
