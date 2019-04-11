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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class implements generic image in-memory caching feature to ensure that we
 * have one memory cache for all images (to reduce memory usage and protect
 * against OutOfMemory).
 *
 * @author andrzej
 */
public class ImageHelper {

	private static final String TAG = "ImageHelper";

	public static float density = 1;
	protected static LruCache<String, Bitmap> memCache = null;
	private static ConcurrentHashMap<String, BitmapDiskLruCache> diskCaches = new ConcurrentHashMap<String, BitmapDiskLruCache>();
	private static Set<Bitmap> placeHolders = new HashSet<Bitmap>();

	protected static void addPlaceHolder(Bitmap placeHolder) {
		placeHolders.add(placeHolder);
	}

	private static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	public static Bitmap get(String type, String key) {
		Bitmap value = memCache.get(key);
		if (value == null) {
			BitmapDiskLruCache diskCache = diskCaches.get(type);
			if (diskCache != null) {
				value = diskCache.get(key);
			}
		}
		return value;
	}

	public static String hashKey(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	protected static void initialize(Context context) {
		if (memCache == null) {
			// Get memory class of this device, exceeding this amount will throw
			// an OutOfMemory exception.
			final int memClass = ((android.app.ActivityManager) context.getSystemService(
					Context.ACTIVITY_SERVICE)).getMemoryClass();

			// Use 1/8th of the available memory for this memory cache.
			final int cacheSize = 1024 * 1024 * memClass / 8;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
				memCache = new LruCache<String, Bitmap>(cacheSize) {
					@TargetApi(12)
					@Override
					protected int sizeOf(String key, Bitmap bitmap) {
						// The cache size will be measured in bytes rather than
						// number of items. Ignoring placeholder bitmap as well.
						return placeHolders.contains(bitmap) ? 0 : bitmap.getByteCount();
					}
				};
			} else {
				// below SDK 12 there is no getByteCount method
				memCache = new LruCache<String, Bitmap>(cacheSize) {
					@Override
					protected int sizeOf(String key, Bitmap bitmap) {
						// The cache size will be measured in bytes rather than
						// number of items. Ignoring placeholder bitmap as well.
						return placeHolders.contains(bitmap) ? 0 : (bitmap.getRowBytes() * bitmap.getHeight());
					}
				};
			}

			// maps images cache
			BitmapDiskLruCache diskCache = new BitmapDiskLruCache();
			diskCache.initialize(context, "maps", 10 * 1024 * 1024);
			diskCaches.put("maps", diskCache);

			// images from files shared with or by us
			diskCache = new BitmapDiskLruCache();
			diskCache.initialize(context, "images-mini", 10 * 1024 * 1024);
			diskCaches.put("images-mini", diskCache);
		}
	}

	@SuppressLint("NewApi")
	public static void onTrimMemory(int level) {
		int count = 0;
		if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
			count = 10;
			if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
				count = 0;
			}
		} else {
			return;
		}

		int size = 0;
		if (count > 0) {
			for (Bitmap b : placeHolders) {
				size += b.getByteCount();
			}
		}

		int trimSize = placeHolders.size() == 0 ? 0 : ((count * size) / placeHolders.size());
		Log.v(TAG,
			  "trim image cache from " + memCache.size() + " to " + trimSize + " to reduce memory usage, max size " +
					  memCache.maxSize());
		memCache.trimToSize(trimSize);
	}

	public static void put(String type, String key, Bitmap bitmap) {
		memCache.put(key, bitmap);
		BitmapDiskLruCache diskCache = diskCaches.get(type);
		if (diskCache != null) {
			diskCache.put(key, bitmap);
		}
	}

	public static class BitmapDiskLruCache
			extends DiskLruCache<Bitmap> {

		@Override
		protected Bitmap decode(org.tigase.messenger.phone.pro.utils.DiskLruCache.Entry e) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(e.file);
				return BitmapFactory.decodeFileDescriptor(fis.getFD());
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}

		@Override
		protected void encode(org.tigase.messenger.phone.pro.utils.DiskLruCache.Entry e, Bitmap value) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(e.file);
				value.compress(CompressFormat.PNG, 100, fos);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}

	}
}
