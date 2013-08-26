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

public class GeolocationTableMetaData implements BaseColumns {

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
}
