/*
 * Tigase Halcyon XMPP Library
 * Copyright (C) 2018 Tigase, Inc. (office@tigase.com)
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

import android.util.Log;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.JaxmppEventWithCallback;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JingleModule
		implements XmppModule {

	public static final String JINGLE_RTP1_XMLNS = "urn:xmpp:jingle:apps:rtp:1";
	public static final String JINGLE_XMLNS = "urn:xmpp:jingle:1";
	public static final Criteria CRIT = ElementCriteria.name("iq").add(ElementCriteria.name("jingle", JINGLE_XMLNS));
	public static final String[] FEATURES = {JINGLE_XMLNS, JINGLE_RTP1_XMLNS, "urn:xmpp:jingle:transports:ice-udp:1",
											 "urn:xmpp:jingle:apps:rtp:audio", "urn:xmpp:jingle:apps:rtp:video",
											 "urn:xmpp:jingle:apps:dtls:0"

	};
	public static final String JINGLE_SESSIONS = "JingleModule#JINGLE_SESSIONS";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private Context context;

	public static JingleSession getSession(SessionObject sessionObject, String sid, JID jid) {
		Map<String, JingleSession> map = sessionObject.getProperty(JINGLE_SESSIONS);
		if (map == null) {
			return null;
		}
		final String key = sid + ":" + jid;
		return map.get(key);
	}

	private static void removeSession(SessionObject sessionObject, JingleSession session) {
		Map<String, JingleSession> map = sessionObject.getProperty(JINGLE_SESSIONS);
		if (map == null) {
			return;
		}
		final String key = session.getSid() + ":" + session.getJid();
		map.remove(key);
	}

	private static void storeSession(SessionObject sessionObject, JingleSession session) {
		Map<String, JingleSession> map = sessionObject.getProperty(JINGLE_SESSIONS);
		if (map == null) {
			map = new HashMap<>();
			sessionObject.setProperty(SessionObject.Scope.user, JINGLE_SESSIONS, map);
		}
		final String key = session.getSid() + ":" + session.getJid();
		map.put(key, session);
	}

	public JingleModule(Context context) {
		this.context = context;
	}

	public void acceptSession(JID jid, String sid, String name, Element description, List<Transport> transports)
			throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(jid);
		iq.setType(StanzaType.set);

		Element jingle = ElementFactory.create("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-accept");
		jingle.setAttribute("sid", sid);

		jingle.setAttribute("initiator", jid.toString());

		JID initiator = context.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		jingle.setAttribute("responder", initiator.toString());

		iq.addChild(jingle);

		Element content = ElementFactory.create("content");
		content.setXMLNS(JINGLE_XMLNS);
		content.setAttribute("creator", "initiator");
		content.setAttribute("name", name);

		jingle.addChild(content);

		content.addChild(description);
		if (transports != null) {
			for (Element transport : transports) {
				content.addChild(transport);
			}
		}

		context.getWriter().write(iq);
	}

	@Override
	public Criteria getCriteria() {
		return CRIT;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	public void initiateSession(JID jid, String sid, String name, Element description, List<Transport> transports)
			throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(jid);
		iq.setType(StanzaType.set);

		Element jingle = ElementFactory.create("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-initiate");
		jingle.setAttribute("sid", sid);

		JID initiator = context.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		jingle.setAttribute("initiator", initiator.toString());

		iq.addChild(jingle);

		Element content = ElementFactory.create("content");
		content.setXMLNS(JINGLE_XMLNS);
		content.setAttribute("creator", "initiator");
		content.setAttribute("name", name);

		jingle.addChild(content);

		content.addChild(description);
		for (Element transport : transports) {
			content.addChild(transport);
		}

		context.getWriter().write(iq);
	}

	@Override
	public void process(Element element) throws JaxmppException {
		if ("iq".equals(element.getName())) {
			IQ iq = (IQ) Stanza.create(element);
			processIq(iq);
		}
	}

	public void terminateSession(JingleSession session) throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(session.getJid());
		iq.setType(StanzaType.set);

		Element jingle = ElementFactory.create("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-terminate");
		jingle.setAttribute("sid", session.getSid());

		jingle.setAttribute("initiator", session.getInitiator().toString());

		iq.addChild(jingle);

		Element reason = ElementFactory.create("result");
		jingle.addChild(reason);

		Element success = ElementFactory.create("success");
		reason.addChild(success);

		context.getWriter().write(iq);
	}

	public void transportInfo(JID recipient, JID initiator, String sid, Element content) throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(recipient);
		iq.setType(StanzaType.set);

		Element jingle = ElementFactory.create("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "transport-info");
		jingle.setAttribute("sid", sid);
		jingle.setAttribute("initiator", initiator.toString());

		iq.addChild(jingle);

		jingle.addChild(content);

		context.getWriter().write(iq);
	}

	public void acceptSession(JingleSession session, Element jingle, AsyncCallback asyncCallback)
			throws JaxmppException {
		jingle.setAttribute("action", "session-accept");
		jingle.setAttribute("sid", session.getSid());
		jingle.setAttribute("responder", ResourceBinderModule.getBindedJID(context.getSessionObject()).toString());

		IQ iq = IQ.createIQ();
		iq.setType(StanzaType.set);
		iq.setId(UIDGenerator.next());
		iq.setTo(session.getJid());

		iq.addChild(jingle);

		context.getWriter().write(iq, asyncCallback);
	}

	public void sendTransportInfo(JingleSession session, Element jingle) throws JaxmppException {
		jingle.setAttribute("action", "transport-info");
		jingle.setAttribute("sid", session.getSid());
		jingle.setAttribute("responder", ResourceBinderModule.getBindedJID(context.getSessionObject()).toString());

		IQ iq = IQ.createIQ();
		iq.setType(StanzaType.set);
		iq.setId(UIDGenerator.next());
		iq.setTo(session.getJid());

		iq.addChild(jingle);

		context.getWriter().write(iq);
	}

	public JingleSession initiateSession(final JID jid, final Element jingle, final AsyncCallback asyncCallback)
			throws JaxmppException {
		final String id = UIDGenerator.next();
		final JID myJid = ResourceBinderModule.getBindedJID(context.getSessionObject());
		jingle.setAttribute("sid", id);
		JingleSession session = JingleSession.create(String.valueOf(System.currentTimeMillis()), jid, myJid, jingle);
		storeSession(context.getSessionObject(), session);

		jingle.setAttribute("action", "session-initiate");
		jingle.setAttribute("initiator", myJid.toString());

		IQ iq = IQ.createIQ();
		iq.setType(StanzaType.set);
		iq.setId(UIDGenerator.next());
		iq.setTo(session.getJid());

		iq.addChild(jingle);

		context.getWriter().write(iq, asyncCallback);

		return session;
	}

	protected synchronized void processIq(final IQ iq) throws JaxmppException {
		Element jingle = iq.getChildrenNS("jingle", JINGLE_XMLNS);

		List<Element> contents = jingle.getChildren("content");
		// if (contents == null || contents.isEmpty()) {
		// // no point in parsing this any more
		// return;
		// }

		JID from = iq.getFrom();
		String sid = jingle.getAttribute("sid");

		String action = jingle.getAttribute("action");

		if ("session-terminate".equals(action)) {
			JingleSession jingleSession = getSession(context.getSessionObject(), sid, from);

			if (jingleSession == null) {
				throw new XMPPException(XMPPException.ErrorCondition.unexpected_request, "Unknown SID");
			}

			removeSession(context.getSessionObject(), jingleSession);

			JingleSessionTerminateHandler.JingleSessionTerminateEvent event = new JingleSessionTerminateHandler.JingleSessionTerminateEvent(
					context.getSessionObject(), from, sid, (ev) -> processHandled(iq, ev.isHandled()));
			context.getEventBus().fire(event);
		} else if ("session-info".equals(action)) {
			JingleSessionInfoHandler.JingleSessionInfoEvent event = new JingleSessionInfoHandler.JingleSessionInfoEvent(
					context.getSessionObject(), from, sid, jingle.getChildren(),
					(ev) -> processHandled(iq, ev.isHandled()));
			context.getEventBus().fire(event);
		} else if ("transport-info".equals(action)) {
			JingleSession jingleSession = getSession(context.getSessionObject(), sid, from);
			if (jingleSession == null) {
				throw new XMPPException(XMPPException.ErrorCondition.unexpected_request, "Unknown SID");
			}

			List<JingleContent> cnt = parseContents(contents);
			jingleSession.getCandidates().addAll(cnt);

			JingleTransportInfoHandler.JingleTransportInfoEvent event = new JingleTransportInfoHandler.JingleTransportInfoEvent(
					context.getSessionObject(), from, sid, cnt.get(0), (ev) -> {
				log.warning("Jingle Session " + sid + " NOT PROCESSED");
				processHandled(iq, ev.isHandled());
			});
			context.getEventBus().fire(event);
		} else {
			if ("session-initiate".equals(action)) {
				JingleSession jingleSession = JingleSession.create(String.valueOf(System.currentTimeMillis()), from,
																   from, jingle);
				storeSession(context.getSessionObject(), jingleSession);

				JingleSessionInitiationHandler.JingleSessionInitiationEvent event = new JingleSessionInitiationHandler.JingleSessionInitiationEvent(
						context.getSessionObject(), jingleSession, (ev) -> processHandled(iq, ev.isHandled()));
				context.getEventBus().fire(event);
			} else if ("session-accept".equals(action)) {
				JingleSession jingleSession = getSession(context.getSessionObject(), sid, from);

				if (jingleSession == null) {
					throw new XMPPException(XMPPException.ErrorCondition.unexpected_request, "Unknown SID");
				}

				JingleSessionAcceptHandler.JingleSessionAcceptEvent event = new JingleSessionAcceptHandler.JingleSessionAcceptEvent(
						context.getSessionObject(), jingleSession, new JingleElement(jingle),
						(ev) -> processHandled(iq, ev.isHandled()));
				context.getEventBus().fire(event);
			}
		}
	}

	private List<JingleContent> parseContents(List<Element> contents) throws JaxmppException {
		ArrayList<JingleContent> result = new ArrayList<>();

		for (Element content : contents) {
			result.add(new JingleContent(content));
		}

		return result;
	}

	private void processHandled(final IQ iq, final boolean handled) {
		try {
			if (handled) {
				// sending result - here should be always ok
				IQ response = IQ.create();
				response.setTo(iq.getFrom());
				response.setId(iq.getId());
				response.setType(StanzaType.result);
				context.getWriter().write(response);
			} else {
				Element errorResult = Processor.createError(iq, new XMPPException(
						XMPPException.ErrorCondition.feature_not_implemented));
				context.getWriter().write(errorResult);
			}
		} catch (JaxmppException e) {
			Log.w("JingleModule", e);
		}
	}

	public interface JingleSessionAcceptHandler
			extends EventHandler {

		boolean onJingleSessionAccept(SessionObject sessionObject, JingleSession jingleSession, JingleElement jingle);

		class JingleSessionAcceptEvent
				extends JaxmppEventWithCallback<JingleSessionAcceptHandler> {

			private final JingleElement jingle;
			private final JingleSession jingleSession;
			private boolean handled = false;

			public JingleSessionAcceptEvent(SessionObject sessionObject, JingleSession jingleSession,
											JingleElement jingle, RunAfter<JingleSessionAcceptEvent> runAfter) {
				super(sessionObject, runAfter);
				this.jingleSession = jingleSession;
				this.jingle = jingle;
			}

			public JingleElement getJingle() {
				return jingle;
			}

			public JingleSession getJingleSession() {
				return jingleSession;
			}

			public boolean isHandled() {
				return handled;
			}

			public void setHandled(boolean handled) {
				this.handled = handled;
			}

			@Override
			public void dispatch(JingleSessionAcceptHandler handler) {
				handled = handler.onJingleSessionAccept(sessionObject, jingleSession, jingle);
			}

		}
	}

	public interface JingleSessionInfoHandler
			extends EventHandler {

		boolean onJingleSessionInfo(SessionObject sessionObject, JID sender, String sid, List<Element> content);

		class JingleSessionInfoEvent
				extends JaxmppEventWithCallback<JingleSessionInfoHandler> {

			private List<Element> content;
			private boolean handled = false;
			private JID sender;
			private String sid;

			public JingleSessionInfoEvent(SessionObject sessionObject, JID sender, String sid, List<Element> content,
										  RunAfter<JingleSessionInfoEvent> runAfter) {
				super(sessionObject, runAfter);
				this.sender = sender;
				this.sid = sid;
				this.content = content;
			}

			public boolean isHandled() {
				return handled;
			}

			public void setHandled(boolean handled) {
				this.handled = handled;
			}

			@Override
			public void dispatch(JingleSessionInfoHandler handler) {
				handled |= handler.onJingleSessionInfo(sessionObject, sender, sid, content);
			}

			public List<Element> getContent() {
				return content;
			}

			public void setContent(List<Element> content) {
				this.content = content;
			}

			public JID getSender() {
				return sender;
			}

			public void setSender(JID sender) {
				this.sender = sender;
			}

			public String getSid() {
				return sid;
			}

			public void setSid(String sid) {
				this.sid = sid;
			}

		}
	}

	public interface JingleSessionInitiationHandler
			extends EventHandler {

		boolean onJingleSessionInitiation(SessionObject sessionObject, JingleSession jingleSession);

		class JingleSessionInitiationEvent
				extends JaxmppEventWithCallback<JingleSessionInitiationHandler> {

			private final JingleSession jingleSession;
			private boolean handled = false;

			public JingleSessionInitiationEvent(SessionObject sessionObject, JingleSession jingleSession,
												RunAfter<JingleSessionInitiationEvent> runAfter) {
				super(sessionObject, runAfter);
				this.jingleSession = jingleSession;
			}

			public JingleSession getJingleSession() {
				return jingleSession;
			}

			public boolean isHandled() {
				return handled;
			}

			public void setHandled(boolean handled) {
				this.handled = handled;
			}

			@Override
			public void dispatch(JingleSessionInitiationHandler handler) {
				handled |= handler.onJingleSessionInitiation(sessionObject, jingleSession);
			}

		}
	}

	public interface JingleSessionTerminateHandler
			extends EventHandler {

		boolean onJingleSessionTerminate(SessionObject sessionObject, JID sender, String sid);

		class JingleSessionTerminateEvent
				extends JaxmppEventWithCallback<JingleSessionTerminateHandler> {

			private boolean handled = false;
			private JID sender;
			private String sid;

			public JingleSessionTerminateEvent(SessionObject sessionObject, JID sender, String sid,
											   RunAfter<JingleSessionTerminateEvent> runAfter) {
				super(sessionObject, runAfter);
				this.sender = sender;
				this.sid = sid;
			}

			public boolean isHandled() {
				return handled;
			}

			public void setHandled(boolean handled) {
				this.handled = handled;
			}

			@Override
			public void dispatch(JingleSessionTerminateHandler handler) {
				handled |= handler.onJingleSessionTerminate(sessionObject, sender, sid);
			}

			public JID getSender() {
				return sender;
			}

			public void setSender(JID sender) {
				this.sender = sender;
			}

			public String getSid() {
				return sid;
			}

			public void setSid(String sid) {
				this.sid = sid;
			}

		}
	}

	public interface JingleTransportInfoHandler
			extends EventHandler {

		boolean onJingleTransportInfo(SessionObject sessionObject, JID sender, String sid, JingleContent content)
				throws JaxmppException;

		class JingleTransportInfoEvent
				extends JaxmppEventWithCallback<JingleTransportInfoHandler> {

			private JingleContent content;
			private boolean handled = false;
			private JID sender;
			private String sid;

			public JingleTransportInfoEvent(SessionObject sessionObject, JID sender, String sid, JingleContent content,
											RunAfter<JingleTransportInfoEvent> runAfter) {
				super(sessionObject, runAfter);
				this.sender = sender;
				this.sid = sid;
				this.content = content;
			}

			public boolean isHandled() {
				return handled;
			}

			public void setHandled(boolean handled) {
				this.handled = handled;
			}

			@Override
			public void dispatch(JingleTransportInfoHandler handler) throws JaxmppException {
				handled |= handler.onJingleTransportInfo(sessionObject, sender, sid, content);
			}

			public Element getContent() {
				return content;
			}

			public void setContent(JingleContent content) {
				this.content = content;
			}

			public JID getSender() {
				return sender;
			}

			public void setSender(JID sender) {
				this.sender = sender;
			}

			public String getSid() {
				return sid;
			}

			public void setSid(String sid) {
				this.sid = sid;
			}

		}
	}

}