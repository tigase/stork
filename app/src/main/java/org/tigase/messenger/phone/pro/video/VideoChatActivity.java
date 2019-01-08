package org.tigase.messenger.phone.pro.video;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import org.tigase.jaxmpp.modules.jingle.*;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.video.component.AVComponent;
import org.webrtc.*;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesCache;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.*;

import static org.tigase.jaxmpp.modules.jingle.JingleModule.JINGLE_XMLNS;

public class VideoChatActivity
		extends AbstractServiceActivity {

	public static final String SID_KEY = "sid";
	public static final String INITIATOR_KEY = "initiator";
	public static final String JID_KEY = "jid";
	public static final String ACCOUNT_KEY = "account";
	final static SDP sdpConverter = new SDP();
	private static final String TAG = "VCA";
	final List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
	MediaConstraints audioConstraints;
	AudioSource audioSource;
	boolean gotUserMedia;
	AudioTrack localAudioTrack;
	PeerConnection localPeer;
	VideoTrack localVideoTrack;
	PeerConnectionFactory peerConnectionFactory;
	MediaConstraints sdpConstraints;
	MediaConstraints videoConstraints;
	VideoSource videoSource;
	private AVComponent avComponent;
	private final JingleModule.JingleSessionAcceptHandler acceptOfferHandler = (sessionObject, jingleSession, jingle) -> {
		onAnswerReceived(sessionObject, jingleSession, jingle);
		return true;
	};
	private boolean initiator;
	private JaxmppCore jaxmpp;
	private JingleSession session;
	private final JingleModule.JingleTransportInfoHandler jingleTransportHandler = new JingleModule.JingleTransportInfoHandler() {
		@Override
		public boolean onJingleTransportInfo(SessionObject sessionObject, JID sender, String sid, JingleContent content)
				throws JaxmppException {
			onIceCandidateReceived(session);
			return true;
		}
	};
	private VideoCapturer videoCapturerAndroid;
	private final JingleModule.JingleSessionTerminateHandler sessionTerminateHandler = (sessionObject, sender, sid) -> {
		onRemoteHangUp(sessionObject);
		return true;
	};

	private static int findSdpMLineIndex(JingleSession session, String contentName) throws XMLException {
		String[] sdp = sdpConverter.toSDP(session.getId(), session.getSid(), session.getJingleElement())
				.split(SDP.LINE);

		int p0 = -1;
		for (int i = 0; i < sdp.length; i++) {
			if (sdp[i].startsWith("m=")) {
				++p0;
			}
			if (sdp[i].startsWith("a=mid:" + contentName)) {
				return p0;
			}
		}
		return -1;
	}

	protected static String getCapsNode(Presence presence) throws XMLException {
		if (presence == null) {
			return null;
		}
		Element c = presence.getChildrenNS("c", "http://jabber.org/protocol/caps");
		if (c == null) {
			return null;
		}

		String node = c.getAttribute("node");
		String ver = c.getAttribute("ver");
		if (node == null || ver == null) {
			return null;
		}

		return node + "#" + ver;
	}

	public void start() {
		EglBase rootEglBase = avComponent.getEglBase();
		//Initialize PeerConnectionFactory globals.
		PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(
				this).setEnableVideoHwAcceleration(true).createInitializationOptions();
		PeerConnectionFactory.initialize(initializationOptions);

		//Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
				rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
		DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(
				rootEglBase.getEglBaseContext());
		peerConnectionFactory = PeerConnectionFactory.builder()
				.setVideoEncoderFactory(defaultVideoEncoderFactory)
				.setVideoDecoderFactory(defaultVideoDecoderFactory)
				.createPeerConnectionFactory();

		//Now create a VideoCapturer instance.
		this.videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));

		//Create MediaConstraints - Will be useful for specifying video and audio constraints.
		audioConstraints = new MediaConstraints();
		videoConstraints = new MediaConstraints();

		audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

		//Create a VideoSource instance
		if (videoCapturerAndroid != null) {
			videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
		}
		localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

		//create an AudioSource instance
		audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
		localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

		if (videoCapturerAndroid != null) {
			videoCapturerAndroid.startCapture(1024, 720, 30);
		}
		// And finally, with our VideoRenderer ready, we
		// can add our renderer to the VideoTrack.
		localVideoTrack.addSink(avComponent.getLocalVideoView());

		gotUserMedia = true;

	}

	public void stop() {
		Log.i(TAG, "Stopped. Removing events handlers.");

		jaxmpp.getEventBus().remove(sessionTerminateHandler);
		jaxmpp.getEventBus().remove(jingleTransportHandler);
		jaxmpp.getEventBus().remove(acceptOfferHandler);

		avComponent.stop();
		try {
			if (localVideoTrack != null) {
				localVideoTrack.dispose();
			}
		} catch (Exception e) {
			Log.w(TAG, "Cannot dispose localVideoTrack", e);
		}
		try {
			if (localAudioTrack != null) {
				localAudioTrack.dispose();
			}
		} catch (Exception e) {
			Log.w(TAG, "Cannot dispose localAudioTrack", e);
		}
		try {
			if (audioSource != null) {
				audioSource.dispose();
			}
		} catch (Exception e) {
			Log.w(TAG, "Cannot dispose audioSource", e);
		}
		try {
			if (videoCapturerAndroid != null) {
				videoCapturerAndroid.dispose();
			}
		} catch (Exception e) {
			Log.w(TAG, "Cannot dispose videoCapturerAndroid", e);
		}
	}

	/**
	 * This method will be called directly by the app when it is the initiator and has got the local media
	 * or when the remote peer sends a message through socket that it is ready to transmit AV data
	 */
	public void onTryToStart() {
		runOnUiThread(() -> {
			if ( // !SignallingClient.getInstance().isStarted &&
					localVideoTrack != null
				//	&&					SignallingClient.getInstance().isChannelReady
			) {
				createPeerConnection();
				//	SignallingClient.getInstance().isStarted = true;
				if (initiator) {
					doCall();
				}
			}
		});
	}

	/**
	 * Received local ice candidate. Send it to remote peer through signalling for negotiation
	 */
	public void onIceCandidateReceived(IceCandidate iceCandidate) {
		//we have received ice candidate. We can set it to the other peer.

		try {
			final Element transport = sdpConverter.fromSDPTransport(iceCandidate.sdp.split(SDP.LINE));
			SDP.findLine("a=ice-pwd:", localPeer.getLocalDescription().description.split(SDP.LINE), line -> {
				transport.setAttribute("pwd", line.substring(10));
			});

			Element content = ElementFactory.create("content");
			content.setAttribute("name", iceCandidate.sdpMid);
			content.setAttribute("creator", "responder");
			content.addChild(transport);

			Element jingle = ElementFactory.create("jingle");
			jingle.setAttribute("creator", "responder");
			jingle.setXMLNS(JINGLE_XMLNS);
			jingle.addChild(content);

			Log.i(TAG, "Emiting IceCandidate sdpMid=" + iceCandidate.sdpMid + " sdpMLineIndex=" +
					iceCandidate.sdpMLineIndex + " serverUrl=" + iceCandidate.serverUrl + "\n" + iceCandidate.sdp +
					"\n" + jingle.getAsString());

			jaxmpp.getModule(JingleModule.class).sendTransportInfo(session, jingle);

		} catch (JaxmppException e) {
			e.printStackTrace();
			Log.w(TAG, e);
		}

	}

	/**
	 * SignallingCallback - called when the room is created - i.e. you are the initiator
	 */
	public void onCreatedRoom() {
		showToast("You created the room " + gotUserMedia);
		if (gotUserMedia) {
			// FIXME	SignallingClient.getInstance().emitMessage("got user media");
		}
	}

	/**
	 * SignallingCallback - called when you join the room - you are a participant
	 */
	public void onJoinedRoom() {
		showToast("You joined the room " + gotUserMedia);
		if (gotUserMedia) {
			// FIXME SignallingClient.getInstance().emitMessage("got user media");
		}
	}

	public void onNewPeerJoined() {
		showToast("Remote Peer Joined");
	}

	/**
	 * SignallingCallback - Called when remote peer sends offer
	 */
	public void onOfferReceived(final JingleSession session) {
		runOnUiThread(() -> {
			if (!initiator) {
				onTryToStart();
			}
			try {
				String sdp = sdpConverter.toSDP(session.getId(), session.getSid(), session.getJingleElement());
				Log.i(TAG, "Received Offer \n" + sdp);

				localPeer.setRemoteDescription(new CustomSdpObserver(TAG + ":localSetRemote"),
											   new SessionDescription(SessionDescription.Type.OFFER, sdp));
				doAnswer(session);
				updateVideoViews(true);
			} catch (JaxmppException e) {
				Log.w(TAG, e);
			}
		});
	}

	public void onAnswerReceived(SessionObject sessionObject, JingleSession jingleSession, JingleElement jingle) {
		showToast("Received Answer");

		try {
			String sdp = sdpConverter.toSDP(jingleSession.getId(), jingleSession.getSid(), jingle);

			localPeer.setRemoteDescription(new CustomSdpObserver(TAG + ":localSetRemote"),
										   new SessionDescription(SessionDescription.Type.ANSWER, sdp));

		} catch (XMLException e) {
			Log.e(TAG, "Cannot convert to SDP", e);
			e.printStackTrace();
		}
		updateVideoViews(true);
	}

	/**
	 * Remote IceCandidate received
	 *
	 * @param session
	 */
	public synchronized void onIceCandidateReceived(JingleSession session) throws XMLException {
		Queue<JingleContent> queue = session.getCandidates();
		JingleContent c;
		while ((c = queue.poll()) != null) {
			Transport transport = new Transport(c.getChildrenNS("transport", "urn:xmpp:jingle:transports:ice-udp:1"));
			String sdp = sdpConverter.toSDP(transport);
			int idx = findSdpMLineIndex(session, c.getContentName());
			// FIXME
			IceCandidate ice = new IceCandidate(transport.getCandidates().get(0).getId(), idx, sdp);
			Log.i(TAG,
				  "Received IceCandidate " + transport.getAsString() + "\nsdpMid=" + ice.sdpMid + " sdpMLineIndex=" +
						  ice.sdpMLineIndex + " serverUrl=" + ice.serverUrl + "\n" + ice.sdp);
			localPeer.addIceCandidate(ice);
		}

	}

	public void showToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(VideoChatActivity.this, msg, Toast.LENGTH_SHORT).show());
	}

	public JID findVideoChatClient(final BareJID jid) throws XMLException {
		Map<String, Presence> pr = PresenceModule.getPresenceStore(jaxmpp.getSessionObject()).getPresences(jid);
		if (pr == null) {
			return null;
		}
		for (Presence p : pr.values()) {
			if (isVideoChatAvailable(p.getFrom())) {
				return p.getFrom();
			}
		}
		return null;
	}

	public boolean isVideoChatAvailable(final JID jid) {
		Presence p = PresenceModule.getPresenceStore(jaxmpp.getSessionObject()).getPresence(jid);
		CapabilitiesModule capsModule = jaxmpp.getModule(CapabilitiesModule.class);
		CapabilitiesCache capsCache = capsModule.getCache();

		try {
			String capsNode = getCapsNode(p);
			Set<String> features = (capsCache != null) ? capsCache.getFeatures(capsNode) : null;

			return (features != null && features.contains(JingleModule.JINGLE_XMLNS) &&
					features.contains("urn:xmpp:jingle:transports:ice-udp:1") &&
					features.contains("urn:xmpp:jingle:apps:rtp:video") &&
					features.contains("urn:xmpp:jingle:apps:rtp:audio") &&
					features.contains("urn:xmpp:jingle:apps:dtls:0"));
		} catch (XMLException ex) {
			return false;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		Log.i(TAG, "permissions: " + Arrays.toString(permissions));
		Log.i(TAG, "grantResults: " + Arrays.toString(grantResults));

		avComponent.initVideos();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_chat);

		this.peerIceServers.add(PeerConnection.IceServer.builder("stun:64.233.161.127:19302").createIceServer());

		this.initiator = getIntent().getBooleanExtra(INITIATOR_KEY, false);

		avComponent = findViewById(R.id.avcomponent);
		avComponent.setAccount(getIntent().getStringExtra(ACCOUNT_KEY));
		avComponent.setHangupHandler(this::hangup);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
				PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
						PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
																 Manifest.permission.CAMERA}, 1);
		} else {
			avComponent.initVideos();
		}
	}

	@Override
	protected void onXMPPServiceConnected() {
		String account = getIntent().getStringExtra(ACCOUNT_KEY);
		Log.i(TAG, "XMPP Service Connected. Getting jaxmpp for " + account);
		this.jaxmpp = getJaxmpp(account);

		if (this.jaxmpp == null) {
			throw new RuntimeException("Cannot get XMPP Client.");
		}

		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleSessionAcceptHandler.JingleSessionAcceptEvent.class, acceptOfferHandler);
		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleSessionTerminateHandler.JingleSessionTerminateEvent.class,
							sessionTerminateHandler);
		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleTransportInfoHandler.JingleTransportInfoEvent.class,
							jingleTransportHandler);

		start();

		final JID jid = JID.jidInstance(getIntent().getStringExtra(JID_KEY));
		avComponent.setRemoteJid(jid);

		if (!initiator) {
			// Incomming call. Asking
			String sid = getIntent().getStringExtra(SID_KEY);
			this.session = JingleModule.getSession(jaxmpp.getSessionObject(), sid, jid);

			avComponent.askForPickup(this::handlePickupResult);

		} else {
			onTryToStart();
		}
	}

	@Override
	protected void onXMPPServiceDisconnected() {

	}

	private void onRemoteHangUp(SessionObject msg) {
		runOnUiThread(() -> showToast("Remote Peer hungup"));
		runOnUiThread(this::hangup);
	}

	private void handlePickupResult(boolean pickedUp) {
		if (pickedUp) {
			acceptIncommingCall();
		} else {
			hangup();
		}
	}

	private void acceptIncommingCall() {
		if (session != null) {
			onOfferReceived(session);
		}

		try {
			onIceCandidateReceived(session);
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * Creating the local peerconnection instance
	 */
	private void createPeerConnection() {
		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
		rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;
		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
		rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
		localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver(
				TAG + ":localPeerCreation") {
			@Override
			public void onIceCandidate(IceCandidate iceCandidate) {
				super.onIceCandidate(iceCandidate);
				onIceCandidateReceived(iceCandidate);
			}

			@Override
			public void onAddStream(MediaStream mediaStream) {
				showToast("Received Remote stream");
				super.onAddStream(mediaStream);
				gotRemoteStream(mediaStream);
			}
		});

		addStreamToLocalPeer();
	}

	/**
	 * Adding the stream to the localpeer
	 */
	private void addStreamToLocalPeer() {
		//creating local mediastream
		MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
		stream.addTrack(localAudioTrack);
		stream.addTrack(localVideoTrack);
		boolean r = localPeer.addStream(stream);
		Log.d(TAG, "Adding MediaStream: " + r);
	}

	/**
	 * This method is called when the app is initiator - We generate the offer and send it over through socket
	 * to remote peer
	 */
	private void doCall() {
		sdpConstraints = new MediaConstraints();
		sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		localPeer.createOffer(new CustomSdpObserver(TAG + ":localCreateOffer") {
			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				super.onCreateSuccess(sessionDescription);
				localPeer.setLocalDescription(new CustomSdpObserver(TAG + ":localSetLocalDesc"), sessionDescription);
				Log.d("onCreateSuccess", "SignallingClient emit ");

				try {
					JID jid = JID.jidInstance(getIntent().getStringExtra(JID_KEY));
					if (jid.getResource() == null) {
						jid = findVideoChatClient(jid.getBareJid());
						if (jid == null) {
							Log.e("TAG", "Can't call!!!");
							throw new RuntimeException("Cannot call to this client");
						}
					}
					Element jingle = sdpConverter.fromSDP(sessionDescription.description);

					VideoChatActivity.this.session = jaxmpp.getModule(JingleModule.class)
							.initiateSession(jid, jingle, new AsyncCallback() {
								@Override
								public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
										throws JaxmppException {
									Log.w(TAG, "Error on initiateSession: " + error);
								}

								@Override
								public void onSuccess(Stanza responseStanza) throws JaxmppException {
									Log.i(TAG, "Session initiated");
								}

								@Override
								public void onTimeout() throws JaxmppException {
									Log.w(TAG, "No response");
								}
							});
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "O kurwa!", e);
				}

			}
		}, sdpConstraints);
	}

	/**
	 * Received remote peer's media stream. we will get the first video track and render it
	 */
	private void gotRemoteStream(MediaStream stream) {
		//we have remote video stream. add to the renderer.
		if (stream != null && stream.videoTracks != null && stream.videoTracks.size() > 0) {
			final VideoTrack videoTrack = stream.videoTracks.get(0);
			runOnUiThread(() -> {
				try {
					avComponent.setRemoteVideoViewVisible(true);
					videoTrack.addSink(avComponent.getRemoteVideoView());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void doAnswer(final JingleSession session) {
		localPeer.createAnswer(new CustomSdpObserver(TAG + ":localCreateAns") {
			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				super.onCreateSuccess(sessionDescription);
				localPeer.setLocalDescription(new CustomSdpObserver(TAG + ":localSetLocal"), sessionDescription);

				try {
					Log.d(TAG, "Accept session. Local description:\n" + sessionDescription.description);
					Element jingle = sdpConverter.fromSDP(sessionDescription.description);
					jaxmpp.getModule(JingleModule.class).acceptSession(session, jingle, new AsyncCallback() {
						@Override
						public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
								throws JaxmppException {
							Log.w(TAG, "Error on acceptSession: " + error);
						}

						@Override
						public void onSuccess(Stanza responseStanza) throws JaxmppException {
							Log.i(TAG, "Session accepted");
						}

						@Override
						public void onTimeout() throws JaxmppException {
							Log.w(TAG, "No response");
						}
					});
				} catch (JaxmppException e) {
					Log.w(TAG, e);
				}
			}
		}, new MediaConstraints());
	}

	private void updateVideoViews(final boolean remoteVisible) {
		runOnUiThread(() -> {
			avComponent.updateVideoViews(remoteVisible);
		});
	}

	private void hangup() {
		try {
			stop();
			if (localPeer != null) {
				localPeer.close();
				localPeer = null;
			}
			updateVideoViews(false);
			jaxmpp.getModule(JingleModule.class).terminateSession(session);
		} catch (Exception e) {
			Log.e(TAG, "Hangup problem", e);
			e.printStackTrace();
		} finally {
			finish();
		}
	}

	private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
		final String[] deviceNames = enumerator.getDeviceNames();

		// First, try to find front facing camera
		Logging.d(TAG, "Looking for front facing cameras.");
		for (String deviceName : deviceNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating front facing camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		// Front facing camera not found, try something else
		Logging.d(TAG, "Looking for other cameras.");
		for (String deviceName : deviceNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating other camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		return null;
	}
}
