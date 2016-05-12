/*
 * ApplicationTest.java
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

package org.tigase.messenger.phone.pro;

import android.app.Application;
import android.content.UriMatcher;
import android.net.Uri;
import android.test.ApplicationTestCase;
import junit.framework.Assert;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing
 * Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
	public ApplicationTest() {
		super(Application.class);
		final UriMatcher sUriMatcher = new UriMatcher(1);

		final String AUTHORITY = "org.tigase.messenger.phone.pro.Roster";

		sUriMatcher.addURI(AUTHORITY, "roster", 2);
		sUriMatcher.addURI(AUTHORITY, "roster/#", 3);
		sUriMatcher.addURI(AUTHORITY, "roster/*", 4);
		sUriMatcher.addURI(AUTHORITY, "roster/*/#", 5);

		Assert.assertEquals(2, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster")));
		Assert.assertEquals(3, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/1")));
		Assert.assertEquals(4, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/dupa")));
		Assert.assertEquals(5, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/dupa/1")));

		Uri uri = Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/dupa/1");
		Assert.assertEquals("1", uri.getLastPathSegment());
		Assert.assertEquals("dupa", uri.getPathSegments().get(uri.getPathSegments().size() - 2));

	}
}