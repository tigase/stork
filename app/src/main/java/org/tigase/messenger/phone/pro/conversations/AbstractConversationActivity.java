package org.tigase.messenger.phone.pro.conversations;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.support.v7.app.AppCompatActivity;

public class AbstractConversationActivity extends AppCompatActivity {

	private JID jid;
	private BareJID account;

	public BareJID getAccount() {
		return account;
	}

	protected void setAccount(BareJID account) {
		this.account = account;
	}

	public JID getJid() {
		return jid;
	}

	protected void setJid(JID jid) {
		this.jid = jid;
	}

}
