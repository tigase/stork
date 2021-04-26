/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.R;

public class NotificationPreferenceFragment
		extends PreferenceFragmentCompat {

	private final static int GROUP_MESSAGES_KEY = 1;
	private final static int NEW_MESSAGES_KEY = 2;

	private final static String PREF_NEW_MESSAGES = "notifications_new_message_ringtone";
	private final static String PREF_GROUPCHAT_MESSAGES = "notifications_new_groupmessage_ringtone";

	@Override
	public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
		setPreferencesFromResource(R.xml.pref_notification, rootKey);

		updateRingtoneSelectorSummary(PREF_NEW_MESSAGES);
		updateRingtoneSelectorSummary(PREF_GROUPCHAT_MESSAGES);

		addRingtoneSelectorToPreference(PREF_NEW_MESSAGES, NEW_MESSAGES_KEY);
		addRingtoneSelectorToPreference(PREF_GROUPCHAT_MESSAGES, GROUP_MESSAGES_KEY);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == NEW_MESSAGES_KEY && resultCode == Activity.RESULT_OK) {
			update(PREF_NEW_MESSAGES, data);
		} else if (requestCode == GROUP_MESSAGES_KEY && resultCode == Activity.RESULT_OK) {
			update(PREF_GROUPCHAT_MESSAGES, data);
		}
	}

	private void updateRingtoneSelectorSummary(final String prefKey) {
		Preference newMessRingtone = findPreference(prefKey);
		String sound = getPreferenceManager().getSharedPreferences().getString(prefKey, null);
		final Uri currentRingtone =
				sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(sound);
		if (newMessRingtone != null) {
			newMessRingtone.setSummary(
					RingtoneManager.getRingtone(getContext(), currentRingtone).getTitle(getContext()));
		}
	}

	private void update(final String prefKey, @Nullable final Intent data) {
		if (data == null) {
			return;
		}
		final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

		getPreferenceManager().getSharedPreferences().edit().putString(prefKey, uri.toString()).apply();
		Preference newMessRingtone = findPreference(prefKey);
		if (newMessRingtone != null) {
			newMessRingtone.setSummary(RingtoneManager.getRingtone(getContext(), uri).getTitle(getContext()));
		}
	}

	private void addRingtoneSelectorToPreference(final String prefKey, final int returnKey) {
		Preference newMessRingtone = findPreference(prefKey);
		if (newMessRingtone != null) {
			newMessRingtone.setOnPreferenceClickListener(preference -> {
				String sound = getPreferenceManager().getSharedPreferences().getString(prefKey, null);
				final Uri currentRingtone = sound == null
											? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
											: Uri.parse(sound);
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtone);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
				startActivityForResult(intent, returnKey);
				return true;
			});
		}
	}
}