package org.tigase.mobile;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side presence
 * 
 * @author bmalkow
 * 
 */
public enum CPresence {
	away(99),
	chat(101),
	dnd(97),
	error(50),
	invisible(1),
	notinroster(0),
	offline(10),
	offline_nonauth(11),
	online(100),
	requested(51),
	xa(98);

	private static Map<Integer, CPresence> cache = null;

	public static final CPresence valueOf(final Integer id) {
		if (cache == null) {
			cache = new HashMap<Integer, CPresence>();
			for (CPresence c : values()) {
				cache.put(c.getId(), c);
			}
		}
		return cache.get(id);
	}

	private final Integer id;

	private CPresence(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

}
