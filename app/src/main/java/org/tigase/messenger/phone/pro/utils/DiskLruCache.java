package org.tigase.messenger.phone.pro.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class DiskLruCache<T> {

	private final ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private final Callable<Void> cleanupCallable = new Callable<Void>() {
        @Override public Void call() throws Exception {
            synchronized (cacheLock) {
                trimToSize();
            }
            return null;
        }
    };	
	
	public class Entry {
		private String key;
		protected File file = null;
		private int size = 0;
		
		public Entry(String key) {
			this.key = key;
		}

	}
		
	private final LinkedHashMap<String,Entry> entries = new LinkedHashMap<String,Entry>(0, 0.75f, true);
	private File cacheDir;
	private int size;
	private int maxSize;
	private String type;
	private final Object cacheLock = new Object();
	
	public DiskLruCache() {	
	}
	
	public void initialize(Context context, String type, int size) {
		synchronized (cacheLock) {
			if (cacheDir != null)
				return;
			this.type = type;
			cacheDir = getDiskCacheDir(context, type);
			if (!cacheDir.exists())
				cacheDir.mkdirs();
			
			new Thread() {
				public void run() {
					synchronized (cacheLock) {
						File[] files = cacheDir.listFiles();
						Arrays.sort(files, new Comparator<File>() {
							public int compare(File f1, File f2)
						    {
						        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
						    }				
						});
						for (File f : files) {
							Entry e = new Entry(f.getName());
							e.file = f;
							entries.put(f.getName(), e);
						}
					}
				}
			}.start();
		}
	}
	
	public T get(String key) {
		Entry e;
		synchronized (cacheLock) {
			e = entries.get(key);
			if (e == null)
				return null;
			entries.put(key, e);
			return decode(e);
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
			if (e == null)
				return;
			size -= e.size;
		}
	}
	
	protected abstract T decode(Entry e);
	
	protected abstract void encode(Entry e, T value);
	
	private void trimToSize() {
		while (size > maxSize) {
//          Map.Entry<String, Entry> toEvict = lruEntries.eldest();
			final Map.Entry<String, Entry> toEvict = entries.entrySet().iterator().next();
			remove(toEvict.getKey());
		}		
	}
	
	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	public static File getDiskCacheDir(Context context, String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
		File cache = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable())
			cache = getExternalCacheDir(context);
		if (cache == null) {
			cache = context.getCacheDir();
		}

	    return new File(cache, uniqueName);
	}	
	
    public static File getExternalCacheDir(Context context) {
        return context.getExternalCacheDir();
    }	
}
