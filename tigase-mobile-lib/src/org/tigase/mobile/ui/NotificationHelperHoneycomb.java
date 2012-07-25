package org.tigase.mobile.ui;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.filetransfer.FileTransfer;
import org.tigase.mobile.filetransfer.FileTransferRequestEvent;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

@TargetApi(11)
public class NotificationHelperHoneycomb extends NotificationHelper {

	protected NotificationHelperHoneycomb(Context context) {
		super(context);
	}

	@Override
	protected Notification prepareFileTransferRequestNotification(int ico, String title, String text,
			FileTransferRequestEvent ev, JaxmppCore jaxmpp, String tag) {

		Notification.Builder builder = prepareFileTransferRequestNotificationInt(ico, title, text, ev, jaxmpp, tag);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	protected Notification.Builder prepareFileTransferRequestNotificationInt(int ico, String title, String text,
			FileTransferRequestEvent ev, JaxmppCore jaxmpp, String tag) {

		Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
		builder.setDefaults(Notification.DEFAULT_SOUND).setOngoing(true);
		builder.setSmallIcon(ico).setContentTitle(title);
		builder.setAutoCancel(true).setLights(Color.GREEN, 500, 500);

		PendingIntent pendingIntent = this.createFileTransferRequestPendingIntent(ev, jaxmpp, tag);
		builder.setContentIntent(pendingIntent).setContentText(text);

		return builder;
	}

	@Override
	protected Notification prepareFileTransferProgressNotification(int ico, String title, String text, FileTransfer ft) {
		Notification.Builder builder = this.prepareFileTransferProgressNotificationInt(ico, title, text, ft);
		return builder.getNotification();
	}

	protected Notification.Builder prepareFileTransferProgressNotificationInt(int ico, String title, String text,
			FileTransfer ft) {
		Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
		builder.setSmallIcon(ico);
		FileTransfer.State state = ft.getState();
		builder.setDefaults(0).setContentTitle(title).setContentText(text);

		boolean finished = state == FileTransfer.State.error || state == FileTransfer.State.finished;
		builder.setAutoCancel(finished).setOngoing(!finished);

		PendingIntent pendingIntent = createFileTransferProgressPendingIntent(ft);
		builder.setContentIntent(pendingIntent);
		return builder;
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException {
		Notification.Builder builder = prepareChatNotificationInt(ico, title, text, pendingIntent, event);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	protected Notification.Builder prepareChatNotificationInt(int ico, String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException {
		Notification.Builder builder = new Notification.Builder(context);
		builder.setContentTitle(title).setLights(Color.GREEN, 500, 500);
		builder.setDefaults(Notification.DEFAULT_SOUND);
		Bitmap avatar = AvatarHelper.getAvatar(event.getChat().getJid().getBareJid());
		builder.setSmallIcon(ico).setContentIntent(pendingIntent).setAutoCancel(true);
		if (avatar != AvatarHelper.mPlaceHolderBitmap) {
			builder.setLargeIcon(avatar);
		}

		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(event.getChat().getJid().getBareJid().toString()));
		Cursor c = context.getContentResolver().query(uri, null,
				ChatTableMetaData.FIELD_STATE + "=" + ChatTableMetaData.STATE_INCOMING_UNREAD, null, null);
		try {
			int count = c.getCount();
			builder.setNumber(count);

			prepareChatNotificationUnreadMessages(builder, c);
		} catch (Exception ex) {
			Log.e(TAG, "exception preparing notification", ex);
		} finally {
			c.close();
		}

		return builder;
	}

	protected void prepareChatNotificationUnreadMessages(Notification.Builder builder, Cursor c) {

	}

}
