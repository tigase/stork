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
package org.tigase.messenger.phone.pro.omemo;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;
import tigase.jaxmpp.core.client.Hex;

public class FingerprintView
		extends RelativeLayout {

	public final static int GROUP_SIZE = 8;

	protected String fingerprint;
	protected TextView fpTextView;

	public FingerprintView(Context context) {
		this(context, null);
	}

	public FingerprintView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FingerprintView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setGravity(Gravity.CENTER_VERTICAL);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflateView(inflater);
		doSomethingWithView(context, inflater, view);
	}

	public void setFingerprint(byte[] fingerprint, int offset) {
		if (fingerprint == null) {
			setFingerprint(null);
		} else {
			setFingerprint(Hex.encode(fingerprint, offset));
		}
	}

	public void setFingerprint(String fingerprint) {
//		this.fingerprint = fingerprint;
		if (fpTextView != null) {
			if (fingerprint == null) {
				fpTextView.setText("");
			} else {
				fpTextView.setText(Hex.format(fingerprint, GROUP_SIZE));
			}
		}
	}

	protected View inflateView(LayoutInflater inflater) {
		return inflater.inflate(R.layout.fingerprint_view, this, true);
	}

	protected void doSomethingWithView(Context context, LayoutInflater inflater, View view) {
		fpTextView = view.findViewById(R.id.fingerprint);
	}

}
