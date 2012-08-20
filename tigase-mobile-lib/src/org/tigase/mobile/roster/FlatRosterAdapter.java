package org.tigase.mobile.roster;

import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;

public class FlatRosterAdapter extends SimpleCursorAdapter {
	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE };
	private final static int[] names = new int[] { R.id.roster_item_jid };
	private int layoutId = 0;

	protected int[] mFrom;

	public FlatRosterAdapter(Context context, Cursor c, int layoutId) {
		// super(context, R.layout.roster_item, c, cols, names);
		super(context, layoutId, c, cols, names);
		this.layoutId = layoutId;
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// GroupRosterAdapter and FlatRosterAdapter are using code
		if (layoutId == R.layout.roster_grid_item) {
			view.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}

		RosterAdapterHelper.bindView(view, context, cursor);
	}

	private void findColumns(String[] from, Cursor mCursor) {
		int i;
		int count = from.length;
		if (mFrom == null) {
			mFrom = new int[count];
		}
		if (mCursor != null) {
			for (i = 0; i < count; i++) {
				mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
			}
		} else {
			for (i = 0; i < count; i++) {
				mFrom[i] = -1;
			}
		}
	}

}
