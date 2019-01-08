package org.tigase.jaxmpp.modules.jingle;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class JingleSession {

	private final Queue<JingleContent> candidates = new LinkedList<>();
	private final String id;
	private final JID initiator;
	private final JID jid;
	private final JingleElement jingleElement;
	private final String sid;

	public static JingleSession create(String id, JID jid, JID initiator, Element jingleElement) throws XMLException {
		return new JingleSession(id, jid, initiator, jingleElement.getAttribute("sid"),
								 new JingleElement(jingleElement));
	}

	JingleSession(String id, JID jid, JID initiator, String sid, JingleElement jingleElement) {
		this.jingleElement = jingleElement;
		this.sid = sid;
		this.id = id;
		this.jid = jid;
		this.initiator = initiator;
	}

	public JID getInitiator() {
		return initiator;
	}

	public String getId() {
		return id;
	}

	public JingleElement getJingleElement() {
		return jingleElement;
	}

	public synchronized Queue<JingleContent> getCandidates() {
		return candidates;
	}

//	public String toSDP() throws JaxmppException {
//		final ArrayList<String> sdp = new ArrayList<>();
//
//		sdp.add("v=0");
//		sdp.add("o=- " + sid + " " + id + " IN IP4 0.0.0.0");
//		sdp.add("s=-");
//		sdp.add("t=0 0");
//
//		if (group != null) {
//			StringBuilder t = new StringBuilder("a=group:").append(group.getAttribute("semantics"));
//
//			List<Element> gs = group.getChildren("content");
//			if (gs != null) {
//				for (Element g : gs) {
//					t.append(" ").append(g.getAttribute("name"));
//				}
//			}
//
//			sdp.add(t.toString());
//		}
//
//		for (JingleContent content : getContents()) {
//			sdp.add(JingleContent.toSDP(content));
//		}
//
//		return JingleContent.join("\r\n", sdp) + "\r\n";
//	}

	public JID getJid() {
		return jid;
	}

	public String getSid() {
		return sid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		JingleSession that = (JingleSession) o;

		if (!jid.equals(that.jid)) {
			return false;
		}
		return sid.equals(that.sid);
	}

	@Override
	public int hashCode() {
		int result = jid.hashCode();
		result = 31 * result + sid.hashCode();
		return result;
	}

}
