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
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public abstract class DiskLruCache<T> {

	private final Object cacheLock = new Object();
	private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>(0, 0.75f, true);
	private final ExecutorService executorService = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
																		   new LinkedBlockingQueue<Runnable>());
	private File cacheDir;
	private int maxSize;
	private int size;
	private final Callable<Void> cleanupCallable = new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			synchronized (cacheLock) {
				trimToSize();
			}
			return null;
		}
	};
	private String type;

	// Creates a unique subdirectory of the designated app cache directory.
	// Tries to use external
	// but if not mounted, falls back on internal storage.
	public static File getDiskCacheDir(Context context, String uniqueName) {
		// Check if media is mounted or storage is built-in, if so, try and use
		// external cache dir
		// otherwise use internal cache dir
		File cache = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
				!Environment.isExternalStorageRemovable()) {
			cache = getExternalCacheDir(context);
		}
		if (cache == null) {
			cache = context.getCacheDir();
		}

		return new File(cache, uniqueName);
	}

	public static File getExternalCacheDir(Context context) {
		return context.getExternalCacheDir();
	}

	public DiskLruCache() {
	}

	public T get(String key) {
		Entry e;
		synchronized (cacheLock) {
			e = entries.get(key);
			if (e == null) {
				return null;
			}
			entries.put(key, e);
			return decode(e);
		}
	}

	public void initialize(Context context, String type, int size) {
		synchronized (cacheLock) {
			if (cacheDir != null) {
				return;
			}
			this.type = type;
			cacheDir = getDiskCacheDir(context, type);
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			(new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					synchronized (cacheLock) {
						File[] files = cacheDir.listFiles();
						if (files == null) {
							Log.w("DiskLruCache", "File list is null! Why???");
							return null;
						}

						Arrays.sort(files, new Comparator<File>() {
							public int compare(File f1, File f2) {
								return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
							}
						});
						for (File f : files) {
							Entry e = new Entry(f.getName());
							e.file = f;
							entries.put(f.getName(), e);
						}
					}

					return null;
				}
			}).execute();
		}
	}

	public void put(String key, T value) {
		synchronized (cacheLock) {
			Entry e = entries.get(key);
			if (e == null) {
				e = new Entry(key);
			}
			e.file = new File(cacheDir, key);
			encode(e, value);
			entries.put(key, e);
			size += e.size;
		}
		executorService.submit(cleanupCallable);
	}

	public void remove(String key) {
		synchronized (cacheLock) {
			Entry e = entries.remove(key);
			if (e == null) {
				return;
			}
			size -= e.size;
		}
	}

	protected abstract T decode(Entry e);

	protected abstract void encode(Entry e, T value);

	private void trimToSize() {
		while (size > maxSize) {
			// Map.Entry<String, Entry> toEvict = lruEntries.eldest();
			final Map.Entry<String, Entry> toEvict = entries.entrySet().iterator().next();
			remove(toEvict.getKey());
		}
	}

	public class Entry {

		protected File file = null;
		private String key;
		private int size = 0;

		public Entry(String key) {
			this.key = key;
		}

	}
}
