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

import org.junit.Test;

public class SDPTest {

	private final String sampleSDP =
			"v=0\r\n" + "o=- 2587374044352840004 2 IN IP4 127.0.0.1\r\n" + "s=-\r\n" + "t=0 0\r\n" +
					"a=group:BUNDLE audio video\r\n" + "a=msid-semantic: WMS ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3\r\n" +
					"m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126\r\n" +
					"c=IN IP4 0.0.0.0\r\n" + "a=rtcp:9 IN IP4 0.0.0.0\r\n" + "a=ice-ufrag:i/Vw\r\n" +
					"a=ice-pwd:NLUEvK4LUXRAPlLsCG73ZBV7\r\n" + "a=ice-options:trickle\r\n" +
					"a=fingerprint:sha-256 07:FE:98:C1:88:38:38:51:63:F0:A4:24:C7:31:30:F6:31:8E:D5:4A:C7:8B:99:88:04:AB:9B:90:2C:7D:0A:80\r\n" +
					"a=setup:actpass\r\n" + "a=mid:audio\r\n" +
					"a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\n" + "a=sendrecv\r\n" + "a=rtcp-mux\r\n" +
					"a=rtpmap:111 opus/48000/2\r\n" + "a=rtcp-fb:111 transport-cc\r\n" +
					"a=fmtp:111 minptime=10;useinbandfec=1\r\n" + "a=rtpmap:103 ISAC/16000\r\n" +
					"a=rtpmap:104 ISAC/32000\r\n" + "a=rtpmap:9 G722/8000\r\n" + "a=rtpmap:0 PCMU/8000\r\n" +
					"a=rtpmap:8 PCMA/8000\r\n" + "a=rtpmap:106 CN/32000\r\n" + "a=rtpmap:105 CN/16000\r\n" +
					"a=rtpmap:13 CN/8000\r\n" + "a=rtpmap:110 telephone-event/48000\r\n" +
					"a=rtpmap:112 telephone-event/32000\r\n" + "a=rtpmap:113 telephone-event/16000\r\n" +
					"a=rtpmap:126 telephone-event/8000\r\n" + "a=ssrc:2691401930 cname:RKlfDkjBnO/bSV3W\r\n" +
					"a=ssrc:2691401930 msid:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3 af52b0c4-ba52-4fd7-aa43-bfdf36c364ab\r\n" +
					"a=ssrc:2691401930 mslabel:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3\r\n" +
					"a=ssrc:2691401930 label:af52b0c4-ba52-4fd7-aa43-bfdf36c364ab\r\n" +
					"m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 102 122 127 121 125 107 108 109 124 120 123 119 114\r\n" +
					"c=IN IP4 0.0.0.0\r\n" + "a=rtcp:9 IN IP4 0.0.0.0\r\n" + "a=ice-ufrag:i/Vw\r\n" +
					"a=ice-pwd:NLUEvK4LUXRAPlLsCG73ZBV7\r\n" + "a=ice-options:trickle\r\n" +
					"a=fingerprint:sha-256 07:FE:98:C1:88:38:38:51:63:F0:A4:24:C7:31:30:F6:31:8E:D5:4A:C7:8B:99:88:04:AB:9B:90:2C:7D:0A:80\r\n" +
					"a=setup:actpass\r\n" + "a=mid:video\r\n" + "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\n" +
					"a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\n" +
					"a=extmap:4 urn:3gpp:video-orientation\r\n" +
					"a=extmap:5 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01\r\n" +
					"a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\r\n" +
					"a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type\r\n" +
					"a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/video-timing\r\n" + "a=sendrecv\r\n" +
					"a=rtcp-mux\r\n" + "a=rtcp-rsize\r\n" + "a=rtpmap:96 VP8/90000\r\n" + "a=rtcp-fb:96 goog-remb\r\n" +
					"a=rtcp-fb:96 transport-cc\r\n" + "a=rtcp-fb:96 ccm fir\r\n" + "a=rtcp-fb:96 nack\r\n" +
					"a=rtcp-fb:96 nack pli\r\n" + "a=rtpmap:97 rtx/90000\r\n" + "a=fmtp:97 apt=96\r\n" +
					"a=rtpmap:98 VP9/90000\r\n" + "a=rtcp-fb:98 goog-remb\r\n" + "a=rtcp-fb:98 transport-cc\r\n" +
					"a=rtcp-fb:98 ccm fir\r\n" + "a=rtcp-fb:98 nack\r\n" + "a=rtcp-fb:98 nack pli\r\n" +
					"a=fmtp:98 x-google-profile-id=0\r\n" + "a=rtpmap:99 rtx/90000\r\n" + "a=fmtp:99 apt=98\r\n" +
					"a=rtpmap:100 H264/90000\r\n" + "a=rtcp-fb:100 goog-remb\r\n" + "a=rtcp-fb:100 transport-cc\r\n" +
					"a=rtcp-fb:100 ccm fir\r\n" + "a=rtcp-fb:100 nack\r\n" + "a=rtcp-fb:100 nack pli\r\n" +
					"a=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42001f\r\n" +
					"a=rtpmap:101 rtx/90000\r\n" + "a=fmtp:101 apt=100\r\n" + "a=rtpmap:102 H264/90000\r\n" +
					"a=rtcp-fb:102 goog-remb\r\n" + "a=rtcp-fb:102 transport-cc\r\n" + "a=rtcp-fb:102 ccm fir\r\n" +
					"a=rtcp-fb:102 nack\r\n" + "a=rtcp-fb:102 nack pli\r\n" +
					"a=fmtp:102 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42001f\r\n" +
					"a=rtpmap:122 rtx/90000\r\n" + "a=fmtp:122 apt=102\r\n" + "a=rtpmap:127 H264/90000\r\n" +
					"a=rtcp-fb:127 goog-remb\r\n" + "a=rtcp-fb:127 transport-cc\r\n" + "a=rtcp-fb:127 ccm fir\r\n" +
					"a=rtcp-fb:127 nack\r\n" + "a=rtcp-fb:127 nack pli\r\n" +
					"a=fmtp:127 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f\r\n" +
					"a=rtpmap:121 rtx/90000\r\n" + "a=fmtp:121 apt=127\r\n" + "a=rtpmap:125 H264/90000\r\n" +
					"a=rtcp-fb:125 goog-remb\r\n" + "a=rtcp-fb:125 transport-cc\r\n" + "a=rtcp-fb:125 ccm fir\r\n" +
					"a=rtcp-fb:125 nack\r\n" + "a=rtcp-fb:125 nack pli\r\n" +
					"a=fmtp:125 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f\r\n" +
					"a=rtpmap:107 rtx/90000\r\n" + "a=fmtp:107 apt=125\r\n" + "a=rtpmap:108 H264/90000\r\n" +
					"a=rtcp-fb:108 goog-remb\r\n" + "a=rtcp-fb:108 transport-cc\r\n" + "a=rtcp-fb:108 ccm fir\r\n" +
					"a=rtcp-fb:108 nack\r\n" + "a=rtcp-fb:108 nack pli\r\n" +
					"a=fmtp:108 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=4d0032\r\n" +
					"a=rtpmap:109 rtx/90000\r\n" + "a=fmtp:109 apt=108\r\n" + "a=rtpmap:124 H264/90000\r\n" +
					"a=rtcp-fb:124 goog-remb\r\n" + "a=rtcp-fb:124 transport-cc\r\n" + "a=rtcp-fb:124 ccm fir\r\n" +
					"a=rtcp-fb:124 nack\r\n" + "a=rtcp-fb:124 nack pli\r\n" +
					"a=fmtp:124 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=640032\r\n" +
					"a=rtpmap:120 rtx/90000\r\n" + "a=fmtp:120 apt=124\r\n" + "a=rtpmap:123 red/90000\r\n" +
					"a=rtpmap:119 rtx/90000\r\n" + "a=fmtp:119 apt=123\r\n" + "a=rtpmap:114 ulpfec/90000\r\n" +
					"a=ssrc-group:FID 3660616395 2660104768\r\n" + "a=ssrc:3660616395 cname:RKlfDkjBnO/bSV3W\r\n" +
					"a=ssrc:3660616395 msid:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3 587c4eda-4b9a-4d4a-8a88-e06cafe431ec\r\n" +
					"a=ssrc:3660616395 mslabel:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3\r\n" +
					"a=ssrc:3660616395 label:587c4eda-4b9a-4d4a-8a88-e06cafe431ec\r\n" +
					"a=ssrc:2660104768 cname:RKlfDkjBnO/bSV3W\r\n" +
					"a=ssrc:2660104768 msid:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3 587c4eda-4b9a-4d4a-8a88-e06cafe431ec\r\n" +
					"a=ssrc:2660104768 mslabel:ApyEDujjFIASuYUEN3lNvSw4v4uYdXrOMDL3\r\n" +
					"a=ssrc:2660104768 label:587c4eda-4b9a-4d4a-8a88-e06cafe431ec\r\n";

	@Test
	public void testFromSDP() throws Exception {

		SDP sdp = new SDP();

		sdp.fromSDP(sampleSDP);

	}

}
