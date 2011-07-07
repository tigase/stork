package org.tigase.mobile.db.providers;

import org.tigase.mobile.db.RosterTableMetaData;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.net.Uri;

public abstract class AbstractRosterProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.mobile.db.providers.RosterProvider";

	public static final String CONTENT_URI = "content://" + AUTHORITY + "/roster";

	protected static final int ROSTER_ITEM_URI_INDICATOR = 2;

	protected static final int ROSTER_URI_INDICATOR = 1;

	protected final UriMatcher uriMatcher;

	public AbstractRosterProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "roster", ROSTER_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "roster/*", ROSTER_ITEM_URI_INDICATOR);

	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ROSTER_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_TYPE;
		case ROSTER_ITEM_URI_INDICATOR:
			return RosterTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

}
