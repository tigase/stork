package org.tigase.mobile.filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.filetransfer.FileTransfer.State;
import org.tigase.mobile.net.SocketThread;

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
import android.os.Build;
import android.os.Environment;
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

	private static ServerSocketChannel serverSocketChannel = null;
	private static final String TAG = "AndroidFileTransferUtility";

	private static final Timer timer = new Timer(true);
	private static final Map<String, FileTransfer> waitingForConnection = Collections.synchronizedMap(new HashMap<String, FileTransfer>());

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

	public static FileTransfer getFileTransferForConnection(final String id) {
		return waitingForConnection.remove(id);
	}

	public static FileTransfer getFileTransferForStreamhost(final String id) {
		return waitingForStreamhosts.get(id);
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

	public static void onStreamhostsReceived(FileTransfer ft, List<Streamhost> hosts) {
		registerFileTransferForConnection(ft);
		List<Streamhost> hostsLocal = new ArrayList<Streamhost>();
		try {
			synchronized (AndroidFileTransferUtility.class) {
				if (serverSocketChannel == null) {
					serverSocketChannel = ServerSocketChannel.open();
					serverSocketChannel.configureBlocking(true);
					serverSocketChannel.socket().bind(new InetSocketAddress(0));
					new Thread() {
						@Override
						public void run() {
							while (true) {
								try {
									SocketChannel channel = serverSocketChannel.accept();
									if (channel.isConnectionPending()) {
										channel.finishConnect();
									}
									Socks5IOService serv = new Socks5IOService(channel, true);
									// serv.processSocketData();
									SocketThread.addSocketService(serv);
								} catch (Exception ex) {
									Log.e(TAG, "socket accepting exception", ex);
								}
							}
						}
					}.start();
				}
			}
			// need to accept connection somewhere
			int port = serverSocketChannel.socket().getLocalPort();
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String addr = inetAddress.getHostAddress().toString();
						String jid = ft.jid.toString();
						Streamhost host = new Streamhost(jid, addr, port);
						hostsLocal.add(host);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		hostsLocal.addAll(hosts);
		if (hostsLocal.isEmpty()) {
			ft.transferError("could not connect to destination");
		} else {
			FileTransferUtility.onStreamhostsReceived(ft, hostsLocal);
		}
	}

	public static void refreshMediaScanner(Context context, File file) {
		final MediaScannerConnectionRefreshClient client = new MediaScannerConnectionRefreshClient();
		client.mediaScanner = new MediaScannerConnection(context, client);
		client.path = file.getPath();
		client.mediaScanner.connect();
	}

	public static void registerFileTransferForConnection(FileTransfer ft) {
		final String id = ft.getAuthString();
		waitingForConnection.put(id, ft);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				FileTransfer ft = waitingForConnection.remove(id);
				if (ft != null && ft.getState() == State.negotiating) {
					ft.transferError("negotiation timed out");
				}

			}

		}, 5L * 60 * 1000);
	}

	public static void registerFileTransferForStreamhost(final String id, FileTransfer ft) {
		waitingForStreamhosts.put(id, ft);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				FileTransfer ft = waitingForStreamhosts.remove(id);
				if (ft != null && ft.getState() == State.negotiating) {
					ft.transferError("negotiation timed out");
				}

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
			// we are using managedQuery so we are not allowed to close this
			// cursor!
			// cursor.close();
		} catch (Exception ex) {
			Log.e(TAG, "should not happen", ex);
		}

		return filename;
	}

	public static void startFileTransfer(final Activity activity, final RosterItem ri, final JID jid, final Uri uri,
			final String mimetype) {
		final String name = ri.getName() != null ? ri.getName() : ri.getJid().toString();
		final Jaxmpp jaxmpp = getJaxmpp(activity, ri.getSessionObject().getUserBareJid());

		new Thread() {
			@Override
			public void run() {

				try {
					final String filename = resolveFilename(activity, uri, mimetype);

					if (jid == null)
						return;
					final FileTransferModule ftModule = jaxmpp.getModule(FileTransferModule.class);
					final ContentResolver cr = activity.getContentResolver();
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
