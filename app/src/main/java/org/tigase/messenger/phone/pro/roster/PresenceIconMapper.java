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

package org.tigase.messenger.phone.pro.roster;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CPresence;

public class PresenceIconMapper {

	public static int getPresenceResource(int presenceStatus) {
		int presenceIconResource;
		switch (presenceStatus) {
			case CPresence.OFFLINE:
				presenceIconResource = R.drawable.presence_offline;
				break;
			case CPresence.ERROR:
				presenceIconResource = R.drawable.presence_error;
				break;
			case CPresence.DND:
				presenceIconResource = R.drawable.presence_dnd;
				break;
			case CPresence.XA:
				presenceIconResource = R.drawable.presence_xa;
				break;
			case CPresence.AWAY:
				presenceIconResource = R.drawable.presence_away;
				break;
			case CPresence.ONLINE:
				presenceIconResource = R.drawable.presence_online;
				break;
			case CPresence.CHAT: // chat
				presenceIconResource = R.drawable.presence_chat;
				break;
			default:
				presenceIconResource = R.drawable.presence_unknown;
		}

		return presenceIconResource;
	}

}
