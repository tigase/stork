package org.tigase.mobile;

import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.DnsResolver;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.Entry;

public class DNSResolver implements DnsResolver {

	@Override
	public List<Entry> resolve(final String hostname) {
		ArrayList<Entry> result = new ArrayList<Entry>();
		try {
			Record[] rr = (new Lookup("_xmpp-client._tcp." + hostname, Type.SRV)).run();
			for (Record r : rr) {
				SRVRecord record = (SRVRecord) r;
				String name = record.getAdditionalName().toString();
				if (name.endsWith(".")) {
					name = name.substring(0, name.length() - 1);
				}

				result.add(new Entry(name, record.getPort()));
			}
		} catch (Exception e) {
			result.add(new Entry(hostname, 5222));
		}
		return result;
	}

}
