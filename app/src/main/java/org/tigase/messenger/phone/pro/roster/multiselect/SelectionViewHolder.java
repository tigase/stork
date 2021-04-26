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
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

public abstract class SelectionViewHolder
		extends RecyclerView.ViewHolder {

	private ItemDetailsLookup.ItemDetails<Long> itemDetails;

	public SelectionViewHolder(@NonNull @NotNull View itemView) {
		super(itemView);
	}

	public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
		return itemDetails;
	}

	void createItemDetails(int adapterPosition, Long id) {
		this.itemDetails = new ItemDetailsLookup.ItemDetails<Long>() {
			@Override
			public int getPosition() {
				return adapterPosition;
			}

			@Override
			public @NotNull Long getSelectionKey() {
				return id;
			}

			@Override
			public boolean inSelectionHotspot(@NonNull @NotNull MotionEvent e) {
				return false;
			}
		};
	}

	protected abstract void bind(final int adapterPosition, final Cursor cursor, final boolean selected);

}
