package org.tigase.messenger.phone.pro.conversations.muc;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import org.tigase.messenger.jaxmpp.android.chat.MarkAsRead;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class MucActivity
		extends AbstractConversationActivity {

	TextView mContactName;
	private MarkAsRead markAsRead;
	private int openChatId;

	public int getOpenChatId() {
		return openChatId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.openChatId = getIntent().getIntExtra("openChatId", Integer.MIN_VALUE);
		setJid(JID.jidInstance(getIntent().getStringExtra("jid")));
		setAccount(BareJID.bareJIDInstance(getIntent().getStringExtra("account")));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_muc);

		mContactName = (TextView) findViewById(R.id.contact_display_name);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mContactName.setText("Room " + getJid().getLocalpart());

		this.markAsRead = new MarkAsRead(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(("muc:" + openChatId).hashCode());
		MessageNotification.cancelSummaryNotification(this);
		markAsRead.markGroupchatAsRead(this.openChatId, this.getAccount(), this.getJid());
	}
}
