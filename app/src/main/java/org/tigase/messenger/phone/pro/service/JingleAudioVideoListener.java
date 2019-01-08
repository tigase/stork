/*
 * JingleAudioVideoListener.java
 *
 * tigase-messenger2
 * Copyright (C) 2004-2018 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.service;

import android.content.Intent;
import org.tigase.jaxmpp.modules.jingle.JingleContent;
import org.tigase.jaxmpp.modules.jingle.JingleElement;
import org.tigase.jaxmpp.modules.jingle.JingleModule;
import org.tigase.jaxmpp.modules.jingle.JingleSession;
import org.tigase.messenger.phone.pro.video.VideoChatActivity;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;

import java.util.List;

public class JingleAudioVideoListener
		implements JingleModule.JingleSessionAcceptHandler,
				   JingleModule.JingleSessionInfoHandler,
				   JingleModule.JingleSessionInitiationHandler,
				   JingleModule.JingleSessionTerminateHandler,
				   JingleModule.JingleTransportInfoHandler {

	private final XMPPService service;

	public JingleAudioVideoListener(XMPPService xmppService) {
		this.service = xmppService;
	}

	@Override
	public boolean onJingleSessionInfo(SessionObject sessionObject, JID sender, String sid, List<Element> content) {
		return true;
	}

	@Override
	public boolean onJingleSessionInitiation(SessionObject sessionObject, JingleSession jingleSession) {
		Intent intent = new Intent(service, VideoChatActivity.class);
		intent.putExtra(VideoChatActivity.ACCOUNT_KEY, sessionObject.getUserBareJid().toString());
		intent.putExtra(VideoChatActivity.JID_KEY, jingleSession.getJid().toString());
		intent.putExtra(VideoChatActivity.SID_KEY, jingleSession.getSid());
		intent.putExtra(VideoChatActivity.INITIATOR_KEY, false);

		service.startActivity(intent);
		return true;
	}

	@Override
	public boolean onJingleSessionTerminate(SessionObject sessionObject, JID sender, String sid) {
		return true;
	}

	@Override
	public boolean onJingleTransportInfo(SessionObject sessionObject, JID sender, String sid, JingleContent content)
			throws JaxmppException {
		return true;
	}

	@Override
	public boolean onJingleSessionAccept(SessionObject sessionObject, JingleSession jingleSession,
										 JingleElement jingle) {
		return true;
	}
}
