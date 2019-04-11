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

package cz.destil.settleup.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiSpinner
		extends Spinner
		implements DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

	private String defaultText;
	private List<String> items;
	private MultiSpinnerListener listener;
	private boolean[] selected;

	public MultiSpinner(Context context) {
		super(context);
	}

	public MultiSpinner(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
	}

	public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
	}

	public Collection<String> getCheckedItems() {
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i]) {
				result.add(items.get(i));
			}
		}
		return result;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		refresh();
		listener.onItemsSelected(selected);
	}

	@Override
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
		selected[which] = isChecked;
	}

	@Override
	public boolean performClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), selected, this);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setOnCancelListener(this);
		builder.show();
		return true;
	}

	public void setChecked(int pos, boolean checked) {
		selected[pos] = checked;
		refresh();
	}

	public void setItems(List<String> items, String allText, MultiSpinnerListener listener) {
		this.items = items;
		this.defaultText = allText;
		this.listener = listener;

		// all selected by default
		selected = new boolean[items.size()];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = false;
		}
		refresh();
	}

	private void refresh() {
		// refresh text on spinner
		StringBuffer spinnerBuffer = new StringBuffer();
		boolean someUnselected = false;
		for (int i = 0; i < items.size(); i++) {
			if (selected[i] == true) {
				spinnerBuffer.append(items.get(i));
				spinnerBuffer.append(", ");
			} else {
				someUnselected = true;
			}
		}
		String spinnerText;
		if (someUnselected) {
			spinnerText = spinnerBuffer.toString();
			if (spinnerText.length() > 2) {
				spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
			}
		} else {
			spinnerText = defaultText;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
																new String[]{spinnerText});
		setAdapter(adapter);
	}

	public interface MultiSpinnerListener {

		void onItemsSelected(boolean[] selected);
	}
}