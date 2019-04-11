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
import android.provider.MediaStore;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FileDownloaderTask
		extends AsyncTask<Uri, Integer, Bitmap> {

	private final static String TAG = "OOBDownloader";
	private final Context context;
	private final String fileUrl;
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
	protected Bitmap doInBackground(Uri... chatItems) {
		this.chatHistoryUri = chatItems[0];
		Log.i(TAG, "Downloading file from " + this.fileUrl);
		try {
			URL u = new URL(fileUrl);
			try (InputStream in = u.openStream()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				return BitmapFactory.decodeStream(in);
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
	protected void onPostExecute(Bitmap bitmap) {
		if (bitmap == null) {
			Log.w(TAG, "File not found on server. Nothing to add");
			return;
		}
		Log.i(TAG, "Saving image in MediaStore");
		final ContentValues values = new ContentValues();
		String uri = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "", "");

		values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, uri);
		context.getContentResolver().update(chatHistoryUri, values, null, null);

		Log.d(TAG, "ChatItem " + chatHistoryUri + " updated with internal_content_uri " + uri);

		context.getContentResolver().notifyChange(chatHistoryUri, null);
	}
}
