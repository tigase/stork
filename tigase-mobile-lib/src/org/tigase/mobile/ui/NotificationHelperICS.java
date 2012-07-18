package org.tigase.mobile.ui;

import org.tigase.mobile.filetransfer.FileTransfer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;

@TargetApi(14)
public class NotificationHelperICS extends NotificationHelperHoneycomb {

	protected NotificationHelperICS(Context context) {
		super(context);
	}

	protected Notification.Builder prepareFileTransferProgressNotificationInt(int ico, String title, String text, FileTransfer ft) {
		Notification.Builder builder = super.prepareFileTransferProgressNotificationInt(ico, title, text, ft);

		FileTransfer.State state = ft.getState();
		builder.setProgress(100, ft.getProgress(), state == FileTransfer.State.connecting
				|| state == FileTransfer.State.negotiating);
		
		return builder;
	}
	
}
