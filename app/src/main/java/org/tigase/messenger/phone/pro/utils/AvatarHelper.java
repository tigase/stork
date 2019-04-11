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
package org.tigase.messenger.phone.pro.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import tigase.jaxmpp.core.client.BareJID;

//import org.tigase.messenger.phone.pro.sync.SyncAdapter;

//import tigase.jaxmpp.R;

public class AvatarHelper
		extends ImageHelper {

	// private static LruCache<BareJID, Bitmap> avatarCache;

	private static final String TAG = "AvatarHelper";
	public static Bitmap mPlaceHolderBitmap;
	private static Context context;
	private static int defaultAvatarSize = 50;

	public static int calculateSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		int scale = 1;
		while (inSampleSize > scale) {
			scale *= 2;
		}
		// is it needed?
		// if (inSampleSize < scale) {
		// scale /= 2;
		// }
		return scale;
		// return inSampleSize;
	}

	public static boolean cancelPotentialWork(BareJID jid, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final BareJID bitmapData = bitmapWorkerTask.data;
			if (!jid.equals(bitmapData)) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was
		// cancelled
		return true;
	}

	public static void clearAvatar(BareJID jid) {
		memCache.remove(jid.toString());
	}

	public static Bitmap cropToSquare(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int newWidth = (height > width) ? width : height;
		int newHeight = (height > width) ? height - (height - width) : height;
		int cropW = (width - height) / 2;
		cropW = (cropW < 0) ? 0 : cropW;
		int cropH = (height - width) / 2;
		cropH = (cropH < 0) ? 0 : cropH;
		Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

		return cropImg;
	}

	public static Bitmap getAvatar(BareJID jid) {
		return getAvatar(context, jid, false);
	}

	public static Bitmap getAvatar(Context context, BareJID jid, boolean noCache) {
		Bitmap bmp = noCache ? null : memCache.get(jid.toString());
		if (bmp == null) {
			bmp = loadAvatar(context, jid, noCache);
		}
		return bmp;
	}

	protected static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	public static Bitmap getCroppedBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		bitmap = cropToSquare(bitmap);

		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		// canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
//		Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
//		return _bmp;
		return output;
	}

	public static void initilize(Context context_) {
		if (mPlaceHolderBitmap == null) {
			context = context_;

			density = context.getResources().getDisplayMetrics().density;
			int defaultAvatarSizeRoster = Math.round(density * 50);
			int defaultAvatarSizeForChat = context.getResources()
					.getDimensionPixelSize(R.dimen.chat_item_layout_item_avatar_size);
			defaultAvatarSize = Math.max(defaultAvatarSizeRoster, defaultAvatarSizeForChat);

			ImageHelper.initialize(context_);
			// // Get memory class of this device, exceeding this amount will
			// throw
			// // an
			// // OutOfMemory exception.
			// final int memClass = ((android.app.ActivityManager)
			// context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
			//
			// // Use 1/8th of the available memory for this memory cache.
			// final int cacheSize = 1024 * 1024 * memClass / 8;
			//
			// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
			// avatarCache = new LruCache<BareJID, Bitmap>(cacheSize) {
			// @TargetApi(12)
			// @Override
			// protected int sizeOf(BareJID key, Bitmap bitmap) {
			// // The cache size will be measured in bytes rather than
			// // number of items. Ignoring placeholder bitmap as well.
			// return bitmap == mPlaceHolderBitmap ? 0 : bitmap.getByteCount();
			// }
			// };
			// } else {
			// // below SDK 12 there is no getByteCount method
			// avatarCache = new LruCache<BareJID, Bitmap>(cacheSize) {
			// @Override
			// protected int sizeOf(BareJID key, Bitmap bitmap) {
			// // The cache size will be measured in bytes rather than
			// // number of items. Ignoring placeholder bitmap as well.
			// return bitmap == mPlaceHolderBitmap ? 0 : (bitmap.getRowBytes() *
			// bitmap.getHeight());
			// }
			// };
			// }

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(context.getResources(), R.drawable.user_avatar, options);
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inSampleSize = calculateSize(options, defaultAvatarSize, defaultAvatarSize);
			options.inJustDecodeBounds = false;
			mPlaceHolderBitmap = getCroppedBitmap(
					BitmapFactory.decodeResource(context.getResources(), R.drawable.user_avatar, options));
			ImageHelper.addPlaceHolder(mPlaceHolderBitmap);
		}
	}

	protected static Bitmap loadAvatar(Context context, BareJID jid, boolean noCache) {
		if (mPlaceHolderBitmap == null) {
			return null;
		}
		Bitmap bmp = loadAvatar(context, jid, defaultAvatarSize);
		if (!noCache) {
			if (bmp == null) {
				memCache.put(jid.toString(), mPlaceHolderBitmap);
			} else {
				memCache.put(jid.toString(), bmp);
			}
		}
		return bmp;
	}

	protected static Bitmap loadAvatar(Context context, BareJID jid, int size) {
		Bitmap bmp = null;

		Log.v(TAG, "loading avatar with size " + size);

		Cursor cursor = context.getContentResolver()
				.query(Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.toString())), null, null, null, null);
		try {
			if (cursor.moveToNext()) {
				// we found avatar in our store
				byte[] avatar = cursor.getBlob(cursor.getColumnIndex(DatabaseContract.VCardsCache.FIELD_DATA));
				if (avatar != null) {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					Bitmap bmp1 = BitmapFactory.decodeByteArray(avatar, 0, avatar.length, options);
					if (bmp1 != null) {
						bmp1.recycle();
					}
					// options.inSampleSize = calculateSize(options, 96, 96);
					options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					options.inSampleSize = calculateSize(options, size, size);
					options.inJustDecodeBounds = false;
					bmp = BitmapFactory.decodeByteArray(avatar, 0, avatar.length, options);
				}
			} else {
				// no avatar in our store - checking for avatar in Android
				// contacts
				// Uri photoUri =
				// SyncAdapter.getAvatarUriFromContacts(context.getContentResolver(),
				// jid);
				// if (photoUri != null) {
				// InputStream input =
				// ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
				// photoUri);
				// if (input != null) {
				// BitmapFactory.Options options = new BitmapFactory.Options();
				// options.inJustDecodeBounds = true;
				// Bitmap bmp1 = BitmapFactory.decodeStream(input, null,
				// options);
				// if (bmp1 != null) {
				// bmp1.recycle();
				// }
				// // options.inSampleSize = calculateSize(options, 96,
				// // 96);
				// input.close();
				// input =
				// ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
				// photoUri);
				// options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				// options.inSampleSize = calculateSize(options, size, size);
				// options.inJustDecodeBounds = false;
				// bmp = BitmapFactory.decodeStream(input, null, options);
				// input.close();
				// }
				// }
			}
		} catch (Exception ex) {
			Log.v(TAG, "exception retrieving avatar for " + jid.toString(), ex);
		} finally {
			cursor.close();
		}

		return bmp == null ? null : getCroppedBitmap(bmp);
	}

	public static void setAvatarToImageView(BareJID jid, ImageView imageView) {
		Bitmap bmp = memCache.get(jid.toString());
		if (bmp != null) {
			imageView.setImageBitmap(bmp);
			return;
		}

		if (cancelPotentialWork(jid, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(context, imageView, null);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(), mPlaceHolderBitmap, task);
			imageView.setImageDrawable(asyncDrawable);
			try {
				task.execute(jid);
			} catch (java.util.concurrent.RejectedExecutionException e) {
				// ignoring: probably avatar big as cow
				Log.e(TAG, "loading avatar failed for " + jid.toString(), e);
			}
		}
	}

	public static void setAvatarToImageView(BareJID jid, ImageView imageView, int size) {
		if (cancelPotentialWork(jid, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(context, imageView, (int) Math.ceil(size * density));
			final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(), mPlaceHolderBitmap, task);
			imageView.setImageDrawable(asyncDrawable);
			try {
				task.execute(jid);
			} catch (java.util.concurrent.RejectedExecutionException e) {
				// ignoring: probably avatar big as cow
				Log.e(TAG, "loading avatar failed for " + jid.toString(), e);
			}
		}
	}

}
