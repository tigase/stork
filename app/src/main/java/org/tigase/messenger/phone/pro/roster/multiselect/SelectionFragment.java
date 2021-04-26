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
package org.tigase.messenger.phone.pro.roster.multiselect;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

public abstract class SelectionFragment<AD extends SelectionAdapter<?>>
		extends Fragment {

	private ActionMode actionMode;
	private AD adapter;
	private RecyclerView recyclerView;
	private SelectionTracker<Long> selectionTracker;
	private StableIdKeyProvider stableIdKeyProvider;

	public SelectionFragment() {
	}

	public SelectionFragment(int contentLayoutId) {
		super(contentLayoutId);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		this.adapter = createAdapterInstance();
		this.recyclerView = findRecyclerView(view);
		this.recyclerView.setAdapter(adapter);
		this.stableIdKeyProvider = new StableIdKeyProvider(recyclerView);
		this.selectionTracker = createSelectionTracker();
		this.adapter.setSelectionTracker(selectionTracker);
		this.selectionTracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
			@Override
			public void onSelectionChanged() {
				doOnSelectionChange();
			}
		});
	}

	public Selection<Long> getSelection() {
		return selectionTracker.getSelection();
	}

	@Override
	public void onDestroy() {
		if (recyclerView != null) {
			recyclerView.setAdapter(null);
		}
		if (adapter != null) {
			adapter.swapCursor(null);
		}
		super.onDestroy();
	}

	public RecyclerView getRecyclerView() {
		return recyclerView;
	}

	protected StableIdKeyProvider getStableIdKeyProvider() {
		return stableIdKeyProvider;
	}

	protected AD getAdapter() {
		return adapter;
	}

	protected ActionMode.Callback getActionModeCallback() {
		return null;
	}

	protected ActionMode getActionMode() {
		return actionMode;
	}

	protected void doOnSelectionChange() {
		if (selectionTracker.getSelection().isEmpty() && actionMode != null) {
			this.actionMode.finish();
			actionMode = null;
		} else if (actionMode == null) {
			final ActionMode.Callback cb = getActionModeCallback();
			if (cb == null) {
				return;
			}
			this.actionMode = requireActivity().startActionMode(new ActionMode.Callback() {
				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					return cb.onCreateActionMode(mode, menu);
				}

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return cb.onPrepareActionMode(mode, menu);
				}

				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					return cb.onActionItemClicked(mode, item);
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					cb.onDestroyActionMode(mode);
					selectionTracker.clearSelection();
				}
			});
		}
	}

	protected @NotNull SelectionTracker<Long> createSelectionTracker() {
		SelectionTracker.SelectionPredicate<Long> selectionPredicate = new SelectionTracker.SelectionPredicate<Long>() {
			@Override
			public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
				return key >= 0;
			}

			@Override
			public boolean canSetStateAtPosition(int position, boolean nextState) {
				return true;
			}

			@Override
			public boolean canSelectMultiple() {
				return true;
			}
		};
		return new SelectionTracker.Builder<>("tracker-" + getClass().getSimpleName(), recyclerView,
											  stableIdKeyProvider, new RosterDetailsLookup(recyclerView),
											  StorageStrategy.createLongStorage()).withSelectionPredicate(
				selectionPredicate).build();
	}

	abstract protected @NotNull RecyclerView findRecyclerView(@NotNull View view);

	abstract protected @NotNull AD createAdapterInstance();
}
