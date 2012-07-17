package org.tigase.mobile;


import org.tigase.mobile.MultiJaxmpp.ChatWrapper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.view.MenuItem;

@TargetApi(11)
public class TigaseMobileMessengerActivityHelperHoneycomb extends TigaseMobileMessengerActivityHelper {
	
	protected TigaseMobileMessengerActivityHelperHoneycomb(TigaseMobileMessengerActivity activity) {
		super(activity);
	}

	@Override
	public void invalidateOptionsMenu() {
		activity.invalidateOptionsMenu();
	}	
	
	public void setShowAsAction(MenuItem item, int actionEnum) { 
		item.setShowAsAction(actionEnum);
	}
	
	@Override
	public void updateActionBar() {
		int currentPage = activity.getCurrentPage();
		
		ActionBar actionBar = activity.getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(currentPage != 1 && !isXLarge());

		// Setting subtitle to show who we chat with
		ChatWrapper c = activity.getChatByPageIndex(currentPage);
		if (c != null) {
//			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
			String subtitle = null;
			if (c.getChat() != null) {
				BareJID jid = c.getChat().getJid().getBareJid();
				RosterItem ri = c.getChat().getSessionObject().getRoster().get(jid);
				subtitle = "Chat with " + (ri != null ? ri.getName() : jid.toString());
			}
			else if (c.getRoom() != null) {
				subtitle = "Room " + c.getRoom().getRoomJid().toString(); 
			}
			actionBar.setSubtitle(subtitle);
		} else {
			if (currentPage == 0) {
				actionBar.setSubtitle("Accounts");					
			}
			else {
				actionBar.setSubtitle(null);					
			}
		}
	}

}
