/*
 * Tigase Android Messenger
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

package org.tigase.messenger.phone.pro.video.ringer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;

public class RingerIncomming
		implements Ringer {

	public static final String TAG = "RingerOutgoing";

	protected final Context context;

	protected MediaPlayer mediaPlayer;

	public static Uri getCallNotificationRingtone(Context context) {
		String result = Settings.System.DEFAULT_RINGTONE_URI.toString();
		if (result != null && result.startsWith("file:")) {
			result = Settings.System.DEFAULT_RINGTONE_URI.toString();
		}
		return Uri.parse(result);
	}

	public RingerIncomming(Context context) {
		this.context = context;
	}

	public void start() {
		mediaPlayer = new MediaPlayer();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			initMediaPlayer_Lollipop();
		} else {
			initMediaPlayer();
		}

		try {
			Uri dataUri = getCallNotificationRingtone(context);
			mediaPlayer.setDataSource(context, dataUri);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
			Log.w(TAG, e);
		}
	}

	public void stop() {
		if (mediaPlayer == null) {
			return;
		}
		mediaPlayer.release();
		mediaPlayer = null;
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private void initMediaPlayer_Lollipop() {
		AudioAttributes attributes = (new AudioAttributes.Builder()).setUsage(
				AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build();
		mediaPlayer.setAudioAttributes(attributes);
		mediaPlayer.setLooping(true);
	}

	private void initMediaPlayer() {
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
		mediaPlayer.setLooping(true);

	}

}
