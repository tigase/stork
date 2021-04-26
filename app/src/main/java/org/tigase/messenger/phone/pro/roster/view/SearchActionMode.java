/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.roster.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.searchbar.SearchCallback;

public class SearchActionMode
		implements ActionMode.Callback {

	private final Context context;
	private SearchCallback searchCallback;
	private EditText searchField;

	public SearchActionMode(Context context) {
		this.context = context;
	}

	public SearchActionMode(Context context, SearchCallback callback) {
		this(context);
		setSearchCallback(callback);
	}

	public String getSearchText() {
		if (searchField == null) {
			return null;
		} else {
			return searchField.getText().toString();
		}
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode am, Menu menu) {
		View customNav = LayoutInflater.from(context).inflate(R.layout.search_actionbar, null);
		am.setCustomView(customNav);

		this.searchField = customNav.findViewById(R.id.text_search);
		this.searchField.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String t = s.toString();
				if (searchCallback != null) {
					searchCallback.onSearchTextChanged(t == null || t.trim().isEmpty() ? null : t.trim());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});
		searchField.requestFocus();

		InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);

		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		searchField = null;
		if (searchCallback != null) {
			searchCallback.onSearchTextChanged(null);
		}
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	public void setSearchCallback(SearchCallback searchCallback) {
		this.searchCallback = searchCallback;
	}
}
