package org.tigase.mobile.filetransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.EventType;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public class FileTransferModule implements XmppModule {

	public static abstract class ActivateCallback implements AsyncCallback {

	}

	public static class FileTransferProgressEvent extends FileTransferEvent {

		private final FileTransfer fileTransfer;

		public FileTransferProgressEvent(EventType type, SessionObject sessionObject, FileTransfer fileTransfer) {
			super(type, sessionObject);
			this.fileTransfer = fileTransfer;
		}

		public FileTransfer getFileTransfer() {
			return fileTransfer;
		}
	}

	public static class Host {

		private String address;
		private String jid;
		private Integer port;

		public Host(String jid, String address, Integer port) {
			this.jid = jid;
			this.address = address;
			this.port = port;
		}

		public String getAddress() {
			return address;
		}

		public String getJid() {
			return jid;
		}

		public Integer getPort() {
			return port;
		}

	}

	public static class HostsEvent extends FileTransferEvent {

		private JID from;
		private List<Host> hosts;
		private String id;
		private String sid;

		public HostsEvent(EventType eventType, SessionObject sessionObject) {
			super(eventType, sessionObject);

			hosts = new ArrayList<Host>();
		}

		public JID getFrom() {
			return from;
		}

		public List<Host> getHosts() {
			return hosts;
		}

		public String getId() {
			return id;
		}

		public String getSid() {
			return sid;
		}

		public void setFrom(JID from) {
			this.from = from;
		}

		public void setHosts(List<Host> hosts) {
			this.hosts = hosts;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setSid(String sid) {
			this.sid = sid;
		}
	}

	public static abstract class StreamhostsCallback implements AsyncCallback {

		private FileTransferModule ftManager;

		public StreamhostsCallback(FileTransferModule ftManager) {
			this.ftManager = ftManager;
		}

		public abstract void onStreamhosts(List<Host> hosts);

		@Override
		public void onSuccess(Stanza stanza) throws JaxmppException {
			List<Host> hosts = ftManager.processStreamhosts(stanza);
			if (hosts != null) {
				onStreamhosts(hosts);
			}
		}

	}

	public static abstract class StreamInitiationOfferAsyncCallback implements AsyncCallback {

		private String sid = null;

		public abstract void onAccept(String sid);

		public abstract void onError();

		@Override
		public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
			if (error == ErrorCondition.forbidden) {
				onReject();
			} else {
				onError();
			}
		}

		public abstract void onReject();

		@Override
		public void onSuccess(Stanza stanza) throws JaxmppException {
			boolean ok = false;
			String sid = null;

			Element si = stanza.getChildrenNS("si", XMLNS_SI);
			if (si != null) {
				sid = si.getAttribute("id");
				Element feature = si.getChildrenNS("feature", "http://jabber.org/protocol/feature-neg");
				if (feature != null) {
					Element x = feature.getChildrenNS("x", "jabber:x:data");
					if (x != null) {
						Element field = x.getFirstChild();
						if (field != null) {
							Element value = field.getFirstChild();
							if (value != null) {
								ok = XMLNS_BS.equals(value.getValue());
							}
						}
					}
				}
			}

			if (sid == null) {
				sid = this.sid;
			}

			if (ok) {
				onAccept(sid);
			} else {
				onError();
			}
		}

		@Override
		public void onTimeout() {
			onError();
		}

		public void setSid(String sid) {
			this.sid = sid;
		}
	}

	public static final String XMLNS_BS = "http://jabber.org/protocol/bytestreams";

	private static final Criteria CRIT = ElementCriteria.name("iq").add(ElementCriteria.name("query", XMLNS_BS));

	private static final String[] FEATURES = new String[] { XMLNS_BS };

	public static final EventType FileTransferProgressEventType = new EventType();

	public static final EventType HostsEvent = new EventType();

	private static final Logger log = Logger.getLogger(FileTransferModule.class.getCanonicalName());

	public static final String XMLNS_SI = "http://jabber.org/protocol/si";

	public static final String XMLNS_SI_FILE = "http://jabber.org/protocol/si/profile/file-transfer";

	private final Observable observable;

	private final SessionObject session;

	private final PacketWriter writer;

	public FileTransferModule(Observable parentObservable, SessionObject sessionObject, PacketWriter packetWriter) {
		observable = new Observable(parentObservable);
		session = sessionObject;
		writer = packetWriter;
	}

	public void addListener(EventType eventType, Listener listener) {
		observable.addListener(eventType, listener);
	}

	public void fileTransferProgressUpdated(FileTransfer ft) {
		FileTransferEvent event = new FileTransferProgressEvent(FileTransferProgressEventType, session, ft);
		try {
			observable.fireEvent(event);
		} catch (JaxmppException e) {
			// TODO - check - should not happen
			e.printStackTrace();
		}
	}

	@Override
	public Criteria getCriteria() {
		return CRIT;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public void process(Element element) throws XMPPException, XMLException, JaxmppException {
		final IQ iq = element instanceof Stanza ? (IQ) element : (IQ) Stanza.create(element);
		process(iq);
	}

	public void process(IQ iq) throws XMLException, JaxmppException {
		List hosts = processStreamhosts(iq);
		if (hosts != null) {
			HostsEvent event = new HostsEvent(HostsEvent, this.session);
			event.setId(iq.getId());
			event.setSid(iq.getChildrenNS("query", XMLNS_BS).getAttribute("sid"));
			event.setFrom(iq.getFrom());
			event.setHosts(hosts);

			observable.fireEvent(event);
			return;
		}
	}

	private List<Host> processStreamhosts(Stanza iq) throws XMLException {
		Element query = iq.getChildrenNS("query", XMLNS_BS);
		List<Element> el_hosts = query.getChildren("streamhost");

		if (el_hosts == null)
			return null;

		List<Host> hosts = new ArrayList<Host>();

		if (el_hosts != null) {
			HostsEvent event = new HostsEvent(HostsEvent, this.session);
			for (Element el_host : el_hosts) {
				String jid = el_host.getAttribute("jid");
				hosts.add(new Host(jid, el_host.getAttribute("host"), Integer.parseInt(el_host.getAttribute("port"))));
			}
		}

		return hosts;
	}

	public void removeListener(Listener listener) {
		observable.removeListener(listener);
	}

	public void requestActivate(JID host, String sid, String jid, ActivateCallback callback) throws XMLException,
			JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(host);
		iq.setType(StanzaType.set);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		query.setAttribute("sid", sid);
		iq.addChild(query);

		Element activate = new DefaultElement("activate", jid, null);
		query.addChild(activate);

		writer.write(iq, callback);
	}

	public void requestStreamhosts(JID host, StreamhostsCallback callback) throws XMLException, JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(host);
		iq.setType(StanzaType.get);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		iq.addChild(query);

		writer.write(iq, callback);
	}

	public void sendStreamhosts(JID recipient, String sid, List<Host> hosts, AsyncCallback callback) throws XMLException,
			JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(recipient);
		iq.setType(StanzaType.set);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		iq.addChild(query);
		query.setAttribute("sid", sid);

		for (Host host : hosts) {
			Element streamhost = new DefaultElement("streamhost");
			streamhost.setAttribute("jid", host.getJid());
			streamhost.setAttribute("host", host.getAddress());
			streamhost.setAttribute("port", String.valueOf(host.getPort()));
			query.addChild(streamhost);
		}

		writer.write(iq, (long) (3 * 60 * 1000), callback);
	}

	public void sendStreamhostUsed(JID to, String id, String sid, Host streamhost) throws XMLException, JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(to);
		iq.setId(id);
		iq.setType(StanzaType.result);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		iq.addChild(query);

		Element streamhostUsed = new DefaultElement("streamhost-used");
		streamhostUsed.setAttribute("jid", streamhost.getJid());
		query.addChild(streamhostUsed);

		// session.registerResponseHandler(iq, callback);
		writer.write(iq);
	}

	public void sendStreamInitiationOffer(JID recipient, String filename, String mimetype, long filesize,
			StreamInitiationOfferAsyncCallback callback) throws XMLException, JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(recipient);
		iq.setType(StanzaType.set);

		Element si = new DefaultElement("si", null, XMLNS_SI);
		si.setAttribute("profile", XMLNS_SI_FILE);
		String sid = UUID.randomUUID().toString();
		si.setAttribute("id", sid);
		callback.setSid(sid);

		if (mimetype != null) {
			si.setAttribute("mime-type", mimetype);
		}

		iq.addChild(si);

		Element file = new DefaultElement("file", null, XMLNS_SI_FILE);
		file.setAttribute("name", filename);
		file.setAttribute("size", String.valueOf(filesize));
		si.addChild(file);

		Element feature = new DefaultElement("feature", null, "http://jabber.org/protocol/feature-neg");
		si.addChild(feature);
		Element x = new DefaultElement("x", null, "jabber:x:data");
		x.setAttribute("type", "form");
		feature.addChild(x);
		Element field = new DefaultElement("field");
		field.setAttribute("var", "stream-method");
		field.setAttribute("type", "list-single");
		x.addChild(field);
		Element option = new DefaultElement("option");
		field.addChild(option);
		Element value = new DefaultElement("value", XMLNS_BS, null);
		option.addChild(value);

		writer.write(iq, (long) (10 * 60 * 1000), callback);
	}
}
