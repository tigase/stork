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
package org.tigase.mobile.service;

import java.io.File;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.filetransfer.FileTransferUtility;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransferEvent;
import tigase.jaxmpp.j2se.filetransfer.FileTransfer;
import tigase.jaxmpp.j2se.filetransfer.FileTransferManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FileTransferFeature {

	public static enum State {
		active,
		connecting,
		error,
		finished,
		negotiating
	}

	private static FileTransferFeature instance;

	private static final String TAG = "FileTransferFeature";

	public static void enableFileTransfer(Jaxmpp jaxmpp, Context context) {
		if (instance == null)
			return;

		FileTransferManager ftManager = jaxmpp.getFileTransferManager();
		if (ftManager != null)
			return;

		try {
			jaxmpp.initFileTransferManager(false);
			ftManager = jaxmpp.getFileTransferManager();

			ftManager.addListener(FileTransferManager.FILE_TRANSFER_PROGRESS, instance.progressListener);
			ftManager.addListener(FileTransferManager.FILE_TRANSFER_REQUEST, instance.requestListener);
			ftManager.addListener(FileTransferManager.FILE_TRANSFER_SUCCESS, instance.successListener);
			ftManager.addListener(FileTransferManager.FILE_TRANSFER_FAILURE, instance.failureListener);
		} catch (JaxmppException e) {
			Log.e(TAG, "Exception during preparation of file transfer manager", e);
		}
	}

	private final Listener<FileTransferEvent> failureListener;

	private final JaxmppService jaxmppService;

	private final Listener<FileTransferEvent> progressListener;

	private final Listener<FileTransferEvent> requestListener;

	private final Listener<FileTransferEvent> successListener;

	public FileTransferFeature(final JaxmppService jaxmppService) {
		instance = this;
		this.jaxmppService = jaxmppService;

		this.progressListener = new Listener<FileTransferEvent>() {

			@Override
			public void handleEvent(FileTransferEvent be) throws JaxmppException {
				// TODO Auto-generated method stub
				instance.jaxmppService.notificationHelper.notifyFileTransferProgress(be.getFileTransfer(), State.active);
			}

		};

		this.requestListener = new Listener<FileTransferEvent>() {

			@Override
			public void handleEvent(FileTransferEvent be) throws JaxmppException {
				instance.jaxmppService.notificationHelper.notifyFileTransferRequest(be.getFileTransfer());
			}

		};

		this.successListener = new Listener<FileTransferEvent>() {

			@Override
			public void handleEvent(FileTransferEvent be) throws JaxmppException {
				instance.jaxmppService.notificationHelper.notifyFileTransferProgress(be.getFileTransfer(), State.finished);
			}

		};

		this.failureListener = new Listener<FileTransferEvent>() {

			@Override
			public void handleEvent(FileTransferEvent be) throws JaxmppException {
				instance.jaxmppService.notificationHelper.notifyFileTransferProgress(be.getFileTransfer(), State.error);
			}

		};
	}

	protected void processFileTransferAction(Intent intent) {
		final JID peer = JID.jidInstance(intent.getStringExtra("peer"));
		final String sid = intent.getStringExtra("sid");
		final String tag = intent.getStringExtra("tag");

		final FileTransfer ft = (FileTransfer) FileTransferUtility.unregisterFileTransfer(peer, sid);

		final Jaxmpp jaxmpp = ((MessengerApplication) jaxmppService.getApplicationContext()).getMultiJaxmpp().get(
				ft.getSessionObject());
		if ("reject".equals(intent.getStringExtra("filetransferAction"))) {
			Log.v(TAG, "incoming file rejected");
			jaxmppService.notificationHelper.cancelFileTransferRequestNotification(tag);
			new Thread() {
				@Override
				public void run() {
					try {
						jaxmpp.getFileTransferManager().rejectFile(ft);
					} catch (JaxmppException e) {
						Log.e(TAG, "Could not send stream initiation reject", e);
					}
				}
			}.start();
		} else if ("accept".equals(intent.getStringExtra("filetransferAction"))) {
			jaxmppService.notificationHelper.cancelFileTransferRequestNotification(tag);
			String mimetype = ft.getFileMimeType();
			String filename = ft.getFilename();
			if (mimetype == null) {
				mimetype = FileTransferUtility.guessMimeType(filename);
				ft.setFileMimeType(mimetype);
			}

			String store = intent.getStringExtra("store");
			final File destination = FileTransferUtility.getPathToSave(filename, mimetype, store);
			ft.setFile(destination);

			new Thread() {
				@Override
				public void run() {
					try {
						jaxmpp.getFileTransferManager().acceptFile(ft);
					} catch (JaxmppException e) {
						Log.e(TAG, "Could not send stream initiation accept", e);
					}
				}
			}.start();

		}
	}
}
