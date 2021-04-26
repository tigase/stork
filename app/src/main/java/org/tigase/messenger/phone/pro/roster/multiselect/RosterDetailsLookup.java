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

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

public class RosterDetailsLookup
		extends ItemDetailsLookup<Long> {

	private final RecyclerView recyclerView;

	public RosterDetailsLookup(RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
	}

	@Nullable
	@org.jetbrains.annotations.Nullable
	@Override
	public ItemDetails<Long> getItemDetails(@NonNull @NotNull MotionEvent event) {
		View view = recyclerView.findChildViewUnder(event.getX(), event.getY());
		if (view != null) {
			return ((SelectionViewHolder) (recyclerView).getChildViewHolder(view)).getItemDetails();
		}
		return null;
	}
}
