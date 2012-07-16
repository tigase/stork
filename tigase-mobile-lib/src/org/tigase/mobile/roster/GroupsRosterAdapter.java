package org.tigase.mobile.roster;

import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.SimpleCursorTreeAdapter;

public class GroupsRosterAdapter extends SimpleCursorTreeAdapter {

	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_STATUS_MESSAGE/*, RosterTableMetaData.FIELD_AVATAR */};
	private final static int[] names = new int[] { R.id.roster_item_jid };

	static Context staticContext;

	private Context context;

	// protected int[] mFrom;

	private int resource;

	public GroupsRosterAdapter(Context context, Cursor c) {
		// Context context, Cursor cursor, int groupLayout, String[] groupFrom,
		// int[] groupTo, int childLayout, String[] childFrom, int[] childTo
		super(context, c, R.layout.roster_group_item, new String[] { RosterTableMetaData.FIELD_GROUP_NAME },
				new int[] { R.id.roster_group_name }, R.layout.roster_item, cols, names);

		this.context = context;
		this.resource = R.layout.roster_item;
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		// GroupRosterAdapter and FlatRosterAdapter are using code
		RosterAdapterHelper.bindView(view, context, cursor);
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		if (context == null)
			context = staticContext;
		String group = groupCursor.getString(1);
		return context.getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, new String[] { group },
				null);
	}
	
}
