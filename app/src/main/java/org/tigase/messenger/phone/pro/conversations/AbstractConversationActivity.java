package org.tigase.messenger.phone.pro.conversations;

import android.support.v7.app.AppCompatActivity;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class AbstractConversationActivity
		extends AppCompatActivity {

	public static final int FILE_UPLOAD_REQUEST_CODE = 109;

	private BareJID account;
	private JID jid;

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
