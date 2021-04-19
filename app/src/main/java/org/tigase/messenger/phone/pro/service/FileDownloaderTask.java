/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
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

package org.tigase.messenger.phone.pro.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

import java.io.*;
import java.net.URL;
import java.util.UUID;

public class FileDownloaderTask
		extends AsyncTask<Uri, Integer, Uri> {

	private final static String TAG = "OOBDownloader";
	private static final int BUFFER_SIZE = 8192;
	private final Context context;
	private final String fileUrl;
	private final String folderName = "Stork Images";
	private final String mimeType;
	private Uri chatHistoryUri;

	/**
	 * Checks if content is alreday downloaded.
	 *
	 * @param context context.
	 * @param messageUri message URI to check.
	 *
	 * @return <code>true</code> if content is internally stored.
	 */
	public static boolean isContentDownloaded(Context context, Uri messageUri) {
		final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
										   DatabaseContract.ChatHistory.FIELD_DATA,
										   DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI};
		try (Cursor c = context.getContentResolver().query(messageUri, cols, null, null, null)) {
			while (c.moveToNext()) {
				String contentUri = c.getString(
						c.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));
				if (contentUri == null || contentUri.trim().isEmpty()) {
					return false;
				} else {
					try (InputStream in = context.getContentResolver().openInputStream(Uri.parse(contentUri))) {
						return true;
					} catch (IOException e) {
						return false;
					}
				}
			}
		}
		return false;
	}

	public FileDownloaderTask(Context context, String fileUrl, String mimeType) {
		this.context = context;
		this.fileUrl = fileUrl;
		this.mimeType = mimeType;
	}

	@Override
	protected Uri doInBackground(Uri... chatItems) {
		this.chatHistoryUri = chatItems[0];
		Log.i(TAG, "Downloading file from " + this.fileUrl);
		try {
			URL u = new URL(fileUrl);
			try (InputStream in = u.openStream()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				return saveImage(in);
			}
		} catch (java.io.FileNotFoundException e) {
			Log.w(TAG, "File not found on server", e);
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Cannot download image", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onPostExecute(Uri storedBitmapUri) {
		if (storedBitmapUri == null) {
			Log.w(TAG, "File not found on server. Nothing to add");
			return;
		}
		Log.i(TAG, "Saving image in MediaStore");
		final ContentValues values = new ContentValues();
		values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, storedBitmapUri.toString());
		context.getContentResolver().update(chatHistoryUri, values, null, null);
		Log.d(TAG, "ChatItem " + chatHistoryUri + " updated with internal_content_uri " + storedBitmapUri);

		context.getContentResolver().notifyChange(chatHistoryUri, null);
	}

	private Uri saveImage(InputStream bitmap) throws FileNotFoundException {
		// TODO
//		if (Build.VERSION.SDK_INT >= 29) {
//			ContentValues values = contentValues();
//			values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName);
//			values.put(MediaStore.Images.Media.IS_PENDING, true);
//			Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//			if (uri != null) {
//				try {
//					saveImageToStream(bitmap, context.getContentResolver().openOutputStream(uri));
//				} catch (IOException e) {
//					Log.e(TAG, "Cannot save image", e);
//				}
//				values.put(MediaStore.Images.Media.IS_PENDING, false);
//				context.getContentResolver().update(uri, values, null, null);
//			}
//			return uri;
//		} else {
			File directory = new File(
					Environment.getExternalStorageDirectory().toString() + File.separator + folderName);
			// getExternalStorageDirectory is deprecated in API 29

			if (!directory.exists()) {
				directory.mkdirs();
			}
			String fileName = UUID.randomUUID().toString() + "." + guessExtension();
			File file = new File(directory, fileName);
			copyInputStreamToFile(bitmap, file);
			if (file.getAbsolutePath() != null) {
				ContentValues values = contentValues();
				values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
				// .DATA is deprecated in API 29
				return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			}
			return null;
//		}
	}

	private String guessExtension() {
		switch (mimeType.toLowerCase()) {
			case "image/gif":
				return "gif";
			case "image/jpeg":
				return "jpg";
			case "image/png":
				return "png";
			case "video/avi":
				return "avi";
			case "video/x-matroska":
				return "mkv";
			case "video/mpeg":
				return "mp4";
			case "audio/mpeg3":
				return "mp3";
			case "application/pdf":
				return "pdf";
		}
		if (mimeType.toLowerCase().startsWith("audio")) {
			return "mp3";
		}
		if (mimeType.toLowerCase().startsWith("video")) {
			return "mp4";
		}
		return "bin";
	}

	private void copyInputStreamToFile(InputStream in, File file) {
		try (OutputStream out = new FileOutputStream(file)) {
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			Log.e(TAG, "Cannot save image", e);
		}
	}

	private void saveImageToStream(InputStream source, OutputStream sink) throws IOException {
		try {
			long nread = 0L;
			byte[] buf = new byte[BUFFER_SIZE];
			int n;
			while ((n = source.read(buf)) > 0) {
				sink.write(buf, 0, n);
				nread += n;
			}
		} finally {
			sink.close();
		}
	}

	private void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
		if (outputStream != null) {
			try {
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private ContentValues contentValues() {
		final ContentValues values = new ContentValues();
//		values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, uri);
		values.put(MediaStore.Images.Media.MIME_TYPE, this.mimeType);
		values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
		values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis() / 1000);

		return values;
	}
}
