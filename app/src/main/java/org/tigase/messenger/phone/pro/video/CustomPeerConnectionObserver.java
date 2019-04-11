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

import android.util.Log;
import org.webrtc.*;

public class CustomPeerConnectionObserver
		implements PeerConnection.Observer {

	private String logTag;

	CustomPeerConnectionObserver(String logTag) {
		this.logTag = this.getClass().getCanonicalName();
		this.logTag = this.logTag + " " + logTag;
	}

	@Override
	public void onSignalingChange(PeerConnection.SignalingState signalingState) {
		Log.d(logTag, "onSignalingChange() called with: signalingState = [" + signalingState + "]");
	}

	@Override
	public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
		Log.d(logTag, "onIceConnectionChange() called with: iceConnectionState = [" + iceConnectionState + "]");
	}

	@Override
	public void onIceConnectionReceivingChange(boolean b) {
		Log.d(logTag, "onIceConnectionReceivingChange() called with: b = [" + b + "]");
	}

	@Override
	public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
		Log.d(logTag, "onIceGatheringChange() called with: iceGatheringState = [" + iceGatheringState + "]");
	}

	@Override
	public void onIceCandidate(IceCandidate iceCandidate) {
		Log.d(logTag, "onIceCandidate() called with: iceCandidate = [" + iceCandidate + "]");
	}

	@Override
	public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
		Log.d(logTag, "onIceCandidatesRemoved() called with: iceCandidates = [" + iceCandidates + "]");
	}

	@Override
	public void onAddStream(MediaStream mediaStream) {
		Log.d(logTag, "onAddStream() called with: mediaStream = [" + mediaStream + "]");
	}

	@Override
	public void onRemoveStream(MediaStream mediaStream) {
		Log.d(logTag, "onRemoveStream() called with: mediaStream = [" + mediaStream + "]");
	}

	@Override
	public void onDataChannel(DataChannel dataChannel) {
		Log.d(logTag, "onDataChannel() called with: dataChannel = [" + dataChannel + "]");
	}

	@Override
	public void onRenegotiationNeeded() {
		Log.d(logTag, "onRenegotiationNeeded() called");
	}

	@Override
	public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
		Log.d(logTag,
			  "onAddTrack() called with: rtpReceiver = [" + rtpReceiver + "], mediaStreams = [" + mediaStreams + "]");
	}
}
