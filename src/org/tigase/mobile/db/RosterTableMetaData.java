package org.tigase.mobile.db;

import android.provider.BaseColumns;

public class RosterTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.rosteritem";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.roster";

	public static final String FIELD_ASK = "ask";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_NAME = "name";

	public static final String FIELD_SUBSCRIPTION = "subscription";

	public static final String TABLE_NAME = "roster";

}
