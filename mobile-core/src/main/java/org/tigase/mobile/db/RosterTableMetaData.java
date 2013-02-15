package org.tigase.mobile.db;

import android.provider.BaseColumns;

public class RosterTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.rosteritem";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.roster";

	public static final String FIELD_ACCOUNT = "account";

	public static final String FIELD_ASK = "ask";

	// public static final String FIELD_AVATAR = "avatar";

	public static final String FIELD_DISPLAY_NAME = "display";

	public static final String FIELD_GROUP_NAME = "group";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_NAME = "name";

	public static final String FIELD_PRESENCE = "presence";

	public static final String FIELD_STATUS_MESSAGE = "status";

	public static final String FIELD_SUBSCRIPTION = "subscription";

	public static final String GROUP_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.groupitem";

	public static final String GROUPS_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.groups";

}
