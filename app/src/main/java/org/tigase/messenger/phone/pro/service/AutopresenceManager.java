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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.CPresence;

import java.util.Timer;
import java.util.TimerTask;

public class AutopresenceManager {

	private final String TAG = "Autopresence";
	private final XMPPService service;
	private final Timer tm = new Timer("AutopresenceTimer");
	private TimerTask currentPendingIntent;

	AutopresenceManager(XMPPService xmppService) {
		this.service = xmppService;
	}

	public void update(final long presenceId) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(service);
		final long xaAfterSecs = Long.parseLong(sharedPref.getString("xa_delay_seconds", "90"));

		Log.d(TAG, "Received autopresence update: " + presenceId);

		if (presenceId == -1) {
			return;
		}

		setAutoPresenceValue(presenceId);
		service.processPresenceUpdate();

		if (xaAfterSecs > 0 && presenceId == CPresence.AWAY) {
			Log.d(TAG, "Starting new timer for XA");

			start(CPresence.XA, 1000 * xaAfterSecs);
		}
	}

	void start() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(service);
		final long awayAfterSecs = Long.parseLong(sharedPref.getString("away_delay_seconds", "60"));
		final int defaultPresence = Long.valueOf(sharedPref.getLong("presence", CPresence.ONLINE)).intValue();

		Log.d(TAG, "Trying to start autopresence: dp=" + defaultPresence + "; delay=" + awayAfterSecs);

		if (awayAfterSecs >= 0 && defaultPresence == CPresence.ONLINE) {
			start(CPresence.AWAY, 1000 * awayAfterSecs);
		} else if (this.currentPendingIntent != null) {
			Log.d(TAG, "Canceling previous timer");
			currentPendingIntent.cancel();
		}
	}

	synchronized void stop() {
		if (this.currentPendingIntent != null) {
			Log.d(TAG, "Stopping autopresence timer");

			TimerTask tmp = this.currentPendingIntent;
			this.currentPendingIntent = null;
			tmp.cancel();
		}

		final long presenceId = getAutoPresenceValue();
		if (presenceId != -1) {
			Log.d(TAG, "Reseting current autopresence status");

			setAutoPresenceValue(null);
			service.processPresenceUpdate();
		}
	}

	private long getAutoPresenceValue() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(service);
		return sharedPref.getLong("auto_presence", -1);
	}

	private void setAutoPresenceValue(Long presenceId) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(service);
		SharedPreferences.Editor editor = sharedPref.edit();
		Log.d(TAG, "New auto_presence value: " + presenceId);

		if (presenceId == null) {
			editor.remove("auto_presence");
		} else {
			editor.putLong("auto_presence", presenceId);
		}
		editor.commit();
	}

	private synchronized void start(final long presenceId, long delayInMilis) {
		TimerTask pi = new TimerTask() {
			@Override
			public void run() {
				update(presenceId);
			}
		};

		if (this.currentPendingIntent != null) {
			Log.d(TAG, "Canceling previous timer");
			currentPendingIntent.cancel();
		}

		this.currentPendingIntent = pi;
		Log.d(TAG, "Starting new autopresence timer pId=" + presenceId + "; delay=" + delayInMilis);
		tm.schedule(pi, delayInMilis);
	}
}
