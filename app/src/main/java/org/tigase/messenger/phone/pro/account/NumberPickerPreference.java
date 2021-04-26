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
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class NumberPickerPreference
		extends DialogPreference {

	public static final int MAX_VALUE = 256;
	public static final int MIN_VALUE = 0;
	public static final int DEF_VALUE = 0;
	private int value = DEF_VALUE;

	public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NumberPickerPreference(Context context) {
		super(context);
	}

	@Override
	public CharSequence getSummary() {
		return String.valueOf(value);
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
		notifyChanged();
	}

	public static class NumberPickerPreferenceDialog
			extends PreferenceDialogFragmentCompat {

		private NumberPicker numberPicker;

		public static NumberPickerPreferenceDialog newInstance(String key) {
			final NumberPickerPreferenceDialog fragment = new NumberPickerPreferenceDialog();
			final Bundle bundle = new Bundle(1);
			bundle.putString(ARG_KEY, key);
			fragment.setArguments(bundle);
			return fragment;
		}

		@Override
		public void onDialogClosed(boolean positiveResult) {
			if (positiveResult) {
				numberPicker.clearFocus();
				int newValue = numberPicker.getValue();
				if (getPreference().callChangeListener(newValue)) {
					((NumberPickerPreference) getPreference()).setValue(newValue);
					getPreference().getSummary();
				}
			}
		}

		@Override
		protected void onBindDialogView(View view) {
			super.onBindDialogView(view);
			numberPicker.setValue(((NumberPickerPreference) getPreference()).getValue());
		}

		@Override
		protected View onCreateDialogView(Context context) {
			this.numberPicker = new NumberPicker(context);
			this.numberPicker.setMinValue(MIN_VALUE);
			this.numberPicker.setMaxValue(MAX_VALUE);
			this.numberPicker.setWrapSelectorWheel(true);
			return numberPicker;
		}
	}

}

