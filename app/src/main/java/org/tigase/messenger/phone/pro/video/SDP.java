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

package org.tigase.messenger.phone.pro.video;

import org.tigase.jaxmpp.modules.jingle.*;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SDP {

	public static final String LINE = "\r\n";
	public static final String TIGASE_SDP_XMLNS = "tigase:sdp:0";

	private static String[] findContentsNames(String[] sdp) {
		String l = findLine("a=group:BUNDLE ", sdp);
		if (l == null) {
			return new String[]{};
		}
		return l.substring(15).split(" ");
	}

	static boolean findLine(String prefix, String[] sdp, Consumer consumer) {
		String t = findLine(prefix, sdp);
		if (t != null) {
			try {
				consumer.apply(t);
				return true;
			} catch (JaxmppException e) {
				throw new RuntimeException(e);
			}
		} else {
			return false;
		}
	}

	private static String findLine(String prefix, String[] sdp) {
		for (String s : sdp) {
			if (s.startsWith(prefix)) {
				return s;
			}
		}
		return null;
	}

	private static String[] findLines(String prefix, String[] sdp) {
		ArrayList<String> result = new ArrayList<>();
		for (String s : sdp) {
			if (s.startsWith(prefix)) {
				result.add(s);
			}
		}
		return result.toArray(new String[]{});
	}

	private static String[] getMediaLines(final String groupName, String[] sdp) {
		int p0 = -1;
		int p1 = -1;
		int p2 = -1;

		for (int i = 0; i < sdp.length; i++) {
			String line = sdp[i];
			if (p0 == -1 && line.startsWith("m=")) {
				p0 = i;
				continue;
			}
			if (p0 != -1 && line.startsWith("a=mid:")) {
				if (line.startsWith("a=mid:" + groupName)) {
					p1 = i;
				} else {
					p0 = -1;
					p1 = -1;
					p2 = -1;
				}
			}

			if (p1 != -1 && line.startsWith("m=")) {
				p2 = i;
				break;
			}
		}

		if (p2 == -1) {
			p2 = sdp.length - 1;
		}

		return Arrays.copyOfRange(sdp, p0, p2);
	}

	public static String getOriginalSDP(Element jingleElement) throws XMLException {
		Element oryginalSdp = jingleElement.getChildrenNS("sdp", SDP.TIGASE_SDP_XMLNS);
		if (oryginalSdp != null) {
			return new String(Base64.decode(oryginalSdp.getValue()));
		} else {
			return null;
		}
	}

	static String join(final CharSequence delimiter, final Iterable<? extends CharSequence> elements) {
		StringBuilder sb = new StringBuilder();
		for (CharSequence element : elements) {
			if (sb.length() != 0) {
				sb.append(delimiter);
			}
			sb.append(element);
		}
		return sb.toString();
	}

	private static void processParams(String[] items, int offset, BiConsumer<String, String> consumer) {
		for (int i = offset; i < items.length; i += 2) {
			consumer.accept(items[i], items[i + 1]);
		}
	}

	public Transport fromSDPTransport(final String[] mediaLines) throws XMLException {
		final Transport transport = new Transport(ElementFactory.create("transport"));
		transport.setXMLNS("urn:xmpp:jingle:transports:ice-udp:1");
		findLine("a=ice-ufrag:", mediaLines, line -> transport.setAttribute("ufrag", line.substring(12)));
		findLine("a=ice-pwd:", mediaLines, line -> transport.setAttribute("pwd", line.substring(10)));

		findLine("a=fingerprint:", mediaLines, fp -> {
			Element fingerprint = ElementFactory.create("fingerprint", null, "urn:xmpp:jingle:apps:dtls:0");
			transport.addChild(fingerprint);
			String[] tmp = fp.substring(14).split(" ");
			fingerprint.setAttribute("hash", tmp[0]);
			fingerprint.setValue(tmp[1]);

			fingerprint.setAttribute("setup", findLine("a=setup:", mediaLines).substring(8));
		});

		findLine("candidate:", mediaLines, line -> {
			String[] cc = line.substring(10).split(" ");
			final Candidate candidate = new Candidate(ElementFactory.create("candidate"));
			candidate.setAttribute("id", UIDGenerator.next());
			transport.addChild(candidate);

			candidate.setAttribute("foundation", cc[0]);
			candidate.setAttribute("component", cc[1]);
			candidate.setAttribute("protocol", cc[2]);
			candidate.setAttribute("priority", cc[3]);
			candidate.setAttribute("ip", cc[4]);
			candidate.setAttribute("port", cc[5]);

			processParams(cc, 6, (key, value) -> {
				try {
					switch (key) {
						case "typ":
							candidate.setAttribute("type", value);
							break;
						case "ufrag":
							transport.setAttribute("ufrag", value);
							break;
						case "generation":
							candidate.setAttribute("generation", value);
							break;
						case "tcptype":
							candidate.setAttribute("tcptype", value);
							break;
						case "raddr":
							candidate.setAttribute("rel-addr", value);
							break;
						case "rport":
							candidate.setAttribute("rel-port", value);
							break;
						case "network-id":
						case "network-cost":
							break;
					}
				} catch (XMLException e) {
					e.printStackTrace();
				}
			});

			candidate.setAttribute("type", cc[7]);
		});

		return transport;
	}

	public Element fromSDP(final String sdpData) throws XMLException {
		final String[] sdp = sdpData.split(LINE);

		final Element jingle = ElementFactory.create("jingle");
		jingle.setXMLNS("urn:xmpp:jingle:1");

//		jingle.addChild(ElementFactory.create("sdp", Base64.encode(sdpData.getBytes()), SDP.TIGASE_SDP_XMLNS));

		String[] groups = findContentsNames(sdp);
		for (String groupName : groups) {
			final String[] mediaLines = getMediaLines(groupName, sdp);
			String[] m = mediaLines[0].substring(2).split(" ");
			final String mediaType = m[0];

			JingleContent content = new JingleContent(ElementFactory.create("content"));
			jingle.addChild(content);
			content.setAttribute("name", groupName);
			content.setAttribute("senders", "both");
			content.setAttribute("creator", "responder");

			Description description = new Description(ElementFactory.create("description"));
			description.setAttribute("xmlns", "urn:xmpp:jingle:apps:rtp:1");
			description.setAttribute("media", mediaType);
			content.addChild(description);

			findLine("a=rtcp-mux", mediaLines, line -> description.addChild(ElementFactory.create("rtcp-mux")));

			String[] payloadIds = Arrays.copyOfRange(m, 3, m.length);
			for (String payloadId : payloadIds) {
				Payload payload = createPayload(payloadId, mediaLines);
				description.addChild(payload);
			}

			// transport
			content.addChild(fromSDPTransport(mediaLines));
		}

		String grLine = findLine("a=group:", sdp);
		if (grLine != null) {
			String[] g = grLine.substring(8).split(" ");
			Element group = ElementFactory.create("group", null, "urn:xmpp:jingle:apps:grouping:0");
			group.setAttribute("semantics", g[0]);
			for (int i = 1; i < g.length; i++) {
				Element c = ElementFactory.create("content");
				c.setAttribute("name", g[i]);
				group.addChild(c);
			}
			jingle.addChild(group);
		}

		return jingle;
	}

	public String toSDP(final JingleContent content) throws XMLException {
		final ArrayList<String> sdp = new ArrayList<>();

		final Description desc = content.getDescription();
		final Transport transport = content.getTransports().size() > 0 ? content.getTransports().get(0) : null;

		List<String> mLine = new ArrayList<>();
		mLine.add(desc.getMedia());
		mLine.add("1");
		if (!desc.getEncryption().isEmpty() || (transport != null && transport.getFingerprint() != null)) {
			mLine.add("RTP/SAVPF");
		} else {
			mLine.add("RTP/AVPF");
		}

		for (Payload p : desc.getPayloads()) {
			mLine.add(p.getId());
		}

		sdp.add("m=" + join(" ", mLine));

		sdp.add("c=IN IP4 0.0.0.0");
		sdp.add("a=rtcp:1 IN IP4 0.0.0.0");

		if (transport != null) {
			if (transport.getUfrag() != null) {
				sdp.add("a=ice-ufrag:" + transport.getUfrag());
			}
			if (transport.getPwd() != null) {
				sdp.add("a=ice-pwd:" + transport.getPwd());
			}

			Element fng = transport.getFingerprint();
			if (fng != null) {
				sdp.add("a=fingerprint:" + fng.getAttribute("hash") + " " + fng.getValue());
				sdp.add("a=setup:" + fng.getAttribute("setup"));
			}
		}

		sdp.add("a=sendrecv");
		sdp.add("a=mid:" + content.getContentName());

		if (!desc.getChildren("rtcp-mux").isEmpty()) {
			sdp.add("a=rtcp-mux");
		}

		for (Element e : desc.getEncryption()) {
			String line = "a=crypto:" + e.getAttribute("tag") + " " + e.getAttribute("crypto-suite") + " " +
					e.getAttribute("key-params");
			if (e.getAttribute("session-params") != null) {
				line += " " + e.getAttribute("session-params");
			}
			sdp.add(line);
		}

		for (Payload e : desc.getPayloads()) {
			String line = "a=rtpmap:" + e.getId() + " " + e.getPayloadName() + "/" + e.getClockrate();
			if (e.getChannels() > 1) {
				line += "/" + e.getChannels();
			}
			sdp.add(line);

			List<Element> rtcpfbs = e.getChildren("rtcp-fb");
			if (rtcpfbs != null) {
				for (Element rtcpfb : rtcpfbs) {
					StringBuilder p = new StringBuilder("a=rtcp-fb:").append(e.getId()).append(" ");
					p.append(rtcpfb.getAttribute("type"));
					if (rtcpfb.getAttribute("subtype") != null) {
						p.append(" ").append(rtcpfb.getAttribute("subtype"));
					}

					sdp.add(p.toString());
				}
			}

			List<Element> params = e.getChildren("parameter");
			if (params != null && params.size() > 0) {
				StringBuilder p = new StringBuilder("a=fmtp:").append(e.getId()).append(" ");
				boolean first = true;
				for (Element param : params) {
					if (!first) {
						p.append(";");
					}
					p.append(param.getAttribute("name")).append("=").append(param.getAttribute("value"));
					first = false;
				}
				sdp.add(p.toString());
			}
		}

		if (transport != null) {
			for (Candidate c : transport.getCandidates()) {
				sdp.add(toSDP(transport, c));
			}
		}

		return join(LINE, sdp);
	}

	public String toSDP(final String id, final String sid, final JingleElement jingleElement) throws XMLException {
		final ArrayList<String> sdp = new ArrayList<>();

		String oryginalSdp = getOriginalSDP(jingleElement);
		if (oryginalSdp != null) {
			return oryginalSdp;
		}

		sdp.add("v=0");
		sdp.add("o=- " + sid + " " + id + " IN IP4 0.0.0.0");
		sdp.add("s=-");
		sdp.add("t=0 0");

		final Element group = jingleElement.getGroup();
		if (group != null) {
			StringBuilder t = new StringBuilder("a=group:").append(group.getAttribute("semantics"));
			List<Element> gs = group.getChildren("content");
			if (gs != null) {
				for (Element g : gs) {
					t.append(" ").append(g.getAttribute("name"));
				}
			}
			sdp.add(t.toString());
		}

		for (JingleContent content : jingleElement.getContents()) {
			sdp.add(toSDP(content));
		}

		return join(LINE, sdp) + LINE;
	}

	public String toSDP(final Transport transport) throws XMLException {
		final ArrayList<String> sdp = new ArrayList<>();
//		if (transport.getUfrag() != null) {
//			sdp.add("a=ice-ufrag:" + transport.getUfrag());
//		}
//		if (transport.getPwd() != null) {
//			sdp.add("a=ice-pwd:" + transport.getPwd());
//		}
//
//		Element fng = transport.getFingerprint();
//		if (fng != null) {
//			sdp.add("a=fingerprint:" + fng.getAttribute("hash") + " " + fng.getValue());
//			sdp.add("a=setup:" + fng.getAttribute("setup"));
//		}

		for (Candidate c : transport.getCandidates()) {
			sdp.add(toSDP(transport, c));
		}

		return join(LINE, sdp) + LINE;
	}

	private Payload createPayload(String payloadId, final String[] sdp) throws XMLException {
		Payload payload = new Payload(ElementFactory.create("payload-type"));
		payload.setAttribute("id", payloadId);

		String[] x = findLine("a=rtpmap:" + payloadId, sdp).split(" ")[1].split("/");
		payload.setAttribute("name", x[0]);
		payload.setAttribute("clockrate", x[1]);
		payload.setAttribute("channels", x.length > 2 ? x[2] : "1");

		// parameters
		findLine("a=fmtp:" + payloadId, sdp, line -> {
			String[] prms = line.split(" ")[1].split(";");
			for (String prm : prms) {
				String[] z = prm.split("=");
				Element p = ElementFactory.create("parameter", null, "urn:xmpp:jingle:apps:rtp:1");
				payload.addChild(p);
				p.setAttribute("name", z[0]);
				p.setAttribute("value", z[1]);
			}
		});

		// <rtcp-fb type="transport-cc" xmlns="urn:xmpp:jingle:apps:rtp:rtcp-fb:0"/>
		findLine("a=rtcp-fb:" + payloadId, sdp, line -> {
			Element p = ElementFactory.create("rtcp-fb", null, "urn:xmpp:jingle:apps:rtp:rtcp-fb:0");
			payload.addChild(p);
			String[] tmp = line.split(" ");
			if (tmp.length > 1) {
				p.setAttribute("type", tmp[1]);
			}
			if (tmp.length > 2) {
				p.setAttribute("subtype", tmp[2]);
			}
		});

		return payload;
	}

	private String toSDP(Transport transport, final Candidate candidate) throws XMLException {
		final ArrayList<String> sdp = new ArrayList<>();
		sdp.add("a=candidate:" + candidate.getAttribute("foundation"));
		sdp.add(candidate.getAttribute("component"));
		sdp.add(candidate.getAttribute("protocol"));
		sdp.add(candidate.getAttribute("priority"));
		sdp.add(candidate.getAttribute("ip"));
		sdp.add(candidate.getAttribute("port"));

//		if (transport.getUfrag() != null) {
//			sdp.add("ufrag");
//			sdp.add(transport.getUfrag());
//		}

		addAttributes("type", candidate, "typ", sdp);
		addAttributes("rel-addr", candidate, "raddr", sdp);
		addAttributes("rel-port", candidate, "rport", sdp);

		addAttributes("generation", candidate, "generation", sdp);
		addAttributes("tcptype", candidate, "tcptype", sdp);
		addAttributes("network-id", candidate, "network", sdp);
		addAttributes("network-cost", candidate, "network-cost", sdp);

		return join(" ", sdp);
	}

	private void addAttributes(final String candidateName, Candidate candidate, String sdpName, ArrayList<String> sdp)
			throws XMLException {
		if (candidate.getAttributes().containsKey(candidateName)) {
			sdp.add(sdpName);
			sdp.add(candidate.getAttribute(candidateName));
		}
	}

	interface BiConsumer<T, U> {

		/**
		 * Performs this operation on the given arguments.
		 *
		 * @param t the first input argument
		 * @param u the second input argument
		 */
		void accept(T t, U u);
	}

	public static interface Consumer {

		void apply(String line) throws JaxmppException;
	}
}
