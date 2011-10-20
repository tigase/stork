package org.tigase.mobile;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.content.Context;
import android.util.Log;

public class RosterDisplayTools {

	private final Context context;

	public RosterDisplayTools(Context context) {
		super();
		this.context = context;
	}

	public String getDisplayName(final BareJID jid) {
		tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = XmppService.jaxmpp(context).getRoster().get(jid);
		return getDisplayName(item);
	}

	public String getDisplayName(final RosterItem item) {
		if (item == null)
			return null;
		else if (item.getName() != null && item.getName().length() != 0) {
			return item.getName();
		} else {
			return item.getJid().toString();
		}
	}

	public CPresence getShowOf(final BareJID jid) {
		tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = XmppService.jaxmpp(context).getRoster().get(jid);
		return getShowOf(item);
	}

	public CPresence getShowOf(final tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item) {
		try {
			if (item == null)
				return CPresence.notinroster;
			if (item.isAsk())
				return CPresence.requested;
			if (item.getSubscription() == Subscription.none || item.getSubscription() == Subscription.to)
				return CPresence.offline_nonauth;
			PresenceStore presenceStore = XmppService.jaxmpp(context).getModulesManager().getModule(PresenceModule.class).getPresence();
			Presence p = presenceStore.getBestPresence(item.getJid());
			CPresence r = CPresence.offline;
			if (p != null) {
				if (p.getType() == StanzaType.unavailable)
					r = CPresence.offline;
				else if (p.getShow() == Show.online)
					r = CPresence.online;
				else if (p.getShow() == Show.away)
					r = CPresence.away;
				else if (p.getShow() == Show.chat)
					r = CPresence.chat;
				else if (p.getShow() == Show.dnd)
					r = CPresence.dnd;
				else if (p.getShow() == Show.xa)
					r = CPresence.xa;
			}
			return r;
		} catch (Exception e) {
			Log.e("tigase", "Can't calculate presence", e);
			return CPresence.error;
		}
	}

}
