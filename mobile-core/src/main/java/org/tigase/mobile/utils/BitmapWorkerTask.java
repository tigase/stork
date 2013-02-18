package org.tigase.mobile.utils;

import java.lang.ref.WeakReference;

import tigase.jaxmpp.core.client.BareJID;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapWorkerTask extends AsyncTask<BareJID, Void, Bitmap> {
	protected BareJID data = null;
	private final WeakReference<ImageView> imageViewReference;

	public BitmapWorkerTask(ImageView imageView) {
		// Use a WeakReference to ensure the ImageView can be garbage collected
		imageViewReference = new WeakReference<ImageView>(imageView);
	}

	// Decode image in background.
	@Override
	protected Bitmap doInBackground(BareJID... params) {
		data = params[0];
		return AvatarHelper.loadAvatar(data);
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
