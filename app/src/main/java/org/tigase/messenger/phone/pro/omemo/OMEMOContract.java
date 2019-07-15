/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.omemo;

import android.provider.BaseColumns;

public class OMEMOContract {

	public static abstract class Identities
			implements BaseColumns {

		public final static int TRUST_UNTRUSTED = 0;
		public final static int TRUST_TRUSTED = 1;
		public final static int TRUST_ULTIMATE = 5;

		public final static String TABLE_NAME = "omemo_identities";
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_ACTIVE = "active";
		public static final String FIELD_FINGERPRINT = "fingerprint";
		public static final String FIELD_TRUST = "trust";
		public static final String FIELD_KEY = "key_data";
		public static final String FIELD_JID = "jid";
		public static final String FIELD_DEVICE_ID = "device_id";
		public static final String FIELD_LAST_USAGE = "last_usage_timestamp";
		public static final String FIELD_HAS_KEYPAIR = "contains_keypair";
	}

	public static abstract class PreKeys
			implements BaseColumns {

		public final static String TABLE_NAME = "omemo_prekeys";
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_ID = _ID;
		public static final String FIELD_KEY = "keydata";

	}

	public static abstract class Sessions
			implements BaseColumns {

		public final static String TABLE_NAME = "omemo_sessions";
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_JID = "jid";
		public static final String FIELD_DEVICE_ID = "device_id";
		public static final String FIELD_KEY = "key_data";
	}

	public static abstract class SignedPreKeys
			implements BaseColumns {

		public final static String TABLE_NAME = "omemo_signed_prekeys";
		public static final String FIELD_ACCOUNT = "account";
		public static final String FIELD_ID = _ID;
		public static final String FIELD_KEY = "key_data";

	}

}
