package org.tigase.mobile.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class AndroidFileTransferUtility {

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

	private static final String TAG = "AndroidFileTransferUtility";
	private static final Timer timer = new Timer(true);

	private static final Map<String, FileTransfer> waitingForStreamhosts = Collections.synchronizedMap(new HashMap<String, FileTransfer>());

	public static void fileTransferHostsEventReceived(Jaxmpp jaxmpp, StreamhostsEvent be) {
		FileTransfer ft = getFileTransferForStreamhost(be.getSid());
		if (ft == null) {
			Log.w(TAG, "file transfer for sid = " + be.getSid() + " not found!");
			return;
		}
		File file = ft.destination;
		boolean connected = false;
		for (Streamhost streamhost : be.getHosts()) {
			try {
				ft.connectToProxy(streamhost, be.getId());
				if (!file.exists()) {
					try {
						file.createNewFile();
					} catch (IOException ex) {
						Log.v(TAG, "could not create file = " + file.getAbsolutePath());
						throw ex;
					}
				}
				connected = true;
				ft.setOutputStream(new FileOutputStream(file));
				break;
			} catch (IOException ex) {
				Log.v(TAG, "could not connect to streamhost = " + streamhost.getAddress() + ":" + streamhost.getPort(), ex);
			}
		}
		if (!connected) {
			ft.transferError("could not connect to any streamhost");
		}
	}

	public static FileTransfer getFileTransferForStreamhost(final String id) {
		return waitingForStreamhosts.get(id);
	}

	public static Jaxmpp getJaxmpp(Activity activity, BareJID account) {
		return ((MessengerApplication) activity.getApplicationContext()).getMultiJaxmpp().get(account);
	}

	public static void refreshMediaScanner(Context context, File file) {
		final MediaScannerConnectionRefreshClient client = new MediaScannerConnectionRefreshClient();
		client.mediaScanner = new MediaScannerConnection(context, client);
		client.path = file.getPath();
		client.mediaScanner.connect();
	}

	public static void registerFileTransferForStreamhost(final String id, FileTransfer ft) {
		waitingForStreamhosts.put(id, ft);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				waitingForStreamhosts.remove(id);
			}

		}, 5L * 60 * 1000);
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
			cursor.close();
		} catch (Exception ex) {
			Log.e(TAG, "should not happen", ex);
		}

		return filename;
	}

	public static void startFileTransfer(final Activity activity, final RosterItem ri, final JID jid, final Uri uri,
			final String mimetype) {
		final ContentResolver cr = activity.getContentResolver();
		final String name = ri.getName() != null ? ri.getName() : ri.getJid().toString();
		final Jaxmpp jaxmpp = getJaxmpp(activity, ri.getSessionObject().getUserBareJid());

		new Thread() {
			@Override
			public void run() {

				try {
					final String filename = resolveFilename(activity, uri, mimetype);

					if (jid == null)
						return;
					final FileTransferModule ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
					final InputStream is = cr.openInputStream(uri);
					final long size = is.available();
					final FileTransfer ft = new FileTransfer(jaxmpp, jid, name, filename, is, size);
					ftModule.sendStreamInitiationOffer(jid, filename, mimetype, size, new String[] { Features.BYTESTREAMS },
							new StreamInitiationOfferAsyncCallback() {
								@Override
								public void onAccept(String sid) {
									Log.v(TAG, "stream initiation accepted by " + jid.toString());
									ft.setSid(sid);
									FileTransferUtility.onStreamAccepted(ft);
								}

								@Override
								public void onError() {
									Log.v(TAG, "stream initiation failed for " + jid.toString());
									ft.transferError("transfer initiation failed");
								}

								@Override
								public void onReject() {
									Log.v(TAG, "stream initiation rejected by " + jid.toString());
									ft.transferError("transfer rejected");
								}
							});
				} catch (XMLException e) {
					Log.e(TAG, "WTF?", e);
				} catch (JaxmppException e) {
					Log.e(TAG, "WTF?", e);
				} catch (FileNotFoundException e) {
					Log.e(TAG, "WTF?", e);
				} catch (IOException e) {
					Log.e(TAG, "WTF?", e);
				}
			}
		}.start();
	}
}
