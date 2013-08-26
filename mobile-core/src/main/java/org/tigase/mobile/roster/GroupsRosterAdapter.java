/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
			RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_STATUS_MESSAGE /*
																						 * ,
																						 * RosterTableMetaData
																						 * .
																						 * FIELD_AVATAR
																						 */};
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
