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

package org.tigase.messenger.phone.pro.settings;

import android.content.Context;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.eventbus.DefaultEventBus;
import tigase.jaxmpp.core.client.eventbus.EventBus;
import tigase.jaxmpp.core.client.eventbus.EventListener;

public class SettingsActivity
		extends AbstractServicePreferencesActivity {

	private final static String TAG = "SettingsActivity";
	private final static String SCREEN_TITLE = "SCREEN_TITLE";
	private final DefaultEventBus localEventBus = new DefaultEventBus();
	private final EventListener eventsRepeater = SettingsActivity.this.localEventBus::fire;

	public static String getDisconnectedCauseMessage(Context c, final XMPPService.DisconnectionCauses cause) {
		if (cause == XMPPService.DisconnectionCauses.AUTHENTICATION) {
			return "Disconnected. Invalid username or password.";
		} else if (cause == XMPPService.DisconnectionCauses.CERTIFICATE_ERROR) {
			return "Disconnected. Invalid certificate.";
		} else {
			return c.getString(R.string.account_status_disconnected);
		}
	}

	public EventBus getEventBus() {
		return this.localEventBus;
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		mConnection.getService().getMultiJaxmpp().addListener(eventsRepeater);
	}

	@Override
	protected void onServiceConnectionDestroy() {
		mConnection.getService().getMultiJaxmpp().remove(eventsRepeater);
		super.onServiceConnectionDestroy();
	}

	@Override
	protected PreferenceFragmentCompat getDefaultFragment() {
		return new MainPreferenceFragment();
	}

}
