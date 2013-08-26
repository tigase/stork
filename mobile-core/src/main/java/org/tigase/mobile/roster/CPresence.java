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
package org.tigase.mobile.roster;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side presence
 * 
 * @author bmalkow
 * 
 */
public enum CPresence {
	away(98),
	chat(101),
	dnd(99),
	error(50),
	invisible(1),
	notinroster(0),
	offline(80),

	offline_nonauth(70),
	online(100),
	requested(75),

	xa(97);

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
