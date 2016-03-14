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
package org.tigase.messenger.phone.pro.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import tigase.jaxmpp.core.client.BareJID;

public class BitmapWorkerTask extends AsyncTask<BareJID, Void, Bitmap> {

	private final Context context;
	protected BareJID data = null;
	private final WeakReference<ImageView> imageViewReference;
	private final Integer size;

	public BitmapWorkerTask(Context context, ImageView imageView, Integer size) {
		this.context = context;
		// Use a WeakReference to ensure the ImageView can be garbage collected
		imageViewReference = new WeakReference<ImageView>(imageView);
		this.size = size;
	}

	// Decode image in background.
	@Override
	protected Bitmap doInBackground(BareJID... params) {
		data = params[0];
		return size == null ? AvatarHelper.loadAvatar(context, data, false) : AvatarHelper.loadAvatar(context, data, size);
	}

	// Once complete, see if ImageView is still around and set bitmap.
	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (isCancelled()) {
			bitmap = null;
		}

		if (imageViewReference != null && bitmap != null) {
			final ImageView imageView = imageViewReference.get();
			final BitmapWorkerTask bitmapWorkerTask = AvatarHelper.getBitmapWorkerTask(imageView);
			if (this == bitmapWorkerTask && imageView != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}
}
