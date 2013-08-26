/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
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
		builder.setProgress(100, (int) (progress == null ? 0 : progress.doubleValue()),
				state == FileTransferFeature.State.connecting || state == FileTransferFeature.State.negotiating);

		return builder;
	}

}
