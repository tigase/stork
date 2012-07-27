package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.MultiJaxmpp.ChatWrapper;

import android.content.res.Configuration;
import android.os.Build;
import android.view.MenuItem;

public class TigaseMobileMessengerActivityHelper {

	public static TigaseMobileMessengerActivityHelper createInstance(TigaseMobileMessengerActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new TigaseMobileMessengerActivityHelperHoneycomb(activity);
		} else {
			return new TigaseMobileMessengerActivityHelper(activity);
		}
	}

	protected final TigaseMobileMessengerActivity activity;

	protected TigaseMobileMessengerActivityHelper(TigaseMobileMessengerActivity activity) {
		this.activity = activity;
	}

	public void invalidateOptionsMenu() {
	}

	public boolean isXLarge() {
		return (activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4;
		// return getResources().getConfiguration().screenLayout >= 0x04 &&
		// Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	};

	public void setShowAsAction(MenuItem item, int value) {
	};

	public void updateActionBar(int itemHashCode) {
	}
	
	public void updateActionBar() {
	}

}
