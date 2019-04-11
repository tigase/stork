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
