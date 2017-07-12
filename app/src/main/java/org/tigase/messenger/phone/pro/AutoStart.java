package org.tigase.messenger.phone.pro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart
		extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.i("AutoStart", "Starting service " + MyFirebaseMessagingService.class.getName());
			Intent ssIntent = new Intent(context, MyFirebaseMessagingService.class);
			ssIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(ssIntent);
		}
	}
}
