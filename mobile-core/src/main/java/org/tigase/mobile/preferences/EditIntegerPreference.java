/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

public class EditIntegerPreference extends EditTextPreference {

	public EditIntegerPreference(Context context) {
		super(context);
		getEditText().setKeyListener(DigitsKeyListener.getInstance(true, true));
	}

	public EditIntegerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		getEditText().setKeyListener(DigitsKeyListener.getInstance(true, true));
	}

	public EditIntegerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		getEditText().setKeyListener(DigitsKeyListener.getInstance(true, true));
	}

	@Override
	public String getText() {
		return String.valueOf(getSharedPreferences().getInt(getKey(), Context.MODE_PRIVATE));
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		if (restoreValue)
			getEditText().setText(getText());
		else
			super.onSetInitialValue(restoreValue, defaultValue);
	}

	@Override
	public void setText(String text) {
		getSharedPreferences().edit().putInt(getKey(), Integer.parseInt(text)).commit();
	}
}
