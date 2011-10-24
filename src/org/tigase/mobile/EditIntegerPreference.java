package org.tigase.mobile;

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
