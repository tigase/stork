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

import android.view.View;
import com.bignerdranch.android.multiselector.SwappingHolder;

public abstract class MultiSelectViewHolder
		extends SwappingHolder
		implements View.OnLongClickListener, View.OnClickListener {

	private final MultiSelectFragment fragment;
	private boolean canBeSelected = true;

	public MultiSelectViewHolder(View itemView, MultiSelectFragment multiSelectFragment) {
		super(itemView, multiSelectFragment.getMultiSelector());
		this.fragment = multiSelectFragment;
		addClickable(itemView);
	}

	public boolean isCanBeSelected() {
		return canBeSelected;
	}

	public void setCanBeSelected(boolean canBeSelected) {
		this.canBeSelected = canBeSelected;
	}

	@Override
	public void onClick(View v) {
		if (fragment.getMultiSelector().isSelectable()) {
			if (isSelectable()) {
				fragment.getMultiSelector().tapSelection(this);
			}
			fragment.updateAction();
		} else {
			onItemClick(v);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (!fragment.getMultiSelector().isSelectable()) {
			fragment.startActionMode();
			fragment.getMultiSelector().setSelectable(true);
			if (isCanBeSelected()) {
				fragment.getMultiSelector().setSelected(this, true);
			}
			fragment.updateAction();
			return true;
		} else if (fragment.getMultiSelector().isSelectable()) {
			if (isCanBeSelected()) {
				fragment.getMultiSelector().tapSelection(this);
			}
			fragment.updateAction();
			return true;
		} else {
			return false;
//			return onItemLongClick(v);
		}
	}

	protected final void addClickable(final View view) {
		if (view != null) {
			view.setLongClickable(true);
			view.setOnLongClickListener(this);
			view.setClickable(true);
			view.setOnClickListener(this);
		}
	}

//	protected abstract boolean onItemLongClick(View v);

	protected abstract void onItemClick(View v);
}
