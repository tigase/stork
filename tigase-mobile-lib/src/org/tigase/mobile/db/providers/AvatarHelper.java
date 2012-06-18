package org.tigase.mobile.db.providers;

import org.tigase.mobile.db.RosterTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

public class AvatarHelper {

	private static LruCache<BareJID,Bitmap> avatarCache;	
	
	public static void initilize(Context context) {
		if (avatarCache == null) {			
		    // Get memory class of this device, exceeding this amount will throw an
		    // OutOfMemory exception.
		    final int memClass = ((android.app.ActivityManager) context.getSystemService(
		            Context.ACTIVITY_SERVICE)).getMemoryClass();

		    // Use 1/8th of the available memory for this memory cache.
		    final int cacheSize = 1024 * 1024 * memClass / 8;

		    avatarCache = new LruCache<BareJID,Bitmap>(cacheSize) {
		        @Override
		        protected int sizeOf(BareJID key, Bitmap bitmap) {
		            // The cache size will be measured in bytes rather than number of items.
		            return bitmap.getByteCount();
		        }
		    };					
		}
	}
	
	public static Bitmap getAvatar(BareJID jid, Cursor cursor, String fieldName) {
		Bitmap bmp = avatarCache.get(jid);
		if (bmp == null) {
			byte[] avatar = cursor.getBlob(cursor.getColumnIndex(fieldName));
			if (avatar != null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeByteArray(avatar, 0, avatar.length, options);
				options.inSampleSize = calculateSize(options, 64, 64);			
				options.inJustDecodeBounds = false;
				bmp = BitmapFactory.decodeByteArray(avatar, 0, avatar.length, options);
				if (bmp != null) {
					avatarCache.put(jid, bmp);					
				}
			}
		}
		return bmp;
	}
	
	public static void clearAvatar(BareJID jid) {
		avatarCache.remove(jid);
	}

	private static int calculateSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;

	    if (height > reqHeight || width > reqWidth) {
	        if (width > height) {
	            inSampleSize = Math.round((float)height / (float)reqHeight);
	        } else {
	            inSampleSize = Math.round((float)width / (float)reqWidth);
	        }
	    }
	    return inSampleSize;		
	}	
}
