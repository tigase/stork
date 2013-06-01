package org.tigase.mobile.ui;

import org.tigase.mobile.service.FileTransferFeature;

import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransfer;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;

@TargetApi(14)
public class NotificationHelperICS extends NotificationHelperHoneycomb {

	protected NotificationHelperICS(Context context) {
		super(context);
	}

	@Override
	protected Notification.Builder prepareFileTransferProgressNotificationInt(int ico, String title, String text,
			FileTransfer ft, FileTransferFeature.State state) {
		Notification.Builder builder = super.prepareFileTransferProgressNotificationInt(ico, title, text, ft, state);
		
		Double progress = ft.getProgress();
		builder.setProgress(100, (int)(progress == null ? 0 : progress.doubleValue()), state == FileTransferFeature.State.connecting
				|| state == FileTransferFeature.State.negotiating);

		return builder;
	}

}
