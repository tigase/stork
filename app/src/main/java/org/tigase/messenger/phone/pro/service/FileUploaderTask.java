package org.tigase.messenger.phone.pro.service;

import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import org.tigase.messenger.phone.pro.R;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.httpfileupload.HttpFileUploadModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public abstract class FileUploaderTask
		extends AsyncTask<Void, Integer, Void> {

	private final static String NOTIFICATION_TAG = "FILE_SENDING_NOTIFICATION";
	private static int idCounter = 1;
	private final Uri content;
	private final Context context;
	private final Jaxmpp jaxmpp;
	private final AccountManager mAccountManager;
	private final HttpFileUploadModule module;
	private final int notificationId;
	private String displayName;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotifyManager;
	private String mimeType;
	private Long size;
	private HttpFileUploadModule.Slot slot;

	public static String guessMimeType(String filename) {
		int idx = filename.lastIndexOf(".");
		if (idx == -1) {
			return "application/octet-stream";
		}
		final String suffix = filename.substring(idx + 1).toLowerCase();
		switch (suffix) {
			case "gif":
				return "image/gif";
			case "png":
				return "image/png";
			case "jpg":
			case "jpeg":
				return "image/jpeg";
			case "avi":
				return "video/avi";
			case "mkv":
				return "video/x-matroska";
			case "mpg":
			case "mp4":
				return "video/mpeg";
			case "mp3":
				return "audio/mpeg3";
			case "ogg":
				return "audio/ogg";
			case "pdf":
				return "application/pdf";
			default:
				return "application/octet-stream";
		}
	}

	public FileUploaderTask(Context context, Jaxmpp jaxmpp, Uri content) {
		notificationId = (++idCounter);
		this.context = context;
		this.jaxmpp = jaxmpp;
		this.content = content;
		this.module = jaxmpp.getModule(HttpFileUploadModule.class);
		this.mAccountManager = AccountManager.get(context);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		try {
			this.mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			this.mBuilder = new NotificationCompat.Builder(context);
			mBuilder.setContentTitle("Sending file")
					.setContentText("Initialize")
					.setSmallIcon(R.drawable.ic_messenger_icon);

			mNotifyManager.notify(NOTIFICATION_TAG, notificationId, mBuilder.build());
			JID componentJid = findUploadComponent();

			if (componentJid == null) {

				notifyError("Cannot find File Upload Component.");
				return null;
			}

			try (Cursor cursor = context.getContentResolver().query(content, null, null, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					this.displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
					int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
					this.size = null;
					System.out.println(cursor.getString(sizeIndex));
					if (!cursor.isNull(sizeIndex)) {
						size = Long.valueOf(cursor.getString(sizeIndex));
					} else {
						try (InputStream in = context.getContentResolver().openInputStream(content)) {
							size = Long.valueOf(in.available());
						}
					}
				}
			}
			this.mimeType = guessMimeType(this.displayName);
			module.requestUploadSlot(componentJid, displayName, size.longValue(), mimeType, new HttpFileUploadModule.RequestUploadSlotHandler() {
				@Override
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
					FileUploaderTask.this.slot = null;
					notifyError(error.getType());
					synchronized (FileUploaderTask.this) {
						FileUploaderTask.this.notify();
					}
				}

				@Override
				public void onSuccess(HttpFileUploadModule.Slot slot) throws JaxmppException {
					FileUploaderTask.this.slot = slot;
					synchronized (FileUploaderTask.this) {
						FileUploaderTask.this.notify();
					}
				}

				@Override
				public void onTimeout() throws JaxmppException {
					FileUploaderTask.this.slot = null;
					notifyError("Server does not respond");
					synchronized (FileUploaderTask.this) {
						FileUploaderTask.this.notify();
					}
				}
			});

			synchronized (FileUploaderTask.this) {
				try {
					FileUploaderTask.this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (this.slot != null) {
				uploadFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private JID findUploadComponent() {
		final JID[] result = new JID[]{null};
		try {
			module.findHttpUploadComponents(
					BareJID.bareJIDInstance(jaxmpp.getSessionObject().getUserBareJid().getDomain()), results -> {
						if (!results.isEmpty()) {
							result[0] = results.keySet().iterator().next();
						}

						synchronized (FileUploaderTask.this) {
							FileUploaderTask.this.notify();
						}
					});
		} catch (JaxmppException e) {
			e.printStackTrace();
		}

		synchronized (FileUploaderTask.this) {
			try {
				FileUploaderTask.this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result[0];
	}

	private void notifyError(String message) {
		this.mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setContentTitle("File is not send").setContentText(message).setSmallIcon(R.drawable.ic_messenger_icon);

		mNotifyManager.notify(NOTIFICATION_TAG, notificationId, mBuilder.build());
	}

	private void notifySuccess() {
//		this.mBuilder = new NotificationCompat.Builder(context);
//		mBuilder.setContentTitle("File is sent").setSmallIcon(R.drawable.ic_messenger_icon);
//		mNotifyManager.notify(NOTIFICATION_TAG, notificationId, mBuilder.build());

		mNotifyManager.cancel(NOTIFICATION_TAG, notificationId);
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		Log.i("Uploader", "Procenty: " + values[0]);
		mBuilder.setContentText("Sending");
		mBuilder.setProgress(100, values[0], false);
		mNotifyManager.notify(NOTIFICATION_TAG, notificationId, mBuilder.build());
	}

	protected abstract void sendMessage(String getUri, String mimeType);

	protected void uploadFile() {
		try {
			URL url = new URL(slot.getPutUri());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", mimeType);
			conn.setRequestProperty("Content-Length", "" + size);
			if (slot.getPutHeaders() != null) {
				for (Map.Entry<String, String> e : slot.getPutHeaders().entrySet()) {
					conn.setRequestProperty(e.getKey(), e.getValue());
				}
			}

			try (OutputStream out = conn.getOutputStream()) {
				byte[] buff = new byte[2048];
				long sent = 0;
				try (InputStream inputStream = context.getContentResolver().openInputStream(content)) {
					int r;
					while ((r = inputStream.read(buff)) > 0) {
						out.write(buff, 0, r);
						sent += r;
						int p = size == 0 ? 100 : (int) ((sent * 100f) / size);
						publishProgress(p);
					}
				}
				out.flush();
			}

			if (conn.getResponseCode() == 201) {
				sendMessage(this.slot.getGetUri(), this.mimeType);
				notifySuccess();
			} else {
				notifyError("Error: " + conn.getResponseMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			notifyError("Error: " + e.getLocalizedMessage());
		}
	}
}
