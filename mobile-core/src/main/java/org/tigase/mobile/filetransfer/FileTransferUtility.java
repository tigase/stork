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
package org.tigase.mobile.filetransfer;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransfer;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class FileTransferUtility {

	private static final class MediaScannerConnectionRefreshClient implements
			MediaScannerConnection.MediaScannerConnectionClient {
		protected MediaScannerConnection mediaScanner;
		protected String path;

		@Override
		public void onMediaScannerConnected() {
			mediaScanner.scanFile(path, null);
		}

		@Override
		public void onScanCompleted(String path, Uri uri) {
			mediaScanner.disconnect();
		}
	}

	public static final String[] FEATURES = { Features.BYTESTREAMS, Features.FILE_TRANSFER };

	private static final String TAG = "FileTransferUtility";

	private static Map<String, FileTransfer> waitingFileTransfers = new HashMap<String, FileTransfer>();

	public static JID getBestJidForFeatures(Jaxmpp jaxmpp, BareJID jid, String[] features) {
		try {
			CapabilitiesCache capsCache = jaxmpp.getModule(CapabilitiesModule.class).getCache();
			Set<String> nodes = capsCache.getNodesWithFeature(features[0]);

			for (int i = 1; i < features.length; i++) {
				nodes.retainAll(capsCache.getNodesWithFeature(features[i]));
			}

			Presence current = null;
			Map<String, Presence> allPresences = jaxmpp.getPresence().getPresences(jid);
			if (allPresences != null) {
				for (Presence p : allPresences.values()) {
					Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
					if (c == null)
						continue;

					final String node = c.getAttribute("node");
					final String ver = c.getAttribute("ver");

					if (nodes.contains(node + "#" + ver)) {
						if (current == null || current.getPriority() < p.getPriority())
							current = p;
					}
				}
			}

			return (current != null) ? current.getFrom() : null;
		} catch (XMLException ex) {
			return null;
		}
	}

	public static FileTransfer getFileTransfer(JID peerJid, String sid) {
		synchronized (waitingFileTransfers) {
			return waitingFileTransfers.remove(peerJid.toString() + ":" + sid);
		}
	}

	public static Jaxmpp getJaxmpp(Activity activity, BareJID account) {
		return ((MessengerApplication) activity.getApplicationContext()).getMultiJaxmpp().get(account);
	}

	public static File getPathToSave(String filename, String mimetype, String store) {
		File path = null;
		String type = Environment.DIRECTORY_DOWNLOADS;
		if (store != null) {
			if ("Pictures".equals(store)) {
				type = Environment.DIRECTORY_PICTURES;
			} else if ("Music".equals(store)) {
				type = Environment.DIRECTORY_MUSIC;
			} else if ("Movies".equals(store)) {
				type = Environment.DIRECTORY_MOVIES;
			}
		} else {
			if (mimetype.startsWith("video/")) {
				type = Environment.DIRECTORY_MOVIES;
			} else if (mimetype.startsWith("audio/")) {
				type = Environment.DIRECTORY_MUSIC;
			} else if (mimetype.startsWith("image/")) {
				type = Environment.DIRECTORY_PICTURES;
			}
		}
		path = Environment.getExternalStoragePublicDirectory(type);

		if (path == null) {
			path = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "Download" + File.separator);
		}
		return new File(path, filename);
	}

	public static String guessMimeType(String filename) {
		int idx = filename.lastIndexOf(".");
		if (idx == -1) {
			return null;
		}
		String suffix = filename.substring(idx + 1).toLowerCase();
		if (suffix.equals("png")) {
			return "image/png";
		} else if (suffix.equals("jpg") || suffix.equals("jpeg")) {
			return "image/jpeg";
		} else if (suffix.equals("avi")) {
			return "video/avi";
		} else if (suffix.equals(".mkv")) {
			return "video/x-matroska";
		} else if (suffix.equals(".mpg") || suffix.equals(".mp4")) {
			return "video/mpeg";
		} else if (suffix.equals(".mp3")) {
			return "audio/mpeg3";
		} else if (suffix.equals(".ogg")) {
			return "audio/ogg";
		} else if (suffix.equals(".pdf")) {
			return "application/pdf";
		} else {
			return null;
		}
	}

	public static void refreshMediaScanner(Context context, File file) {
		final MediaScannerConnectionRefreshClient client = new MediaScannerConnectionRefreshClient();
		client.mediaScanner = new MediaScannerConnection(context, client);
		client.path = file.getPath();
		client.mediaScanner.connect();
	}

	public static void registerFileTransfer(FileTransfer ft) {
		synchronized (waitingFileTransfers) {
			waitingFileTransfers.put(ft.getPeer().toString() + ":" + ft.getSid(), ft);
		}
	}

	public static String resolveFilename(Activity activity, Uri uri, String mimetype) {
		if (uri == null)
			return "";

		String filename = uri.getLastPathSegment();
		try {
			String[] proj = { MediaStore.MediaColumns.DISPLAY_NAME };
			Cursor cursor = activity.managedQuery(uri, proj, null, null, null);
			int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
			if (cursor.moveToFirst()) {
				filename = cursor.getString(columnIndex);
				Log.v(TAG, "for uri = " + uri.toString() + " found name = " + filename);
			} else {
				// it should not happen
				Log.v(TAG, "no file for uri = " + uri.toString());
			}
			// we are using managedQuery so we are not allowed to close this
			// cursor!
			// cursor.close();
		} catch (Exception ex) {
			Log.e(TAG, "should not happen", ex);
		}

		return filename;
	}

	public static boolean resourceContainsFeatures(Jaxmpp jaxmpp, JID jid, String[] features) throws XMLException {
		CapabilitiesCache capsCache = jaxmpp.getModule(CapabilitiesModule.class).getCache();
		Set<String> nodes = capsCache.getNodesWithFeature(features[0]);

		for (int i = 1; i < features.length; i++) {
			nodes.retainAll(capsCache.getNodesWithFeature(features[i]));
		}

		Presence p = jaxmpp.getPresence().getPresence(jid);
		if (p == null)
			return false;

		Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
		if (c == null)
			return false;

		final String node = c.getAttribute("node");
		final String ver = c.getAttribute("ver");

		return nodes.contains(node + "#" + ver);
	}

	public static void startFileTransfer(final Activity activity, final Jaxmpp jaxmpp, final JID peerJid, final Uri uri,
			final String mimetype) {
		new Thread() {
			@Override
			public void run() {
				try {
					final ContentResolver cr = activity.getContentResolver();
					final InputStream is = cr.openInputStream(uri);
					final long size = is.available();

					String filename = FileTransferUtility.resolveFilename(activity, uri, mimetype);
					jaxmpp.getFileTransferManager().sendFile(peerJid, filename, size, is, null);
				} catch (Exception ex) {
					Log.e(TAG, "problem with starting filetransfer", ex);
				}
			}
		}.start();
	}

	public static FileTransfer unregisterFileTransfer(JID peerJid, String sid) {
		synchronized (waitingFileTransfers) {
			return waitingFileTransfers.remove(peerJid.toString() + ":" + sid);
		}
	}

}
