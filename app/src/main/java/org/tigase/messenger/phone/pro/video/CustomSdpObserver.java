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
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Webrtc_Step2
 * Created by vivek-3102 on 11/03/17.
 */

class CustomSdpObserver
		implements SdpObserver {

	private String tag;

	CustomSdpObserver(String logTag) {
		tag = this.getClass().getCanonicalName();
		this.tag = this.tag + " " + logTag;
	}

	@Override
	public void onCreateSuccess(SessionDescription sessionDescription) {
		Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
	}

	@Override
	public void onSetSuccess() {
		Log.d(tag, "onSetSuccess() called");
	}

	@Override
	public void onCreateFailure(String s) {
		Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
	}

	@Override
	public void onSetFailure(String s) {
		Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
	}

}
