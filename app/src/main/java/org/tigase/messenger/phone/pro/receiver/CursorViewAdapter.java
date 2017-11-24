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
