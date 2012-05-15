package org.tigase.mobile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.DnsResolver;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector.Entry;

public class DNSResolver implements DnsResolver {

	private static Entry resolveSRV(String domain) {
		String hostName = null;
		int hostPort = -1;
		int priority = Integer.MAX_VALUE;
		int weight = 0;
		Lookup lookup;

		try {
			lookup = new Lookup(domain, Type.SRV);
			Record recs[] = lookup.run();
			if (recs == null) {
				return null;
			}
			for (Record rec : recs) {
				SRVRecord record = (SRVRecord) rec;
				if (record != null && record.getTarget() != null) {
					int _weight = (int) (record.getWeight() * record.getWeight() * Math.random());
					if (record.getPriority() < priority) {
						priority = record.getPriority();
						weight = _weight;
						hostName = record.getTarget().toString();
						hostPort = record.getPort();
					} else if (record.getPriority() == priority) {
						if (_weight > weight) {
							priority = record.getPriority();
							weight = _weight;
							hostName = record.getTarget().toString();
							hostPort = record.getPort();
						}
					}
				}
			}
		} catch (TextParseException e) {
		} catch (NullPointerException e) {
		}
		if (hostName == null) {
			return null;
		} else if (hostName.endsWith(".")) {
			hostName = hostName.substring(0, hostName.length() - 1);
		}
		return new Entry(hostName, hostPort);
	}

	private final HashMap<String, Entry> cache = new HashMap<String, Entry>();

	private long lastAccess = -1;

	public DNSResolver() {
	}

	@Override
	public List<Entry> resolve(final String hostname) {
		ArrayList<Entry> result = new ArrayList<Entry>();
		synchronized (cache) {
			long now = (new Date()).getTime();
			if (now - lastAccess > 1000 * 60 * 10) {
				cache.clear();
			}
			lastAccess = now;
			if (cache.containsKey(hostname)) {
				Entry address = cache.get(hostname);
				if (address != null) {
					result.add(address);
					return result;
				}
			}
		}

		try {
			Entry rr = resolveSRV("_xmpp-client._tcp." + hostname);

			if (rr == null) {
				rr = new Entry(hostname, 5222);
			}

			synchronized (cache) {
				cache.put(hostname, rr);
			}

			result.add(rr);
		} catch (Exception e) {
			result.add(new Entry(hostname, 5222));
		}
		return result;
	}

}
