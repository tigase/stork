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
package org.tigase.mobile.sync;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

final public class BatchOperation {

	private final ArrayList<ContentProviderOperation> mOperations;

	private final ContentResolver mResolver;

	private final String TAG = "BatchOperation";

	public BatchOperation(Context context, ContentResolver resolver) {
		mResolver = resolver;
		mOperations = new ArrayList<ContentProviderOperation>();
	}

	public void add(ContentProviderOperation cpo) {
		mOperations.add(cpo);
	}

	public Uri execute() {
		Uri result = null;

		if (mOperations.size() == 0) {
			return result;
		}
		// Apply the mOperations to the content provider
		try {
			ContentProviderResult[] results = mResolver.applyBatch(ContactsContract.AUTHORITY, mOperations);
			if ((results != null) && (results.length > 0))
				result = results[0].uri;
		} catch (final OperationApplicationException e1) {
			Log.e(TAG, "storing contact data failed", e1);
		} catch (final RemoteException e2) {
			Log.e(TAG, "storing contact data failed", e2);
		}
		mOperations.clear();
		return result;
	}

	public int size() {
		return mOperations.size();
	}
}
