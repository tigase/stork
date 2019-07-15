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
package org.tigase.messenger.phone.pro.settings;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.omemo.FingerprintView;

public class FingerprintPreference
		extends Preference {

	private byte[] fingerprint;
	private int offset;

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public FingerprintPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FingerprintPreference(Context context) {
		super(context);
		init();
	}

	public void setFingerprint(byte[] fingerprint, int offset) {
		this.fingerprint = fingerprint;
		this.offset = offset;
		notifyChanged();
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		final FingerprintView fpView = view.findViewById(R.id.fingerprint_view);
		fpView.setFingerprint(this.fingerprint, this.offset);
	}

	private void init() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		setLayoutResource(R.layout.preference_fingerprint_item);
	}

}
