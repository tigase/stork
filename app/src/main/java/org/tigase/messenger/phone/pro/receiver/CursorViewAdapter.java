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

package org.tigase.messenger.phone.pro.receiver;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;

public class CursorViewAdapter
		extends CursorRecyclerViewAdapter<ViewHolder> {

	private final Context context;
	private final ReceiverContextActivity.OnItemSelected selectionHandler;

	public CursorViewAdapter(Context context, Cursor cursor, ReceiverContextActivity.OnItemSelected selectionHandler) {
		super(cursor);
		this.context = context;
		this.selectionHandler = selectionHandler;
	}

	@Override
	public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
		holder.bind(context, cursor);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_rosteritem, parent, false);
		return new ViewHolder(view, selectionHandler);
	}
}
