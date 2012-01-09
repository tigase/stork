package org.tigase.mobile.db;

//import android.provider.BaseColumns;

/**
 * Class left as container for constant keys used for working with accounts
 *
 */
public class AccountsTableMetaData /*implements BaseColumns*/ {

	public static final String ACCOUNT_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.account";

	public static final String ACCOUNTS_LIST_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.account";

	public static final String FIELD_ACTIVE = "active";

	public static final String FIELD_HOSTNAME = "hostname";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_NICKNAME = "nickname";

	public static final String FIELD_PASSWORD = "password";

	public static final String FIELD_PORT = "port";

	public static final String FIELD_ROSTER_VERSION = "roster_version";

	public static final String TABLE_NAME = "accounts";

}
