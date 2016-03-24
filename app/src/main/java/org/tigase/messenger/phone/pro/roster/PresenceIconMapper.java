/*
 * PresenceIconMapper.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
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

package org.tigase.messenger.phone.pro.roster;

import org.tigase.messenger.phone.pro.db.CPresence;

public class PresenceIconMapper {

	public static int getPresenceResource(int presenceStatus) {
		int presenceIconResource;
		switch (presenceStatus) {
		case CPresence.OFFLINE:
			presenceIconResource = android.R.drawable.presence_invisible;
			break;
		case CPresence.ERROR:
			presenceIconResource = android.R.drawable.presence_offline;
			break;
		case CPresence.DND:
			presenceIconResource = android.R.drawable.presence_busy;
			break;
		case CPresence.XA:
			presenceIconResource = android.R.drawable.presence_away;
			break;
		case CPresence.AWAY:
			presenceIconResource = android.R.drawable.presence_away;
			break;
		case CPresence.ONLINE:
			presenceIconResource = android.R.drawable.presence_online;
			break;
		case CPresence.CHAT: // chat
			presenceIconResource = android.R.drawable.presence_online;
			break;
		default:
			presenceIconResource = android.R.drawable.presence_offline;
		}

		return presenceIconResource;
	}

}
