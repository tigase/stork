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

import android.database.Cursor;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;

public abstract class CursorMultiSelectViewAdapter<VH extends android.support.v7.widget.RecyclerView.ViewHolder>
		extends CursorRecyclerViewAdapter<VH> {

	private final MultiSelectFragment fragment;

	public CursorMultiSelectViewAdapter(Cursor cursor, MultiSelectFragment fragment) {
		super(cursor);
		this.fragment = fragment;
	}

	public MultiSelectFragment getFragment() {
		return fragment;
	}
}
