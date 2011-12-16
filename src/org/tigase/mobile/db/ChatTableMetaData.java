package org.tigase.mobile.db;

import android.provider.BaseColumns;

public class ChatTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.chatitem";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.chat";

	public static final String FIELD_AUTHOR_JID = "author_jid";

	public static final String FIELD_AUTHOR_NICKNAME = "author_nickname";

	public static final String FIELD_BODY = "body";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	/**
	 * <ul>
	 * <li><code>0</code> - incoming message</li>
	 * <li><code>1</code> - outgoing, not sent</li>
	 * <li><code>2</code> - outgoing, sent</li>
	 * </ul>
	 */
	public static final String FIELD_STATE = "state";

	public static final String FIELD_THREAD_ID = "thread_id";

	public static final String FIELD_TIMESTAMP = "timestamp";

	public final static int STATE_INCOMING = 0;

	public final static int STATE_OUT_NOT_SENT = 1;

	public final static int STATE_OUT_SENT = 2;

	public static final String TABLE_NAME = "chat_history";

}
