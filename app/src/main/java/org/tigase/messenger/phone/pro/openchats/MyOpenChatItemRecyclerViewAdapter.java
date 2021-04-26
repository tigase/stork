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

package org.tigase.messenger.phone.pro.openchats;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionAdapter;

public class MyOpenChatItemRecyclerViewAdapter
		extends SelectionAdapter<OpenChatViewHolder> {

	public MyOpenChatItemRecyclerViewAdapter() {
		super();
	}

	@NonNull
	@Override
	public OpenChatViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_openchatitem, parent, false);
		return new OpenChatViewHolder(view);
	}

	@Override
	public int getItemViewType(int position) {
		if (!isDataValid()) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		if (!getCursor().moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position " + position);
		}

		return getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.OpenChats.FIELD_TYPE));
	}

	@Override
	protected Long getKey(Cursor cursor) {
		return cursor.getLong(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
	}

}
