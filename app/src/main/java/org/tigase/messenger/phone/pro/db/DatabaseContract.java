/*
 * DatabaseContract.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.db;

import android.provider.BaseColumns;

public final class DatabaseContract {

	public static final int DATABASE_VERSION = 9;

	public static final String DATABASE_NAME = "mobile_messenger2.db";

	private DatabaseContract() {
	}

	public static class CapsFeatures
			implements BaseColumns {

		public static final String FIELD_FEATURE = "feature";

		public static final String FIELD_ID = "_id";

		public static final String FIELD_NODE = "node";

		public static final String TABLE_NAME = "caps_features";

	}

	public static class CapsIdentities
			implements BaseColumns {

		public static final String FIELD_CATEGORY = "category";

		public static final String FIELD_ID = "_id";

		public static final String FIELD_NAME = "name";

		public static final String FIELD_NODE = "node";

		public static final String FIELD_TYPE = "type";

		public static final String TABLE_NAME = "caps_identities";

	}

	public static abstract class ChatHistory
			implements BaseColumns {

		public static final String TABLE_NAME = "chat_history";
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_AUTHOR_JID = "author_jid";
		public static final String FIELD_AUTHOR_NICKNAME = "author_nickname";
		public static final String FIELD_BODY = "body";
		public static final String FIELD_DATA = "data";
		public static final String FIELD_INTERNAL_CONTENT_URI = "content_uri";
		public static final String FIELD_ID = "_id";
		public static final String FIELD_JID = "jid";
		/**
		 * Description of values for FIELD_CONTACT_PRESENCE (state) <ul> <li><code>0</code> - incoming</li>
		 * <li><code>1</code> - outgoing, sent</li> <li><code>2</code> - incoming unread</li> <li><code>3</code> -
		 * outgoing, not sent</li> <li><code>4</code> - outgoing, delivered</li> </ul>
		 */
		public static final String FIELD_STATE = "state";
		public static final String FIELD_THREAD_ID = "thread_id";
		public static final String FIELD_STANZA_ID = "stanza_id";
		public static final String FIELD_TIMESTAMP = "timestamp";

		/**
		 * Description of values for FIELD_ITEM_TYPE (item_type) <ul> <li><code>0</code> - message</li>
		 * <li><code>1</code> - locality</li> <li><code>2</code> - file</li> <li><code>3</code> - image</li>
		 * <li><code>4</code> - video</li> <li><code>5</code> - error message</li></ul>
		 */
		public static final String FIELD_ITEM_TYPE = "item_type";

		public final static int ITEM_TYPE_MESSAGE = 0;
		public final static int ITEM_TYPE_LOCALITY = 1;
		public final static int ITEM_TYPE_FILE = 2;
		public final static int ITEM_TYPE_IMAGE = 3;
		public final static int ITEM_TYPE_VIDEO = 4;
		public final static int ITEM_TYPE_ERROR = 5;

		public static final String FIELD_CHAT_TYPE = "chat_type";

		/**
		 * One-to-one chat.
		 */
		public final static int CHAT_TYPE_P2P = 0;
		public final static int CHAT_TYPE_MUC = 1;
		public final static int CHAT_TYPE_MIX = 2;

		/**
		 * Incoming message or object.
		 */
		public final static int STATE_INCOMING = 0;
		/**
		 * Outgoing message or object. Sent.
		 */
		public final static int STATE_OUT_SENT = 1;
		/**
		 * Incoming message or object. Unread.
		 */
		public final static int STATE_INCOMING_UNREAD = 2;
		/**
		 * Outgoing message or object. Not sent.
		 */
		public final static int STATE_OUT_NOT_SENT = 3;
		/**
		 * Outgoing message or object. Delivered to recipient.
		 */
		public final static int STATE_OUT_DELIVERED = 4;

		/**
		 * index name
		 */
		public static final String INDEX_JID = "chat_history_jid_index";
		public static final String INDEX_STATE = "chat_history_state_index";

		public static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" + FIELD_ID + " INTEGER PRIMARY KEY, " + FIELD_ACCOUNT + " TEXT, " +
						FIELD_THREAD_ID + " TEXT, " + FIELD_JID + " TEXT, " + FIELD_AUTHOR_JID + " TEXT, " +
						FIELD_AUTHOR_NICKNAME + " TEXT, " + FIELD_TIMESTAMP + " DATETIME, " + FIELD_BODY + " TEXT, " +
						FIELD_ITEM_TYPE + " INTEGER, " + FIELD_CHAT_TYPE + " INTEGER, " + FIELD_DATA + " TEXT, " +
						FIELD_INTERNAL_CONTENT_URI + " TEXT, " + FIELD_STANZA_ID + " TEXT, " + FIELD_STATE +
						" INTEGER" + ");";

		public static final String CREATE_INDEX_JID =
				"CREATE INDEX IF NOT EXISTS " + INDEX_JID + " ON " + TABLE_NAME + " (" + FIELD_ACCOUNT + ", " +
						FIELD_JID + ")";

		public static final String CREATE_INDEX_STATE =
				"CREATE INDEX IF NOT EXISTS " + INDEX_STATE + " ON " + TABLE_NAME + " (" + FIELD_STATE + ")";

		public static final String CHATS_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.chats";
		public static final String CHATS_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.chatitem";

	}

	public static abstract class OpenChats
			implements BaseColumns {

		// common
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_ID = "_id";
		public static final String FIELD_JID = "jid";

		/**
		 * <ul> <li><code>0</code> - single user chat</li> <li><code>1</code> - multi user chat</li> </ul>
		 */
		public static final String FIELD_TYPE = "type";

		// for chat
		public static final String FIELD_RESOURCE = "resource";
		public static final String FIELD_THREAD_ID = "thread_id";

		// for muc
		public static final String FIELD_NICKNAME = "nickname";
		public static final String FIELD_PASSWORD = "password";
		public static final String FIELD_ROOM_STATE = "room_state";

		// common
		public static final String FIELD_TIMESTAMP = "timestamp";

		public static final String TABLE_NAME = "open_chats";

		public static final int TYPE_CHAT = 0;
		public static final int TYPE_MUC = 1;

		public static final String OPENCHATS_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.openchats";
		public static final String OPENCHAT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.openchatitem";
	}

	public static abstract class RosterGroupsCache
			implements BaseColumns {

		public static final String FIELD_ID = "_id";
		public static final String FIELD_NAME = "name";
		public static final String TABLE_NAME = "roster_groups";

		private static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" + FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + FIELD_NAME +
						" TEXT NOT NULL" + ");";
	}

	public static abstract class RosterItemsCache
			implements BaseColumns {

		public static final String FIELD_ACCOUNT = "account";

		public static final String FIELD_ASK = "ask";

		public static final String FIELD_ID = "_id";

		public static final String FIELD_JID = "jid";

		public static final String FIELD_NAME = "name";

		public static final String FIELD_SUBSCRIPTION = "subscription";

		public static final String FIELD_TIMESTAMP = "timestamp";

		public static final String TABLE_NAME = "roster_items";

		/**
		 * Additional field to keep status of buddy from roster item to speed up searches of online users <ul>
		 * <li><code>0</code> - unavailable</li> <li><code>1</code> - error</li> <li><code>5</code> - dnd</li>
		 * <li><code>10</code> - xa</li> <li><code>15</code> - away</li> <li><code>20</code> - available</li>
		 * <li><code>25</code> - chat</li> </ul>
		 */
		public static final String FIELD_STATUS = "status";

		public static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" + FIELD_ID + " INTEGER PRIMARY KEY, " + FIELD_ACCOUNT + " TEXT, " +
						FIELD_JID + " TEXT, " + FIELD_NAME + " TEXT, " + FIELD_ASK + " BOOLEAN, " + FIELD_SUBSCRIPTION +
						" TEXT, " + FIELD_TIMESTAMP + " DATETIME" + FIELD_STATUS + " INTEGER, " + ");";

		public static final String ROSTER_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.roster";
		public static final String ROSTER_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.rosteritem";
	}

	public static class RosterItemsGroups
			implements BaseColumns {

		public static final String FIELD_ITEM = "item_id";

		public static final String FIELD_GROUP = "group_id";

		public static final String TABLE_NAME = "roster_items_groups";

	}

	public static abstract class RosterItemsGroupsCache
			implements BaseColumns {

		public static final String FIELD_ITEM = "item_id";
		public static final String FIELD_GROUP = "group_id";
		public static final String TABLE_NAME = "roster_items_groups";
		private static final String CREATE_TABLE =
				"CREATE TABLE " + RosterItemsGroupsCache.TABLE_NAME + " (" + RosterItemsGroupsCache.FIELD_ITEM +
						" INTEGER, " + RosterItemsGroupsCache.FIELD_GROUP + " INTEGER, " + "FOREIGN KEY(" +
						RosterItemsGroupsCache.FIELD_ITEM + ") REFERENCES " + RosterItemsCache.TABLE_NAME + "(" +
						RosterItemsCache.FIELD_ID + ")," + "FOREIGN KEY(" + RosterItemsGroupsCache.FIELD_GROUP +
						") REFERENCES " + RosterGroupsCache.TABLE_NAME + "(" + RosterGroupsCache.FIELD_ID + ")" + ");";
	}

	public static abstract class VCardsCache
			implements BaseColumns {

		public static final String FIELD_DATA = "data";
		public static final String FIELD_HASH = "hash";
		public static final String FIELD_ID = "_id";
		public static final String FIELD_JID = "jid";
		public static final String FIELD_TIMESTAMP = "subscription";
		public static final String INDEX_JID = "vcards_cache_jid_index";
		public static final String TABLE_NAME = "vcards_cache";

		public static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" + FIELD_ID + " INTEGER PRIMARY KEY, " + FIELD_JID + " TEXT, " +
						FIELD_HASH + " TEXT, " + FIELD_DATA + " BLOB, " + FIELD_TIMESTAMP + " DATETIME" + ");";

		public static final String CREATE_INDEX =
				"CREATE INDEX IF NOT EXISTS " + INDEX_JID + " ON " + TABLE_NAME + " (" + FIELD_JID + ")";
		public static final String VCARD_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.vcard";

	}

	public class Geolocation
			implements BaseColumns {

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.geoloc";
		public static final String FIELD_ALT = "alt";
		public static final String FIELD_COUNTRY = "country";
		public static final String FIELD_ID = "_id";
		public static final String FIELD_JID = "jid";
		public static final String FIELD_LAT = "lat";
		public static final String FIELD_LOCALITY = "locality";
		public static final String FIELD_LON = "lon";
		public static final String FIELD_STREET = "street";
		public static final String INDEX_JID = "geolocation_jid_index";
		public static final String TABLE_NAME = "geolocation";

		public static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " (" + FIELD_ID + " INTEGER PRIMARY KEY, " + FIELD_JID + " TEXT, " +
						FIELD_LON + " REAL, " + FIELD_LAT + " REAL, " + FIELD_ALT + " REAL, " + FIELD_COUNTRY +
						" TEXT, " + FIELD_LOCALITY + " TEXT, " + FIELD_STREET + " TEXT " + ");";

		public static final String CREATE_INDEX =
				"CREATE INDEX IF NOT EXISTS " + INDEX_JID + " ON " + TABLE_NAME + " (" + FIELD_JID + ")";
	}
}
