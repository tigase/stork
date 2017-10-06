package org.tigase.messenger.phone.pro.selectionview;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;

public abstract class MultiSelectFragment
		extends android.support.v4.app.Fragment {

	protected final MultiSelector mMultiSelector = new MultiSelector();
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

	protected abstract boolean onActionItemClicked(ActionMode mode, MenuItem item);

	protected abstract boolean onCreateActionMode(ActionMode actionMode, Menu menu);

	public void startActionMode() {
		((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);

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

	protected abstract void updateActionMode(ActionMode actionMode);
}
