package org.tigase.mobile.sync;

import org.tigase.mobile.Constants;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

public class ContactOperations {

	private final BatchOperation mBatchOperation;
	private final Context mContext;
	private final ContentValues mValues;
    private boolean mIsSyncOperation;
    private long mRawContactId;
    private int mBackReference;
    private boolean mIsNewContact;
    private boolean mIsYieldAllowed;    
    
	public static ContactOperations createNewContact(Context context, long userId,
            String accountName, boolean isSyncOperation, BatchOperation batchOperation) {
        return new ContactOperations(context, userId, accountName, isSyncOperation, batchOperation);
    }

    public static ContactOperations updateExistingContact(Context context, long rawContactId,
            boolean isSyncOperation, BatchOperation batchOperation) {
        return new ContactOperations(context, rawContactId, isSyncOperation, batchOperation);
    }

    public ContactOperations(Context context, boolean isSyncOperation,
            BatchOperation batchOperation) {
        mValues = new ContentValues();
        mIsYieldAllowed = true;
        mIsSyncOperation = isSyncOperation;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public ContactOperations(Context context, long userId, String accountName,
            boolean isSyncOperation, BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(RawContacts.SOURCE_ID, userId);
        mValues.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        mValues.put(RawContacts.ACCOUNT_NAME, accountName);
        ContentProviderOperation.Builder builder =
                newInsertCpo(RawContacts.CONTENT_URI, mIsSyncOperation, true).withValues(mValues);
        mBatchOperation.add(builder.build());
    }

    public ContactOperations(Context context, long rawContactId, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mIsNewContact = false;
        mRawContactId = rawContactId;
    }

	public ContactOperations addName(String fullName, String firstName, String lastName) {
		mValues.clear();

		if (!TextUtils.isEmpty(fullName)) {
			mValues.put(StructuredName.DISPLAY_NAME, fullName);
			mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		} else {
			if (!TextUtils.isEmpty(firstName)) {
				mValues.put(StructuredName.GIVEN_NAME, firstName);
				mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			}
			if (!TextUtils.isEmpty(lastName)) {
				mValues.put(StructuredName.FAMILY_NAME, lastName);
				mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			}
		}
		if (mValues.size() > 0) {
			addInsertOp();
		}
		return this;
	}

	public ContactOperations addJID(BareJID jid) {
		mValues.clear();

		String jidStr = jid.toString();
		if (!TextUtils.isEmpty(jidStr)) {
			mValues.put(Im.DATA, jidStr);
			mValues.put(Im.LABEL, "IM");
			mValues.put(Im.PROTOCOL, Im.PROTOCOL_JABBER);
			mValues.put(Im.TYPE, Im.TYPE_OTHER);
			mValues.put(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}    

	public ContactOperations addAvatar(byte[] blob) {
		mValues.clear();

		if (blob != null && blob.length > 0) {
			mValues.put(Photo.PHOTO, blob);
			mValues.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}    

    public ContactOperations addGroupMembership(long groupId) {
        mValues.clear();
        mValues.put(GroupMembership.GROUP_ROW_ID, groupId);
        mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }	
    
    public ContactOperations addProfile(String username) {
    	mValues.clear();
    	if (username != null) {
    		mValues.put(ContactsContract.Data.MIMETYPE, Constants.PROFILE_MIMETYPE);
        	mValues.put(ContactsContract.Data.DATA1, username);
        	mValues.put(ContactsContract.Data.DATA2, "XMPP Profile");
        	mValues.put(ContactsContract.Data.DATA3, "View profile");
        	addInsertOp();
    	}
        return this;
    }
    
    public ContactOperations updateName(Uri uri,
			String existingFirstName,
			String existingLastName,
			String existingFullName,
			String firstName,
			String lastName,
			String fullName) {

		mValues.clear();
		if (TextUtils.isEmpty(fullName)) {
			if (!TextUtils.equals(existingFirstName, firstName)) {
				mValues.put(StructuredName.GIVEN_NAME, firstName);
			}
			if (!TextUtils.equals(existingLastName, lastName)) {
				mValues.put(StructuredName.FAMILY_NAME, lastName);
			}
		} else {
			if (!TextUtils.equals(existingFullName, fullName)) {
				mValues.put(StructuredName.DISPLAY_NAME, fullName);
			}
		}
		if (mValues.size() > 0) {
			addUpdateOp(uri);
		}
		return this;
	}    

	public ContactOperations updateAvatar(Uri uri, byte[] blob) {		

		if (blob != null && blob.length > 0) {
			mValues.clear();
			mValues.put(Photo.PHOTO, blob);
			mValues.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			addUpdateOp(uri);
		}
		
		return this;
	}    
	
    public ContactOperations updateGroupMembership(Uri uri, long groupId) {
        mValues.clear();
        mValues.put(GroupMembership.GROUP_ROW_ID, groupId);
        mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        addUpdateOp(uri);
        return this;
    }	
    
    public ContactOperations updateProfile(Uri uri, String username) {
    	mValues.clear();
    	if (username != null) {
    		mValues.put(ContactsContract.Data.MIMETYPE, Constants.PROFILE_MIMETYPE);
        	mValues.put(ContactsContract.Data.DATA1, username);
        	mValues.put(ContactsContract.Data.DATA2, "XMPP Profile");
        	mValues.put(ContactsContract.Data.DATA3, "View profile");
        	addUpdateOp(uri);
    	}
        return this;
    }
    
	private void addInsertOp() {

		if (!mIsNewContact) {
			mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
		}
		ContentProviderOperation.Builder builder =
				newInsertCpo(Data.CONTENT_URI, mIsSyncOperation, mIsYieldAllowed);
		builder.withValues(mValues);
		if (mIsNewContact) {
			builder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
		}
		mIsYieldAllowed = false;
		mBatchOperation.add(builder.build());
	}    

    private void addUpdateOp(Uri uri) {
        ContentProviderOperation.Builder builder =
                newUpdateCpo(uri, mIsSyncOperation, mIsYieldAllowed).withValues(mValues);
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }
    
    public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }
    
    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }
    
    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }    
}
