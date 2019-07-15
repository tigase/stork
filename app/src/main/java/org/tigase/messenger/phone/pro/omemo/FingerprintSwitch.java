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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import org.tigase.messenger.phone.pro.R;

public class FingerprintSwitch
		extends FingerprintView {

	private Switch switcher;

	public FingerprintSwitch(Context context) {
		super(context);
	}

	public FingerprintSwitch(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public FingerprintSwitch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setChecked(boolean checked) {
		this.switcher.setChecked(checked);
	}

	public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
		this.switcher.setOnCheckedChangeListener(listener);
	}

	@Override
	protected View inflateView(LayoutInflater inflater) {
		return inflater.inflate(R.layout.fingerprint_switch, this, true);
	}

	@Override
	protected void doSomethingWithView(Context context, LayoutInflater inflater, View view) {
		super.doSomethingWithView(context, inflater, view);
		this.switcher = view.findViewById(R.id.switcher);
	}
}
