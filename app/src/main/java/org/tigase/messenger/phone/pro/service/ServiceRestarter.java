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

package org.tigase.messenger.phone.pro.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.CPresence;

public class ServiceRestarter
		extends BroadcastReceiver {

	public static final String ACTION_NAME = "org.tigase.messenger.phone.pro.XMPP_SERVICE_DESTROYED";

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		long presenceId = sharedPref.getLong("presence", CPresence.OFFLINE);

		if (presenceId != CPresence.OFFLINE) {
			Log.i("ServiceRestarter", "Starting service!");
			Intent ssIntent = new Intent(context, XMPPService.class);
			ssIntent.setAction(XMPPService.CONNECT_ALL);
			ssIntent.putExtra("destroyed", true);
			context.startService(ssIntent);
		} else {
			Log.i("ServiceRestarter", "Service not started, because presence is set to OFFLINE");
		}
	}
}
