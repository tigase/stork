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

import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import org.tigase.messenger.phone.pro.service.ServiceRestarter;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

/**
 * Created by bmalkow on 16.03.16.
 */
public class MessengerApplication
		extends MultiDexApplication {

	@Override
	public void onCreate() {
		super.onCreate();
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		AvatarHelper.initilize(this);
	}

	@Override
	public void onTerminate() {
		sendBroadcast(new Intent(ServiceRestarter.ACTION_NAME));
		super.onTerminate();
	}
}
