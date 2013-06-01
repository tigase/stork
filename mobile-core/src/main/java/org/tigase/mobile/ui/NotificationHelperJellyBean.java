package org.tigase.mobile.ui;

import org.tigase.mobile.R;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.service.FileTransferFeature;

import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransfer;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

@TargetApi(16)
public class NotificationHelperJellyBean extends NotificationHelperICS {

	protected NotificationHelperJellyBean(Context context) {
		super(context);
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException {
		Notification.Builder builder = prepareChatNotificationInt(ico, title, text, pendingIntent, event);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	@Override
	protected void prepareChatNotificationUnreadMessages(Notification.Builder builder, Cursor c) {
		Notification.InboxStyle style = new Notification.InboxStyle();
		int count = c.getCount();
		int used = 0;
		int fieldBodyIdx = c.getColumnIndex(ChatTableMetaData.FIELD_BODY);
		while (c.moveToNext() && used < 3) {
			String body = c.getString(fieldBodyIdx);
			style.addLine(body);
			used++;
		}
		if (count > 3) {
			style.setSummaryText("...");
		} else if (count <= 3) {
			style.setSummaryText("");
		}
		builder.setStyle(style);
	}

	@Override
	protected Notification prepareFileTransferProgressNotification(int ico, String title, String text, FileTransfer ft, FileTransferFeature.State state) {
		Notification.Builder builder = this.prepareFileTransferProgressNotificationInt(ico, title, text, ft, state);
		return builder.build();
	}

	@Override
	protected Notification prepareFileTransferRequestNotification(int ico, String title, String text,
			FileTransfer ft, JaxmppCore jaxmpp, String tag) {

		Notification.Builder builder = prepareFileTransferRequestNotificationInt(ico, title, text, ft, jaxmpp, tag);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	@Override
	protected Notification.Builder prepareFileTransferRequestNotificationInt(int ico, String title, String text,
			FileTransfer ft, JaxmppCore jaxmpp, String tag) {
		Notification.Builder builder = super.prepareFileTransferRequestNotificationInt(ico, title, text, ft, jaxmpp, tag);

		Intent intentReject = NotificationHelper.createFileTransferRejectIntent(context, ft,
				jaxmpp.getSessionObject().getUserBareJid(), tag);
		builder.addAction(
				android.R.drawable.ic_menu_close_clear_cancel,
				context.getString(R.string.reject),
				PendingIntent.getService(context, 1, intentReject, PendingIntent.FLAG_CANCEL_CURRENT
						| PendingIntent.FLAG_ONE_SHOT));

		Intent intentAccept = NotificationHelper.createFileTransferAcceptIntent(context, ft,
				jaxmpp.getSessionObject().getUserBareJid(), tag);
		builder.addAction(
				android.R.drawable.ic_menu_save,
				context.getString(R.string.accept),
				PendingIntent.getService(context, 2, intentAccept, PendingIntent.FLAG_CANCEL_CURRENT
						| PendingIntent.FLAG_ONE_SHOT));

		return builder;
	}
}
