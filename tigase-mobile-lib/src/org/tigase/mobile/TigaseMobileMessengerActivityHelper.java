package org.tigase.mobile;


import android.content.res.Configuration;
import android.os.Build;
import android.view.MenuItem;

public class TigaseMobileMessengerActivityHelper {

	public static TigaseMobileMessengerActivityHelper createInstance(TigaseMobileMessengerActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new TigaseMobileMessengerActivityHelperHoneycomb(activity);
		}
		else {
			return new TigaseMobileMessengerActivityHelper(activity);
		}
	}
	
	protected final TigaseMobileMessengerActivity activity;
	
	protected TigaseMobileMessengerActivityHelper(TigaseMobileMessengerActivity activity) {
		this.activity = activity;
	}
	
	public void setShowAsAction(MenuItem item, int value) { }
	
	public void invalidateOptionsMenu() { };
	
	public void updateActionBar() {};

	public boolean isXLarge() {
		return (activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4;
		// return getResources().getConfiguration().screenLayout >= 0x04 &&
		// Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}	
	
}
