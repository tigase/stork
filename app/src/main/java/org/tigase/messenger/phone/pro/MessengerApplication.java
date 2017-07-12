/*
 * MessengerApplication.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

import android.app.Application;
import android.content.Intent;
import org.tigase.messenger.phone.pro.service.ServiceRestarter;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import static org.tigase.messenger.phone.pro.service.XMPPService.CONNECT_ALL;

/**
 * Created by bmalkow on 16.03.16.
 */
public class MessengerApplication
		extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		AvatarHelper.initilize(this);

		Intent ssIntent = new Intent(this, XMPPService.class);
		ssIntent.setAction(CONNECT_ALL);
		startService(ssIntent);
	}

	@Override
	public void onTerminate() {
		sendBroadcast(new Intent(ServiceRestarter.ACTION_NAME));
		super.onTerminate();
	}
}
