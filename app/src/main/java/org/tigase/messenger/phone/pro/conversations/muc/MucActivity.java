package org.tigase.messenger.phone.pro.conversations.muc;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

public class MucActivity extends AbstractConversationActivity {

	@Bind(R.id.contact_display_name)
	TextView mContactName;
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
		ButterKnife.bind(this);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mContactName.setText("Room " + getJid().getLocalpart());
	}

	@Override
	protected void onResume() {
		super.onResume();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(("muc:" + openChatId).hashCode());
	}
}
