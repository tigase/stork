package org.tigase.mobile.db;

import android.provider.BaseColumns;

public class GeolocationTableMetaData  implements BaseColumns {

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_LON = "lon";
	
	public static final String FIELD_LAT = "lat";
	
	public static final String FIELD_ALT = "alt";
	
	public static final String FIELD_COUNTRY = "country";
	
	public static final String FIELD_LOCALITY = "locality";
	
	public static final String FIELD_STREET = "street";
	
	public static final String TABLE_NAME = "geolocation";
}
