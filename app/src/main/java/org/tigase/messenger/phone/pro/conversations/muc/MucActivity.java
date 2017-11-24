package org.tigase.messenger.phone.pro.conversations.muc;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import org.tigase.messenger.jaxmpp.android.chat.MarkAsRead;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.conversations.imagepreview.PreviewImageActivity;
import org.tigase.messenger.phone.pro.notifications.MessageNotification;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;

public class MucActivity
		extends AbstractConversationActivity {

	public static final int PREVIEW_IMAGE_REQUEST_CODE = 1;
	TextView mContactName;
	private MarkAsRead markAsRead;
	private int openChatId;

	private void doPreviewImage(final Intent data) {
		startWhenBinded(() -> {
			Intent intent = new Intent(this, PreviewImageActivity.class);
			intent.putExtra(PreviewImageActivity.ACCOUNT_KEY, getAccount().toString());
			intent.putExtra(PreviewImageActivity.JID_KEY, getJid().getBareJid().toString());
			intent.setData(data.getData());

			startActivityForResult(intent, PREVIEW_IMAGE_REQUEST_CODE);
		});
	}

	private void doUploadFile(Intent data) {
		Intent ssIntent = new Intent(getApplicationContext(), XMPPService.class);
		ssIntent.setAction(MessageSender.SEND_GROUPCHAT_MESSAGE_ACTION);
		ssIntent.putExtra(MessageSender.LOCAL_CONTENT_URI, data.getData());

		ssIntent.putExtra(MessageSender.BODY, data.getStringExtra(TEXT));
		startWhenBinded(() -> {
			ssIntent.putExtra(MessageSender.ACCOUNT, getAccount().toString());
			ssIntent.putExtra(MessageSender.ROOM_JID, getJid().getBareJid().toString());
			getApplicationContext().startService(ssIntent);
		});
	}

	public int getOpenChatId() {
		return openChatId;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PREVIEW_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			doUploadFile(data);
		} else if (requestCode == FILE_UPLOAD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			doPreviewImage(data);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
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
