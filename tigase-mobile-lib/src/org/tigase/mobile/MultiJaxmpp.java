package org.tigase.mobile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.EventType;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.observer.ObservableFactory;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

public class MultiJaxmpp {

	public static class ChatWrapper {

		private final Object data;

		public ChatWrapper(Chat chat) {
			this.data = chat;
		}

		public ChatWrapper(Room room) {
			this.data = room;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ChatWrapper) {
				return data.equals(((ChatWrapper) o).data);
			} else
				return data.equals(o);
		}

		public Chat getChat() {
			if (data instanceof Chat)
				return (Chat) data;
			else
				return null;
		}

		public Room getRoom() {
			if (data instanceof Room)
				return (Room) data;
			else
				return null;
		}

		@Override
		public int hashCode() {
			return data.hashCode();
		}

		public boolean isChat() {
			return data instanceof Chat;
		}

		public boolean isRoom() {
			return data instanceof Room;
		}

		@Override
		public String toString() {
			if (data instanceof Chat) {
				return "chatid:" + ((Chat) data).getId();
			} else if (data instanceof Room) {
				return "roomid:" + ((Room) data).getId();
			} else
				return super.toString();
		}
	}

	private final List<ChatWrapper> chats = Collections.synchronizedList(new ArrayList<ChatWrapper>());

	private final HashMap<BareJID, JaxmppCore> jaxmpps = new HashMap<BareJID, JaxmppCore>();

	private final Listener<BaseEvent> listener;

	private final Observable observable = ObservableFactory.instance();

	public MultiJaxmpp() {
		this.listener = new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) throws JaxmppException {
				synchronized (chats) {
					if (be.getType() == MessageModule.ChatCreated) {
						chats.add(new ChatWrapper(((MessageEvent) be).getChat()));
					} else if (be.getType() == MessageModule.ChatClosed) {
						chats.remove(new ChatWrapper(((MessageEvent) be).getChat()));
					} else if (be.getType() == MucModule.RoomClosed) {
						chats.remove(new ChatWrapper(((MucEvent) be).getRoom()));
					} else if (be.getType() == MucModule.JoinRequested) {
						chats.add(new ChatWrapper(((MucEvent) be).getRoom()));
					}
				}
				observable.fireEvent(be);
			}
		};
	}

	public <T extends JaxmppCore> void add(final T jaxmpp) {
		synchronized (jaxmpps) {
			jaxmpp.addListener(listener);
			jaxmpps.put(jaxmpp.getSessionObject().getUserBareJid(), jaxmpp);

			for (Chat c : jaxmpp.getModule(MessageModule.class).getChatManager().getChats()) {
				this.chats.add(new ChatWrapper(c));
			}

			for (Room r : jaxmpp.getModule(MucModule.class).getRooms()) {
				this.chats.add(new ChatWrapper(r));

			}
		}
	}

	public void addListener(EventType eventType, Listener<? extends BaseEvent> listener) {
		observable.addListener(eventType, listener);
	}

	public void addListener(Listener<? extends BaseEvent> listener) {
		observable.addListener(listener);
	}

	public Collection<JaxmppCore> get() {
		return Collections.unmodifiableCollection(jaxmpps.values());
	}

	public <T extends JaxmppCore> T get(final BareJID userJid) {
		synchronized (jaxmpps) {
			return (T) jaxmpps.get(userJid);
		}
	}

	public <T extends JaxmppCore> T get(final SessionObject sessionObject) {
		return get(sessionObject.getUserBareJid());
	}

	public ChatWrapper getChatById(final long id) {
		synchronized (chats) {
			for (ChatWrapper w : chats) {
				if (w.isChat() && w.getChat().getId() == id)
					return w;
			}
		}
		return null;
	}

	public List<ChatWrapper> getChats() {
		synchronized (chats) {
			return Collections.unmodifiableList(chats);
		}
	}

	public ChatWrapper getRoomById(final long id) {
		synchronized (chats) {
			for (ChatWrapper w : chats) {
				if (w.isRoom() && w.getRoom().getId() == id)
					return w;
			}
		}
		return null;
	}

	public <T extends JaxmppCore> void remove(final T jaxmpp) {
		synchronized (jaxmpps) {
			this.chats.removeAll(jaxmpp.getModule(MessageModule.class).getChatManager().getChats());
			jaxmpp.removeListener(listener);
			jaxmpps.remove(jaxmpp.getSessionObject().getUserBareJid());
		}
	}

	public void removeAllListeners() {
		observable.removeAllListeners();
	}

	public void removeListener(EventType eventType, Listener<? extends BaseEvent> listener) {
		observable.removeListener(eventType, listener);
	}

	public void removeListener(Listener<? extends BaseEvent> listener) {
		observable.removeListener(listener);
	}

}
