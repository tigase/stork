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
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.criteria.Or;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.EventType;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.observer.ObservableFactory;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public class FileTransferModule implements XmppModule {

	public static abstract class ActivateCallback implements AsyncCallback {

	}

	public static final String XMLNS_BS = "http://jabber.org/protocol/bytestreams";

	public static final String XMLNS_SI = "http://jabber.org/protocol/si";

	public static final String XMLNS_SI_FILE = "http://jabber.org/protocol/si/profile/file-transfer";	
	
	private static final Criteria CRIT = ElementCriteria.name("iq").add(
			new Or(ElementCriteria.name("query", XMLNS_BS), ElementCriteria.name("si", new String[] { "xmlns", "profile" },
					new String[] { XMLNS_SI, XMLNS_SI_FILE })));
	private static final String[] FEATURES = new String[] { XMLNS_BS, XMLNS_SI, XMLNS_SI_FILE };
	private static final Logger log = Logger.getLogger(FileTransferModule.class.getCanonicalName());

	public static final EventType ProgressEventType = new EventType();
	public static final EventType RequestEventType = new EventType();

	public static final EventType StreamhostsEventType = new EventType();

	private final Observable observable;

	private final SessionObject session;

	private final PacketWriter writer;

	public FileTransferModule(Observable parentObservable, SessionObject sessionObject, PacketWriter packetWriter) {
		observable = ObservableFactory.instance(parentObservable);
		session = sessionObject;
		writer = packetWriter;
	}

	public void acceptStreamInitiation(JID to, String id, String streamMethod) throws JaxmppException {
		Element iq = new DefaultElement("iq");
		iq.setAttribute("type", "result");
		iq.setAttribute("to", to.toString());
		iq.setAttribute("id", id);

		Element si = new DefaultElement("si", null, XMLNS_SI);
		iq.addChild(si);

		Element feature = new DefaultElement("feature", null, "http://jabber.org/protocol/feature-neg");
		si.addChild(feature);

		Element x = new DefaultElement("x", null, "jabber:x:data");
		x.setAttribute("type", "submit");
		feature.addChild(x);

		Element field = new DefaultElement("field");
		field.setAttribute("var", "stream-method");
		x.addChild(field);

		Element value = new DefaultElement("value", streamMethod, null);
		field.addChild(value);

		writer.write(iq);
	}

	public void addListener(EventType eventType, Listener listener) {
		observable.addListener(eventType, listener);
	}

	public void fileTransferProgressUpdated(FileTransfer ft) {
		FileTransferEvent event = new FileTransferProgressEvent(ProgressEventType, session, ft);
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
		Element query = iq.getChildrenNS("query", XMLNS_BS);
		if (query != null) {
			List<Streamhost> hosts = processStreamhosts(iq);
			if (hosts != null) {
				StreamhostsEvent event = new StreamhostsEvent(StreamhostsEventType, this.session);
				event.setId(iq.getId());
				event.setSid(iq.getChildrenNS("query", XMLNS_BS).getAttribute("sid"));
				event.setFrom(iq.getFrom());
				event.setHosts(hosts);

				observable.fireEvent(event);
				return;
			}
		}
		query = iq.getChildrenNS("si", XMLNS_SI);
		if (query != null) {
			processStreamInitiationRequest(iq);
		}
	}

	List<Streamhost> processStreamhosts(Stanza iq) throws XMLException {
		Element query = iq.getChildrenNS("query", XMLNS_BS);
		List<Element> el_hosts = query.getChildren("streamhost");

		if (el_hosts == null)
			return null;

		List<Streamhost> hosts = new ArrayList<Streamhost>();

		if (el_hosts != null) {
			StreamhostsEvent event = new StreamhostsEvent(StreamhostsEventType, this.session);
			for (Element el_host : el_hosts) {
				String jid = el_host.getAttribute("jid");
				hosts.add(new Streamhost(jid, el_host.getAttribute("host"), Integer.parseInt(el_host.getAttribute("port"))));
			}
		}

		return hosts;
	}

	private void processStreamInitiationRequest(IQ iq) throws JaxmppException {
		if (iq.getType() != StanzaType.set)
			return;

		Element si = iq.getChildrenNS("si", XMLNS_SI);
		Element file = si.getChildrenNS("file", XMLNS_SI_FILE);
		if (file == null)
			return;

		Element feature = si.getChildrenNS("feature", "http://jabber.org/protocol/feature-neg");
		if (feature == null) {
			returnErrorBadRequest(iq);
			return;
		}
		Element x = feature.getChildrenNS("x", "jabber:x:data");
		if (x == null) {
			returnErrorBadRequest(iq);
			return;
		}
		Element field = x.getFirstChild();
		if (field == null) {
			returnErrorBadRequest(iq);
			return;
		}
		List<String> streamMethods = new ArrayList<String>();
		List<Element> options = field.getChildren("option");
		if (options != null) {
			for (Element option : options) {
				Element value = option.getFirstChild();
				if (value != null) {
					if (value.getValue() != null) {
						streamMethods.add(value.getValue());
					}
				}
			}
		}

		Long filesize = null;
		if (file.getAttribute("size") != null) {
			filesize = Long.parseLong(file.getAttribute("size"));
		}

		FileTransferRequestEvent event = new FileTransferRequestEvent(RequestEventType, this.session, iq.getFrom(),
				iq.getAttribute("id"), si.getAttribute("id"), file.getAttribute("name"), filesize, streamMethods,
				si.getAttribute("mimetype"));

		observable.fireEvent(event);
	}

	public void rejectStreamInitiation(JID to, String id) throws JaxmppException {
		returnError(to.toString(), id, "cancel", new String[] { "forbidden" },
				new String[] { "urn:ietf:params:xml:ns:xmpp-stanzas" });
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

	private void returnError(String to, String id, String type, String[] names, String[] xmlnss) throws JaxmppException {
		Element result = new DefaultElement("iq");
		result.setAttribute("id", id);
		result.setAttribute("to", to);
		result.setAttribute("type", "error");

		Element error = new DefaultElement("error");
		error.setAttribute("type", type);
		for (int i = 0; i < names.length; i++) {
			Element err = new DefaultElement(names[i], null, xmlnss[i]);
			error.addChild(err);
		}
		result.addChild(error);
		writer.write(result);
	}

	private void returnErrorBadRequest(IQ iq) throws JaxmppException {
		returnError(iq.getAttribute("from"), iq.getAttribute("id"), "cancel", new String[] { "bad-request" },
				new String[] { "urn:ietf:params:xml:ns:xmpp-stanzas" });
	}

	public void sendNoValidStreams(FileTransferRequestEvent be) throws JaxmppException {
		returnError(be.getSender().toString(), be.getId(), "cancel", new String[] { "bad-request", "no-valid-streams" },
				new String[] { XMLNS_BS, XMLNS_SI });
	}

	public void sendStreamhosts(JID recipient, String sid, List<Streamhost> hosts, AsyncCallback callback) throws XMLException,
			JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(recipient);
		iq.setType(StanzaType.set);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		iq.addChild(query);
		query.setAttribute("sid", sid);

		for (Streamhost host : hosts) {
			Element streamhost = new DefaultElement("streamhost");
			streamhost.setAttribute("jid", host.getJid());
			streamhost.setAttribute("host", host.getAddress());
			streamhost.setAttribute("port", String.valueOf(host.getPort()));
			query.addChild(streamhost);
		}

		writer.write(iq, (long) (3 * 60 * 1000), callback);
	}

	public void sendStreamhostUsed(JID to, String id, String sid, Streamhost streamhost) throws XMLException, JaxmppException {
		IQ iq = IQ.create();
		iq.setTo(to);
		iq.setId(id);
		iq.setType(StanzaType.result);

		Element query = new DefaultElement("query", null, XMLNS_BS);
		query.setAttribute("sid", sid);
		iq.addChild(query);

		Element streamhostUsed = new DefaultElement("streamhost-used");
		streamhostUsed.setAttribute("jid", streamhost.getJid());
		query.addChild(streamhostUsed);

		// session.registerResponseHandler(iq, callback);
		writer.write(iq);
	}

	public void sendStreamInitiationOffer(JID recipient, String filename, String mimetype, long filesize,
			String[] streamMethods, StreamInitiationOfferAsyncCallback callback) throws XMLException, JaxmppException {
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
		for (String streamMethod : streamMethods) {
			Element option = new DefaultElement("option");
			field.addChild(option);
			Element value = new DefaultElement("value", streamMethod, null);
			option.addChild(value);
		}

		writer.write(iq, (long) (10 * 60 * 1000), callback);
	}
}
