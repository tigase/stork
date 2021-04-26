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

import android.database.Cursor;
import android.view.View;
import androidx.recyclerview.selection.SelectionTracker;

public abstract class SelectionAdapter<V extends SelectionViewHolder>
		extends BaseCursorAdapter<V> {

	private OnItemClickListener<V> onItemClickListener;
	private SelectionTracker<Long> selectionTracker;

	public OnItemClickListener<V> getOnItemClickListener() {
		return onItemClickListener;
	}

	public void setOnItemClickListener(OnItemClickListener<V> onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
		this.selectionTracker = selectionTracker;
	}

	@Override
	public void onBindViewHolder(V holder, int position, Cursor cursor) {
		long id = getKey(cursor);
		boolean selected = selectionTracker != null && selectionTracker.isSelected(id);
		holder.createItemDetails(position, id);
		holder.bind(position, cursor, selected);
		if (onItemClickListener != null) {
			holder.itemView.setOnClickListener(new ClickListenerWrapper(id, holder, onItemClickListener));
		} else {
			holder.itemView.setOnClickListener(null);
		}
	}

	public interface OnItemClickListener<V extends SelectionViewHolder> {

		void onClick(View v, V item);

	}

	private static class ClickListenerWrapper<V extends SelectionViewHolder>
			implements View.OnClickListener {

		private final V holder;
		private final long id;
		private final OnItemClickListener<V> onItemClickListener;

		public ClickListenerWrapper(long id, V holder, OnItemClickListener<V> onItemClickListener) {
			this.id = id;
			this.holder = holder;
			this.onItemClickListener = onItemClickListener;
		}

		@Override
		public void onClick(View v) {
			onItemClickListener.onClick(v, holder);
		}
	}
}
