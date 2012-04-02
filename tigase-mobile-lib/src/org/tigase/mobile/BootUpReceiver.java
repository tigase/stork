package org.tigase.mobile;

import org.tigase.mobile.service.JaxmppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent i) {
		SharedPreferences prefs = context.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);

		boolean autostart = prefs.getBoolean(Preferences.AUTOSTART_KEY, true);
		if (autostart) {
			Intent intent = new Intent(context, JaxmppService.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(intent);
		}
	}

}
