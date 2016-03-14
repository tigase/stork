package org.tigase.messenger.phone.pro;

import android.app.Application;
import android.content.UriMatcher;
import android.net.Uri;
import android.test.ApplicationTestCase;

import junit.framework.Assert;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
        final UriMatcher sUriMatcher = new UriMatcher(1);

        final String AUTHORITY = "org.tigase.messenger.phone.pro.Roster";

        sUriMatcher.addURI(AUTHORITY, "roster", 2);
        sUriMatcher.addURI(AUTHORITY, "roster/#", 3);
        sUriMatcher.addURI(AUTHORITY, "roster/*", 4);


        Assert.assertEquals(2, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster")));
        Assert.assertEquals(3, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/1")));
        Assert.assertEquals(4, sUriMatcher.match(Uri.parse("content://org.tigase.messenger.phone.pro.Roster/roster/dupa")));

    }
}