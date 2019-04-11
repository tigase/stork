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

package org.tigase.messenger.phone.pro.account;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

/**
 * A {@link android.preference.Preference} that displays a number picker as a dialog.
 */
public class NumberPickerPreference
		extends DialogPreference {

	public static final int MAX_VALUE = 100;
	public static final int MIN_VALUE = 0;
	public static final boolean WRAP_SELECTOR_WHEEL = true;

	private NumberPicker picker;
	private int value;

	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public int getValue() {
		return this.value;
	}

	public void setValue(int value) {
		this.value = value;
		persistInt(this.value);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		picker.setMinValue(MIN_VALUE);
		picker.setMaxValue(MAX_VALUE);
		picker.setWrapSelectorWheel(WRAP_SELECTOR_WHEEL);
		picker.setValue(getValue());
	}

	@Override
	protected View onCreateDialogView() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
																			 ViewGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.gravity = Gravity.CENTER;

		picker = new NumberPicker(getContext());
		picker.setLayoutParams(layoutParams);

		FrameLayout dialogView = new FrameLayout(getContext());
		dialogView.addView(picker);

		return dialogView;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			picker.clearFocus();
			int newValue = picker.getValue();
			if (callChangeListener(newValue)) {
				setValue(newValue);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, MIN_VALUE);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		setValue(restorePersistedValue ? getPersistedInt(MIN_VALUE) : (Integer) defaultValue);
	}
}