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
