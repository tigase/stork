package org.tigase.messenger.phone.pro.video.ringer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import org.tigase.messenger.phone.pro.R;

import java.io.IOException;

public class RingerOutgoing
		implements Ringer {

	public static final String TAG = "RingerOutgoing";

	protected final Context context;

	protected MediaPlayer mediaPlayer;

	public RingerOutgoing(Context context) {
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
			Uri dataUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.ring_outgoing);
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
				AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING).build();
		mediaPlayer.setAudioAttributes(attributes);
		mediaPlayer.setLooping(true);
	}

	private void initMediaPlayer() {
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
		mediaPlayer.setLooping(true);

	}

}
