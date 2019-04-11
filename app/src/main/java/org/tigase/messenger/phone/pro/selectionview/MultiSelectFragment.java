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

package org.tigase.messenger.phone.pro.selectionview;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SelectableHolder;

public abstract class MultiSelectFragment
		extends android.support.v4.app.Fragment {

	protected final MultiSelector mMultiSelector = new MultiSelector() {
		@Override
		public void setSelected(SelectableHolder holder, boolean isSelected) {
			if (holder instanceof MultiSelectViewHolder && !((MultiSelectViewHolder) holder).isCanBeSelected()) {
				super.setSelected(holder, false);
				return;
			}
			super.setSelected(holder, isSelected);
		}

		@Override
		public boolean tapSelection(SelectableHolder holder) {
			if (holder instanceof MultiSelectViewHolder && !((MultiSelectViewHolder) holder).isCanBeSelected()) {
				return false;
			}
			return super.tapSelection(holder);
		}
	};
	protected ActionMode actionMode;
	protected ModalMultiSelectorCallback mActionModeCallback = new ModalMultiSelectorCallback(mMultiSelector) {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return MultiSelectFragment.this.onActionItemClicked(mode, item);
		}

		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			super.onCreateActionMode(actionMode, menu);
			boolean r = MultiSelectFragment.this.onCreateActionMode(actionMode, menu);
			if (r) {
				MultiSelectFragment.this.actionMode = actionMode;
			} else {
				MultiSelectFragment.this.actionMode = null;
			}
			return r;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			super.onDestroyActionMode(actionMode);
		}
	};

	public MultiSelector getMultiSelector() {
		return mMultiSelector;
	}

	public ActionMode startActionMode() {
		return ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);

	}

	public ActionMode startActionMode(ActionMode.Callback callback) {
		return ((AppCompatActivity) getActivity()).startSupportActionMode(callback);

	}

	public void stopActionMode() {
		mMultiSelector.setSelectable(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	void updateAction() {
		if (mMultiSelector.getSelectedPositions().isEmpty()) {
			stopActionMode();
		} else if (actionMode != null) {
			updateActionMode(actionMode);
		}
	}

	protected abstract boolean onActionItemClicked(ActionMode mode, MenuItem item);

	protected abstract boolean onCreateActionMode(ActionMode actionMode, Menu menu);

	protected abstract void updateActionMode(ActionMode actionMode);
}
