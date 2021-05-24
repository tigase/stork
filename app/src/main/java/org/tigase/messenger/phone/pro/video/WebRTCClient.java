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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import org.tigase.jaxmpp.modules.jingle.*;
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
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.tigase.jaxmpp.modules.jingle.JingleModule.JINGLE_XMLNS;

public class WebRTCClient
		implements Closeable {

	private static final String TAG = "WRTCClient";
	private final Context context;
	private final EglBase eglBase;
	private final Handler handler;
	private final Queue<IceCandidate> iceCandidates = new LinkedList<>();
	private final boolean initiator;
	private final JaxmppCore jaxmpp;
	private final List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
	private final SDP sdpConverter = new SDP();
	private AudioSource audioSource;
	private boolean canEmitIceCandidate = false;
	private boolean initialized = false;
	private JID jid;
	private AudioTrack localAudioTrack;
	private PeerConnection localPeer;
	private VideoSinkHandler localVideoSinkHandler;
	private VideoTrack localVideoTrack;
	private PeerConnectionFactory peerConnectionFactory;
	private RemoteHangupHandler remoteHangupHandler;
	private VideoSinkHandler remoteVideoSinkHandler;
	private RemoteVideoVisibleHandler remoteVideoVisibleHandler;
	private JingleSession session;
	private final JingleModule.JingleSessionAcceptHandler acceptOfferHandler = (sessionObject, jingleSession, jingle) -> {
		onAnswerReceived(sessionObject, jingleSession, jingle);
		return true;
	};
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
	private VideoSource videoSource;

	private static Collection<JID> getAllPotentialResources(final JaxmppCore jaxmpp, final BareJID jid)
			throws XMLException {
		final HashSet<JID> result = new HashSet<>();
		Map<String, Presence> pr = PresenceModule.getPresenceStore(jaxmpp.getSessionObject()).getPresences(jid);
		if (pr != null) {
			for (Presence p : pr.values()) {
				if (isVideoChatAvailable(jaxmpp, p.getFrom())) {
					result.add(p.getFrom());
				}
			}
		}
		return result;
	}

	private static String getCapsNode(Presence presence) throws XMLException {
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

	public static boolean isVideoAvailable(final JaxmppCore jaxmpp, final BareJID jid) {
		try {
			return getAllPotentialResources(jaxmpp, jid).size() > 0;
		} catch (Exception e) {
			Log.w(TAG, "Cannot check video availability", e);
			return false;
		}
	}

	private static boolean isVideoChatAvailable(final JaxmppCore jaxmpp, final JID jid) {
		if (jid == null || jid.getResource() == null) {
			return false;
		}
		Presence p = PresenceModule.getPresenceStore(jaxmpp.getSessionObject()).getPresence(jid);
		CapabilitiesModule capsModule = jaxmpp.getModule(CapabilitiesModule.class);
		CapabilitiesCache capsCache = capsModule.getCache();

		try {
			StanzaType presenceType = p.getType();
			if (presenceType == StanzaType.error) {
				return false;
			}

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

	public WebRTCClient(Context context, EglBase eglBase, Handler handler, JaxmppCore jaxmpp, JID jid,
						boolean initiator) {
		this.context = context;
		this.jaxmpp = jaxmpp;
		this.initiator = initiator;
		this.jid = jid;
		this.handler = handler;
		this.eglBase = eglBase;

		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleSessionAcceptHandler.JingleSessionAcceptEvent.class, acceptOfferHandler);
		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleSessionTerminateHandler.JingleSessionTerminateEvent.class,
							sessionTerminateHandler);
		jaxmpp.getEventBus()
				.addHandler(JingleModule.JingleTransportInfoHandler.JingleTransportInfoEvent.class,
							jingleTransportHandler);
	}

	public JID getJid() {
		return jid;
	}

	public void setLocalVideoSinkHandler(VideoSinkHandler localVideoSinkHandler) {
		this.localVideoSinkHandler = localVideoSinkHandler;
	}

	@Override
	public void close() throws IOException {
		try {
			jaxmpp.getModule(JingleModule.class).terminateSession(session);
		} catch (JaxmppException e) {
			Log.w(TAG, "Cannot send terminate session", e);
		}

		jaxmpp.getEventBus().remove(sessionTerminateHandler);
		jaxmpp.getEventBus().remove(jingleTransportHandler);
		jaxmpp.getEventBus().remove(acceptOfferHandler);

		if (localPeer != null) {
			localPeer.close();
		}

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

	public void startCalling() {
		createPeerConnection();

		MediaConstraints sdpConstraints = new MediaConstraints();
		sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		Log.d(TAG, "Creating offer");
		localPeer.createOffer(new CustomSdpObserver(TAG + ":localCreateOffer") {
			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				try {
					super.onCreateSuccess(sessionDescription);
					Log.d(TAG, "Setting local description");
					localPeer.setLocalDescription(new CustomSdpObserver(TAG + ":localSetLocalDesc") {
						@Override
						public void onSetSuccess() {
							try {
								super.onSetSuccess();

								Log.d(TAG, "SignallingClient emit SDP\n" + sessionDescription.description);

								Element jingle = sdpConverter.fromSDP(sessionDescription.description);

								sendInitiateToAllResources(jingle);

							} catch (Exception e) {
								e.printStackTrace();
								Log.e(TAG, "O kurwa!", e);
							} catch (Throwable e) {
								Log.wtf(TAG, "Fail 03", e);
								throw e;
							}
						}
					}, sessionDescription);
				} catch (Throwable e) {
					Log.wtf(TAG, "Fail 02", e);
					throw e;
				}
			}
		}, sdpConstraints);
	}

	public void addIceServers(final String... uri) {
		for (String s : uri) {
			addIceServer(s);
		}
	}

	public void addIceServer(final String uri) {
		PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder(uri);
		iceServerBuilder.setTlsCertPolicy(
				PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
		PeerConnection.IceServer iceServer = iceServerBuilder.createIceServer();

		this.peerIceServers.add(iceServer);
	}

	public void addIceServer(final String uri, String username, String password) {
		PeerConnection.IceServer.Builder iceServerBuilder = PeerConnection.IceServer.builder(uri);
		iceServerBuilder.setPassword(username);
		iceServerBuilder.setPassword(password);
		iceServerBuilder.setTlsCertPolicy(
				PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK); //this does the magic.
		PeerConnection.IceServer iceServer = iceServerBuilder.createIceServer();

		this.peerIceServers.add(iceServer);
	}

	public void setSession(JingleSession session) {
		this.session = session;
	}

	public void acceptIncommingCall() {
		if (session != null) {
			onOfferReceived(session);
		}
	}

	public void setRemoteVideoSinkHandler(VideoSinkHandler remoteVideoSinkHandler) {
		this.remoteVideoSinkHandler = remoteVideoSinkHandler;
	}

	public void setRemoteVideoVisibleHandler(RemoteVideoVisibleHandler remoteVideoVisibleHandler) {
		this.remoteVideoVisibleHandler = remoteVideoVisibleHandler;
	}

	public void initialize() {
		PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(
				context).createInitializationOptions();
		PeerConnectionFactory.initialize(initializationOptions);

		//Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
				eglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
		DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(
				eglBase.getEglBaseContext());
		peerConnectionFactory = PeerConnectionFactory.builder()
				.setVideoEncoderFactory(defaultVideoEncoderFactory)
				.setVideoDecoderFactory(defaultVideoDecoderFactory)
				.createPeerConnectionFactory();

		//Now create a VideoCapturer instance.
		this.videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));
		//Create MediaConstraints - Will be useful for specifying video and audio constraints.
		MediaConstraints audioConstraints = new MediaConstraints();
		MediaConstraints videoConstraints = new MediaConstraints();

		audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

		SurfaceTextureHelper helper = SurfaceTextureHelper.create("SurfaceTexture", eglBase.getEglBaseContext());

		//Create a VideoSource instance
		if (videoCapturerAndroid != null) {
			videoSource = peerConnectionFactory.createVideoSource(true);
			videoCapturerAndroid.initialize(helper, context, videoSource.getCapturerObserver());
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

		if (localVideoSinkHandler != null) {
			handler.post(() -> {
				VideoSink sink = localVideoSinkHandler.getVideoSink();
				localVideoTrack.addSink(sink);
			});
		}
	}

	public void setRemoteHangupHandler(RemoteHangupHandler remoteHangupHandler) {
		this.remoteHangupHandler = remoteHangupHandler;
	}

	private void sendInitiateToJid(final ConcurrentHashMap<JID, JingleSession> sessions, final Element jingle,
								   final JID jid) throws JaxmppException {
		JingleSession js = jaxmpp.getModule(JingleModule.class).initiateSession(jid, jingle, new AsyncCallback() {
			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				Log.w(TAG, "Error on initiateSession: " + error);
				cancelInitiation(sessions, jid);
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				Log.i(TAG, "Session initiated !!");
				acceptInitiationFrom(sessions, jid);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.w(TAG, "No response for initiateSession");
				cancelInitiation(sessions, jid);
			}
		});
		synchronized (sessions) {
			sessions.put(jid, js);
		}
	}

	private void cancelInitiation(ConcurrentHashMap<JID, JingleSession> sessions, JID jid) throws JaxmppException {
		synchronized (sessions) {
			JingleSession js = sessions.remove(jid);
			jaxmpp.getModule(JingleModule.class).terminateSession(js);
		}
	}

	private void acceptInitiationFrom(ConcurrentHashMap<JID, JingleSession> sessions, JID jid) throws JaxmppException {
		synchronized (sessions) {
			this.session = sessions.remove(jid);
			this.jid = jid;
			emitIceCandidates();
		}
		ArrayList<JID> jids = new ArrayList<>(sessions.keySet());
		for (JID j : jids) {
			cancelInitiation(sessions, j);
		}
	}

	private void sendInitiateToAllResources(final Element jingle) throws JaxmppException {
		final Collection<JID> potentialClients = new HashSet<>();
		if (this.jid.getResource() != null) {
			potentialClients.add(this.jid);
		} else {
			potentialClients.addAll(getAllPotentialResources(jaxmpp, jid.getBareJid()));
		}

		final ConcurrentHashMap<JID, JingleSession> sessions = new ConcurrentHashMap<>();
		for (JID potentialClient : potentialClients) {
			sendInitiateToJid(sessions, jingle, potentialClient);
		}

	}

	/**
	 * SignallingCallback - Called when remote peer sends offer
	 */
	private void onOfferReceived(final JingleSession session) {
		handler.post(() -> {
			try {
				createPeerConnection();

				String sdp = sdpConverter.toSDP(session.getId(), session.getSid(), session.getJingleElement());
				Log.i(TAG, "Received Offer \n" + sdp);

				Log.d(TAG, "Setting remote description");
				localPeer.setRemoteDescription(new CustomSdpObserver(TAG + ":localSetRemote") {
					@Override
					public void onSetSuccess() {
						try {
							super.onSetSuccess();
							WebRTCClient.this.initialized = true;
//							handler.post(() -> onIceCandidateReceived(session));
							onIceCandidateReceived(session);

							doAnswer(session);
							updateVideoViews(true);
						} catch (Throwable e) {
							Log.wtf(TAG, "Fail 04", e);
							throw e;
						}
					}
				}, new SessionDescription(SessionDescription.Type.OFFER, sdp));
			} catch (JaxmppException e) {
				Log.w(TAG, e);
			}
		});
	}

	private void updateVideoViews(boolean remoteVideoVisible) {
		if (remoteVideoVisibleHandler != null) {
			handler.post(() -> {
				remoteVideoVisibleHandler.updateVideoVisible(remoteVideoVisible);
			});
		}
	}

	private void onRemoteHangUp(SessionObject msg) {
		try {
			close();
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
			if (remoteHangupHandler != null) {
				handler.post(() -> remoteHangupHandler.onRemoteHangup());
			}
		}
	}

	private void emitIceCandidates() {
		Log.i(TAG, "Flushing IceCandidates queue: " + iceCandidates.size());
		canEmitIceCandidate = true;
		IceCandidate iceCandidate;
		while ((iceCandidate = iceCandidates.poll()) != null) {
			//we have received ice candidate. We can set it to the other peer.
			Log.i(TAG, "Emit iceCandidate " + iceCandidate);
			try {
				final Element transport = sdpConverter.fromSDPTransport(iceCandidate.sdp.split(SDP.LINE));
				SDP.findLine("a=ice-pwd:", localPeer.getLocalDescription().description.split(SDP.LINE), line -> {
					transport.setAttribute("pwd", line.substring(10));
				});

				Element content = ElementFactory.create("content");
				content.setAttribute("name", iceCandidate.sdpMid);
				content.setAttribute("creator", initiator ? "initiator" : "responder");
				content.addChild(transport);

				Element jingle = ElementFactory.create("jingle");
//				jingle.addChild(ElementFactory.create("sdp", Base64.encode(iceCandidate.sdp.getBytes()), SDP.TIGASE_SDP_XMLNS));
				jingle.setAttribute("creator", initiator ? "initiator" : "responder");
				jingle.setXMLNS(JINGLE_XMLNS);
				jingle.addChild(content);

				Log.i(TAG, "Emiting IceCandidate sdpMid=" + iceCandidate.sdpMid + " sdpMLineIndex=" +
						iceCandidate.sdpMLineIndex + " serverUrl=" + iceCandidate.serverUrl + "\n" + iceCandidate.sdp +
						"\n" + jingle.getAsString());

				jaxmpp.getModule(JingleModule.class).sendTransportInfo(session, jingle);

			} catch (Throwable e) {
				e.printStackTrace();
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * Received local ice candidate. Send it to remote peer through signalling for negotiation
	 */
	private void emitIceCandidate(IceCandidate iceCandidate) {
		iceCandidates.offer(iceCandidate);
		if (canEmitIceCandidate) {
			emitIceCandidates();
		}
	}

	private void gotRemoteStream(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
		//we have remote video stream. add to the renderer.
		if (mediaStreams!=null && mediaStreams.length > 0) {
			final VideoTrack videoTrack = mediaStreams[0].videoTracks.get(0);


			updateVideoViews(true);
			if (remoteVideoSinkHandler != null) {
				handler.post(() -> {
					VideoSink sink = remoteVideoSinkHandler.getVideoSink();
					videoTrack.addSink(sink);
				});
			}
		}}

	/**
	 * Received remote peer's media stream. we will get the first video track and render it
	 */
	private void gotRemoteStream(MediaStream stream) {
		//we have remote video stream. add to the renderer.
		if (stream != null && stream.videoTracks != null && stream.videoTracks.size() > 0) {
			final VideoTrack videoTrack = stream.videoTracks.get(0);

			updateVideoViews(true);
			if (remoteVideoSinkHandler != null) {
				handler.post(() -> {
					VideoSink sink = remoteVideoSinkHandler.getVideoSink();
					videoTrack.addSink(sink);
				});
			}
		}
	}

	private void createPeerConnection() {
		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
		rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

		rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
		rtcConfig.iceCandidatePoolSize = 2;
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
		rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
		rtcConfig.candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL;

		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
		rtcConfig.disableIpv6 = true;

		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
		localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver(
				TAG + ":localPeerCreation") {
			@Override
			public void onIceCandidate(IceCandidate iceCandidate) {
				super.onIceCandidate(iceCandidate);
				Log.i(TAG, "MAM!!! PUBLIKUJEMY!!! " + iceCandidate);
				emitIceCandidate(iceCandidate);
			}

			@Override
			public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
				super.onAddTrack(rtpReceiver, mediaStreams);
				gotRemoteStream(rtpReceiver, mediaStreams);
			}

			@Override
			public void onAddStream(MediaStream mediaStream) {
				super.onAddStream(mediaStream);
				gotRemoteStream(mediaStream);
			}

		});

		addStreamToLocalPeer();
	}



	private void doAnswer(final JingleSession session) {
		Log.d(TAG, "Creating answer");
		localPeer.createAnswer(new CustomSdpObserver(TAG + ":localCreateAns") {
			@Override
			public void onCreateSuccess(SessionDescription sessionDescription) {
				try {
					super.onCreateSuccess(sessionDescription);
					Log.d(TAG, "Setting local description");
					localPeer.setLocalDescription(new CustomSdpObserver(TAG + ":localSetLocal") {

						@Override
						public void onSetSuccess() {
							try {
								super.onSetSuccess();

								Log.d(TAG, "Accept session. Local description:\n" + sessionDescription.description);
								Element jingle = sdpConverter.fromSDP(sessionDescription.description);
								jaxmpp.getModule(JingleModule.class)
										.acceptSession(session, jingle, new AsyncCallback() {
											@Override
											public void onError(Stanza responseStanza,
																XMPPException.ErrorCondition error)
													throws JaxmppException {
												Log.w(TAG, "Error on acceptSession: " + error);
											}

											@Override
											public void onSuccess(Stanza responseStanza) throws JaxmppException {
												Log.i(TAG, "Session accepted");
												emitIceCandidates();
											}

											@Override
											public void onTimeout() throws JaxmppException {
												Log.w(TAG, "No response");
											}
										});
							} catch (JaxmppException e) {
								Log.w(TAG, e);
							} catch (Throwable e) {
								Log.wtf(TAG, "Fail 01", e);
								throw e;
							}
						}
					}, sessionDescription);
				} catch (Throwable e) {
					Log.wtf(TAG, "Fail 01", e);
					throw e;
				}
			}
		}, new MediaConstraints());
	}

	/**
	 * Adding the stream to the localpeer
	 */
	private void addStreamToLocalPeer() {
		//creating local mediastream
		MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
		stream.addTrack(localAudioTrack);
		stream.addTrack(localVideoTrack);

		localPeer.addTrack(localAudioTrack);
		localPeer.addTrack(localVideoTrack);

//		boolean r = localPeer.addStream(stream);
//		Log.d(TAG, "Adding MediaStream: " + r);
	}

	/**
	 * Remote IceCandidate received
	 */
	private synchronized void onIceCandidateReceived(JingleSession session) {
		if (!initialized) {
			return;
		}
		if (localPeer == null) {
			return;
		}
		try {
			Queue<JingleContent> queue = session.getCandidates();
			JingleContent c;
			while ((c = queue.poll()) != null) {
				String originalSDP = SDP.getOriginalSDP(c);
				Transport transport = new Transport(
						c.getChildrenNS("transport", "urn:xmpp:jingle:transports:ice-udp:1"));
				String sdp = originalSDP != null ? originalSDP : sdpConverter.toSDP(transport);
				int idx = findSdpMLineIndex(session, c.getContentName());
				// FIXME

				Log.i(TAG, "-----\n" + transport.getCandidates() + "\n-----\n");

				IceCandidate ice = new IceCandidate(c.getContentName(), idx, sdp);
				Log.i(TAG, "add candidate to localPeer " + transport.getAsString() + "\n#sdpMid=" + ice.sdpMid +
						" sdpMLineIndex=" + ice.sdpMLineIndex + " serverUrl=" + ice.serverUrl + "\n" + ice.sdp);
				localPeer.addIceCandidate(ice);
			}
		} catch (Exception e) {
			Log.e(TAG, "Cannot process candidate", e);
		}
	}

	private int findSdpMLineIndex(JingleSession session, String contentName) throws XMLException {
		String[] sdp = sdpConverter.toSDP(session.getId(), session.getSid(), session.getJingleElement())
				.split(SDP.LINE);

		int p0 = -1;
		for (String s : sdp) {
			if (s.startsWith("m=")) {
				++p0;
			}
			if (s.startsWith("a=mid:" + contentName)) {
				return p0;
			}
		}
		return -1;
	}

	private void onAnswerReceived(SessionObject sessionObject, JingleSession jingleSession, JingleElement jingle) {
		try {
			String sdp = sdpConverter.toSDP(jingleSession.getId(), jingleSession.getSid(), jingle);
			Log.d(TAG, "Setting remote description");
			localPeer.setRemoteDescription(new CustomSdpObserver(TAG + ":localSetRemote") {
				@Override
				public void onSetSuccess() {
					super.onSetSuccess();
					WebRTCClient.this.initialized = true;
					onIceCandidateReceived(session);
					updateVideoViews(true);
				}
			}, new SessionDescription(SessionDescription.Type.ANSWER, sdp));

		} catch (XMLException e) {
			Log.e(TAG, "Cannot convert to SDP", e);
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

	public interface RemoteHangupHandler {

		void onRemoteHangup();
	}

	public interface RemoteVideoVisibleHandler {

		void updateVideoVisible(boolean visible);

	}

	public interface VideoSinkHandler {

		VideoSink getVideoSink();
	}
}
