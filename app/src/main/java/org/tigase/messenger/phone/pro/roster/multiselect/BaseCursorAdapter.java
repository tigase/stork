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

import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseCursorAdapter<V extends RecyclerView.ViewHolder>
		extends RecyclerView.Adapter<V> {

	protected Cursor mCursor;
	private ContentObserver mContentObserver;
	private DataSetObserver mDataSetObserver;
	private boolean mDataValid;
	private int mRowIDColumn;

	public BaseCursorAdapter(Cursor c) {
		setHasStableIds(true);
		swapCursor(c);
	}

	public BaseCursorAdapter() {
		setHasStableIds(true);
		mContentObserver = new DefaultChangeObserver();
		mDataSetObserver = new DefaultDataSetObserver();
	}

	public boolean isDataValid() {
		return mDataValid;
	}

	public Cursor getCursor() {
		return mCursor;
	}

	public abstract void onBindViewHolder(final V holder, final int position, final Cursor cursor);

	@Override
	public void onBindViewHolder(V holder, int position) {
		if (!mDataValid) {
			throw new IllegalStateException("Cannot bind view holder when cursor is in invalid state.");
		}
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException(
					"Could not move cursor to position " + position + " when trying to bind view holder");
		}
		onBindViewHolder(holder, position, mCursor);
	}

	@Override
	public int getItemCount() {
		if (mDataValid) {
			return mCursor.getCount();
		} else {
			return 0;
		}
	}

	@Override
	public long getItemId(int position) {
		if (!mDataValid) {
			throw new IllegalStateException("Cannot lookup item id when cursor is in invalid state.");
		}
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException(
					"Could not move cursor to position " + position + " when trying to get an item id");
		}
		return mCursor.getLong(mRowIDColumn);
	}

	public Cursor getItem(int position) {
		if (!mDataValid) {
			throw new IllegalStateException("Cannot lookup item id when cursor is in invalid state.");
		}
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException(
					"Could not move cursor to position " + position + " when trying to get an item id");
		}
		return mCursor;
	}

	public void swapCursor(Cursor newCursor) {
		if (newCursor == mCursor) {
			return;
		}

		Cursor oldCursor = mCursor;
		if (oldCursor != null) {
			if (mContentObserver != null) {
				oldCursor.unregisterContentObserver(mContentObserver);
			}
			if (mDataSetObserver != null) {
				oldCursor.unregisterDataSetObserver(mDataSetObserver);
			}
			oldCursor.close();
		}

		if (newCursor != null) {
			mCursor = newCursor;
			if (mContentObserver != null) {
				newCursor.registerContentObserver(mContentObserver);
			}
			if (mDataSetObserver != null) {
				newCursor.registerDataSetObserver(mDataSetObserver);
			}
			mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
			mDataValid = true;
			// notify the observers about the new cursor
			notifyDataSetChanged();
		} else {
			notifyItemRangeRemoved(0, getItemCount());
			mCursor = null;
			mRowIDColumn = -1;
			mDataValid = false;
		}
	}

	protected Long getKey(Cursor cursor) {
		return cursor.getLong(mRowIDColumn);
	}

	protected void onContentChanged() {
	}

	private class DefaultChangeObserver
			extends ContentObserver {

		public DefaultChangeObserver() {
			super(new Handler());
		}

		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}

		@Override
		public void onChange(boolean selfChange) {
			onContentChanged();
		}
	}

	private class DefaultDataSetObserver
			extends DataSetObserver {

		@Override
		public void onChanged() {
			mDataValid = true;
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			mDataValid = false;
			// notifyDataSetInvalidated();
			notifyItemRangeRemoved(0, getItemCount());
		}
	}
}
