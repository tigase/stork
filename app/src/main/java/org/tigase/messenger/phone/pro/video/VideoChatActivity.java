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

package org.tigase.messenger.phone.pro.video;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.tigase.jaxmpp.modules.jingle.JingleModule;
import org.tigase.jaxmpp.modules.jingle.JingleSession;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.video.component.AVComponent;
import org.tigase.messenger.phone.pro.video.ringer.Ringer;
import org.tigase.messenger.phone.pro.video.ringer.RingerIncomming;
import org.tigase.messenger.phone.pro.video.ringer.RingerOutgoing;
import org.webrtc.EglBase;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;

import java.util.Arrays;

public class VideoChatActivity
		extends AbstractServiceActivity {

	public static final String SID_KEY = "sid";
	public static final String INITIATOR_KEY = "initiator";
	public static final String JID_KEY = "jid";
	public static final String ACCOUNT_KEY = "account";
	private static final String TAG = "VideoChatActivity";
	private AVComponent avComponent;
	private WebRTCClient client;
	private EglBase eglBase;
	private final Handler handler = new Handler();
	private JaxmppCore jaxmpp;
	private boolean permissionsGranted = false;
	private Ringer ringer;
	private boolean xmppServiceConnected = false;

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		Log.i(TAG, "permissions: " + Arrays.toString(permissions));
		Log.i(TAG, "grantResults: " + Arrays.toString(grantResults));

		permissionsGranted = (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
				grantResults[1] == PackageManager.PERMISSION_GRANTED);

		if (xmppServiceConnected) {
			initializeClient(true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.eglBase = EglBase.create();

		startWhenBinded(() -> initializeClient(false));

		// Set default WebRTC tracing and INFO libjingle logging.
		// NOTE: this _must_ happen while |factory| is alive!

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
				PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
						PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
																 Manifest.permission.CAMERA}, 123);
		} else {
			this.permissionsGranted = true;
		}
	}

	@Override
	protected void onXMPPServiceConnected() {
		final String account = getIntent().getStringExtra(ACCOUNT_KEY);
		Log.i(TAG, "XMPP Service Connected. Getting jaxmpp for " + account);
		this.jaxmpp = getJaxmpp(account);
		this.xmppServiceConnected = true;

		final boolean initiator = getIntent().getBooleanExtra(INITIATOR_KEY, false);

		if (initiator) {
			this.ringer = new RingerOutgoing(this);
		} else {
			this.ringer = new RingerIncomming(this);
		}

		JID jid = JID.jidInstance(getIntent().getStringExtra(JID_KEY));

		this.client = new WebRTCClient(this, eglBase, handler, jaxmpp, jid, initiator);
		this.client.addIceServers("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302",
								  "stun:stun2.l.google.com:19302", "stun:stun3.l.google.com:19302",
								  "stun:stun4.l.google.com:19302", "stun:stunserver.org:3478");
		this.client.setRemoteHangupHandler(this::hangup);
	}

	@Override
	protected void onXMPPServiceDisconnected() {
	}

	private void initializeClient(boolean permissionsRequested) {
		if (permissionsRequested && !this.permissionsGranted) {
			hangup();
		} else if (!(this.xmppServiceConnected && this.permissionsGranted)) {
//			finish();
			return;
		}

		setContentView(R.layout.activity_video_chat);
		avComponent = findViewById(R.id.avcomponent);
		avComponent.setAccount(getIntent().getStringExtra(ACCOUNT_KEY));
		avComponent.setHangupHandler(this::hangup);

		avComponent.initVideos(eglBase);

		if (this.jaxmpp == null) {
			throw new RuntimeException("Cannot get XMPP Client.");
		}

		boolean initiator = getIntent().getBooleanExtra(INITIATOR_KEY, false);
		avComponent.setRemoteJid(this.client.getJid());

		this.client.setRemoteVideoSinkHandler(() -> avComponent.getRemoteVideoView());
		this.client.setLocalVideoSinkHandler(() -> avComponent.getLocalVideoView());
		this.client.setRemoteVideoVisibleHandler(visible -> {
			ringer.stop();
			avComponent.setRemoteVideoViewVisible(visible);
			avComponent.updateVideoViews(visible);
		});

		this.client.initialize();

		if (initiator) {
			this.ringer.start();
			this.client.startCalling();
		} else {
			String sid = getIntent().getStringExtra(SID_KEY);
			JingleSession session = JingleModule.getSession(jaxmpp.getSessionObject(), sid, this.client.getJid());
			this.client.setSession(session);
			ringer.start();
			avComponent.askForPickup(this::handlePickupResult);
		}
	}

	private void handlePickupResult(boolean pickedUp) {
		if (pickedUp) {
			client.acceptIncommingCall();
		} else {
			hangup();
		}
	}

	private void hangup() {
		try {
			this.ringer.stop();
			if (client != null) {
				client.close();

			}
			avComponent.stop();
			avComponent.updateVideoViews(false);
		} catch (Exception e) {
			Log.e(TAG, "Hangup problem", e);
			e.printStackTrace();
		} finally {
			finish();
		}
	}

}
