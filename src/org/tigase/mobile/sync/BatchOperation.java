package org.tigase.mobile.sync;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

final public class BatchOperation {

    private final String TAG = "BatchOperation";

    private final ContentResolver mResolver;

    private final ArrayList<ContentProviderOperation> mOperations;

    public BatchOperation(Context context, ContentResolver resolver) {
        mResolver = resolver;
        mOperations = new ArrayList<ContentProviderOperation>();
    }

    public int size() {
        return mOperations.size();
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
            ContentProviderResult[] results = mResolver.applyBatch(ContactsContract.AUTHORITY,
                    mOperations);
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
}
