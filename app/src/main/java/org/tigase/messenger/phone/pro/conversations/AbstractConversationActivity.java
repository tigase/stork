package org.tigase.messenger.phone.pro.conversations;

import android.content.Intent;
import android.view.MenuItem;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.MainActivity;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class AbstractConversationActivity
		extends AbstractServiceActivity {

	public static final String TEXT = "TEXT";

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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			startActivity(new Intent(this, MainActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onXMPPServiceConnected() {

	}

	@Override
	protected void onXMPPServiceDisconnected() {

	}

}
