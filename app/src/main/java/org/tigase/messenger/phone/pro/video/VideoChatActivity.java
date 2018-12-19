package org.tigase.messenger.phone.pro.video;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.tigase.jaxmpp.modules.jingle.JingleContent;
import org.tigase.jaxmpp.modules.jingle.JingleModule;
import org.tigase.jaxmpp.modules.jingle.JingleSession;
import org.tigase.jaxmpp.modules.jingle.Transport;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.webrtc.*;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.ArrayList;
import java.util.List;

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
	Button hangup;
	AudioTrack localAudioTrack;
	PeerConnection localPeer;
	VideoTrack localVideoTrack;
	SurfaceViewRenderer localVideoView;
	PeerConnectionFactory peerConnectionFactory;
	SurfaceViewRenderer remoteVideoView;
	EglBase rootEglBase;
	MediaConstraints sdpConstraints;
	MediaConstraints videoConstraints;
	VideoSource videoSource;
	private boolean initiator;
	private JaxmppCore jaxmpp;
	private JingleSession session;

	private final JingleModule.JingleTransportInfoHandler jingleTransportHandler = new JingleModule.JingleTransportInfoHandler() {
		@Override
		public boolean onJingleTransportInfo(SessionObject sessionObject, JID sender, String sid, JingleContent content)
				throws JaxmppException {
			onIceCandidateReceived(session, content);
			return true;
		}
	};

	private static int findSdpMLineIndex(JingleSession session, String contentName) throws XMLException {
		String[] sdp = sdpConverter.toSDP(session).split(SDP.LINE);

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

	public void start() {
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
		VideoCapturer videoCapturerAndroid;
		videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));

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
		localVideoView.setVisibility(View.VISIBLE);
		// And finally, with our VideoRenderer ready, we
		// can add our renderer to the VideoTrack.
		localVideoTrack.addSink(localVideoView);

		localVideoView.setMirror(true);
		remoteVideoView.setMirror(true);

		gotUserMedia = true;
		if (initiator) {
			onTryToStart();
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
		// FIXME	SignallingClient.getInstance().emitIceCandidate(iceCandidate);

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

	public void onRemoteHangUp(String msg) {
		showToast("Remote Peer hungup");
		runOnUiThread(this::hangup);
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
				String sdp = sdpConverter.toSDP(session);
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

	/**
	 * SignallingCallback - Called when remote peer sends answer to your offer
	 */
	public void onAnswerReceived() {
		showToast("Received Answer");
//		FIXME	localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(
//					SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()),
//					data.getString("sdp")));
		updateVideoViews(true);
	}

	/**
	 * Remote IceCandidate received
	 *
	 * @param session
	 * @param c
	 */
	public void onIceCandidateReceived(JingleSession session, JingleContent c) throws XMLException {
		Transport transport = new Transport(c.getChildrenNS("transport", "urn:xmpp:jingle:transports:ice-udp:1"));
		String sdp = sdpConverter.toSDP(transport);
		int idx = findSdpMLineIndex(session, c.getContentName());
		// FIXME
		IceCandidate ice = new IceCandidate(transport.getCandidates().get(0).getId(), idx, sdp);
		Log.i(TAG, "Received IceCandidate " + transport.getAsString() + "\nsdpMid=" + ice.sdpMid + " sdpMLineIndex=" +
				ice.sdpMLineIndex + " serverUrl=" + ice.serverUrl + "\n" + ice.sdp);
		localPeer.addIceCandidate(ice);
	}

	/**
	 * Util Methods
	 */
	public int dpToPx(int dp) {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
	}

	/**
	 * Closing up - normal hangup and app destroye
	 */

	public void showToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(VideoChatActivity.this, msg, Toast.LENGTH_SHORT).show());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_chat);

		this.peerIceServers.add(PeerConnection.IceServer.builder("stun:64.233.161.127:19302").createIceServer());

		this.initiator = getIntent().getBooleanExtra(INITIATOR_KEY, false);

		initViews();
		initVideos();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onXMPPServiceConnected() {
		String account = getIntent().getStringExtra(ACCOUNT_KEY);
		this.jaxmpp = getJaxmpp(account);

		start();

		if (!initiator) {
			JID jid = JID.jidInstance(getIntent().getStringExtra(JID_KEY));
			String sid = getIntent().getStringExtra(SID_KEY);
			this.session = JingleModule.getSession(jaxmpp.getSessionObject(), sid, jid);

			onOfferReceived(session);

			List<JingleContent> z = new ArrayList<>(session.getCandidates());
			jaxmpp.getEventBus()
					.addHandler(JingleModule.JingleTransportInfoHandler.JingleTransportInfoEvent.class,
								jingleTransportHandler);
			try {
				for (JingleContent c : z) {
					onIceCandidateReceived(session, c);
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
		}
	}

	@Override
	protected void onXMPPServiceDisconnected() {

	}

	private void initViews() {
		hangup = findViewById(R.id.end_call);
		localVideoView = findViewById(R.id.local_gl_surface_view);
		remoteVideoView = findViewById(R.id.remote_gl_surface_view);
		hangup.setOnClickListener(v -> hangup());
	}

	private void initVideos() {
		rootEglBase = EglBase.create();
		localVideoView.init(rootEglBase.getEglBaseContext(), null);
		remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
		localVideoView.setZOrderMediaOverlay(true);
		remoteVideoView.setZOrderMediaOverlay(true);
	}

	/**
	 * Creating the local peerconnection instance
	 */
	private void createPeerConnection() {
		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
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
				// FIXME 		SignallingClient.getInstance().emitMessage(sessionDescription);
			}
		}, sdpConstraints);
	}

	/**
	 * Received remote peer's media stream. we will get the first video track and render it
	 */
	private void gotRemoteStream(MediaStream stream) {
		//we have remote video stream. add to the renderer.
		final VideoTrack videoTrack = stream.videoTracks.get(0);
		runOnUiThread(() -> {
			try {
				remoteVideoView.setVisibility(View.VISIBLE);
				videoTrack.addSink(remoteVideoView);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private void doAnswer(final JingleSession session) {
		localPeer.createAnswer(new CustomSdpObserver(TAG + ":localCreateAns") {
			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				super.onCreateSuccess(sessionDescription);
				localPeer.setLocalDescription(new CustomSdpObserver(TAG + ":localSetLocal"), sessionDescription);

				try {
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
			ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
			if (remoteVisible) {
				params.height = dpToPx(100);
				params.width = dpToPx(100);
			} else {
				params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
													  ViewGroup.LayoutParams.MATCH_PARENT);
			}
			localVideoView.setLayoutParams(params);
		});

	}

	private void hangup() {
		try {
			localPeer.close();
			localPeer = null;
			// FIXME SignallingClient.getInstance().close();
			updateVideoViews(false);
		} catch (Exception e) {
			e.printStackTrace();
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
