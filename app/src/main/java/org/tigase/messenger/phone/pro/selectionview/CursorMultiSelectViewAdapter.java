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
