package org.tigase.messenger.jaxmpp.android.muc;

import org.tigase.messenger.jaxmpp.android.chat.ChatProvider;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.AbstractRoomsManager;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

import java.util.List;

public class AndroidRoomsManager extends AbstractRoomsManager {

	private final ChatProvider provider;

	public AndroidRoomsManager(ChatProvider provider) {
		this.provider = provider;
	}

	@Override
	protected Room createRoomInstance(BareJID roomJid, String nickname,
									  String password) {
		long roomId = provider.createMuc(sessionObject, JID.jidInstance(roomJid), nickname, password);
		Room room = new Room(roomId, context, roomJid, nickname);
		room.setPassword(password);
		return room;
	}

	@Override
	protected void initialize() {
		List<Object[]> datas = provider.getRooms(sessionObject);
		if (datas != null) {
			for (Object[] data : datas) {
				BareJID roomJid = (BareJID) data[1];
				String nickname = (String) data[2];
				String password = (String) data[3];
				Room room = new Room((Long) data[0], this.context, roomJid, nickname);
				room.setPassword(password);
				this.register(room);
			}
		}
	}

	@Override
	public boolean remove(Room room) {
		provider.close(room.getSessionObject(), room.getId());
		return super.remove(room);
	}

}
