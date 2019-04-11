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

package org.tigase.messenger.jaxmpp.android.muc;

import android.util.Log;
import org.tigase.messenger.jaxmpp.android.chat.ChatProvider;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.AbstractRoomsManager;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

import java.util.List;

public class AndroidRoomsManager
		extends AbstractRoomsManager {

	private final ChatProvider provider;

	public AndroidRoomsManager(ChatProvider provider) {
		this.provider = provider;
	}

	@Override
	public boolean remove(Room room) {
		Log.i("AndroidRoomsManager", "Removing room " + room.getRoomJid() + " (" + room.getId() + ")");
		provider.close(room.getSessionObject(), room.getId());
		return super.remove(room);
	}

	@Override
	protected Room createRoomInstance(BareJID roomJid, String nickname, String password) {
		long roomId = provider.createMuc(sessionObject, JID.jidInstance(roomJid), nickname, password);
		Room room = new Room(roomId, context, roomJid, nickname);
		room.setPassword(password);
		return room;
	}

	@Override
	protected void initialize() {
		List<Room> datas = provider.getRooms(sessionObject, this.context);
		if (datas != null) {
			for (Room room : datas) {
				this.register(room);
			}
		}
	}

}
