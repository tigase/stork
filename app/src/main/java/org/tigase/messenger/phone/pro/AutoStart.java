package org.tigase.messenger.phone.pro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart
		extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
//		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
//
//			Log.i("AutoStart", "Starting service " + XMPPService.class.getName());
//			Intent ssIntent = new Intent(context, XMPPService.class);
//			ssIntent.setAction(CONNECT_ALL);
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//				context.startForegroundService(ssIntent);
//			} else {
//				context.startService(ssIntent);
//			}
//
//			Log.i("AutoStart", "Starting service " + MyFirebaseMessagingService.class.getName());
//			ssIntent = new Intent(context, MyFirebaseMessagingService.class);
//			ssIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//				context.startForegroundService(ssIntent);
//			} else {
//				context.startService(ssIntent);
//			}
//		}
	}
}
