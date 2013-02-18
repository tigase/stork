package org.tigase.mobile.sync;

import org.tigase.mobile.Constants;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
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
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.util.Log;

public class ContactOperations {

	private static final String TAG = "ContactOperations";

	private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
		if (isSyncOperation) {
			return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
		}
		return uri;
	}

	public static ContactOperations createNewContact(Context context, long userId, String accountName, boolean isSyncOperation,
			BatchOperation batchOperation) {
		return new ContactOperations(context, userId, accountName, isSyncOperation, batchOperation);
	}

	public static ContentProviderOperation.Builder newDeleteCpo(Uri uri, boolean isSyncOperation, boolean isYieldAllowed) {
		return ContentProviderOperation.newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation)).withYieldAllowed(
				isYieldAllowed);
	}

	public static ContentProviderOperation.Builder newInsertCpo(Uri uri, boolean isSyncOperation, boolean isYieldAllowed) {
		return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation)).withYieldAllowed(
				isYieldAllowed);
	}

	public static ContentProviderOperation.Builder newUpdateCpo(Uri uri, boolean isSyncOperation, boolean isYieldAllowed) {
		return ContentProviderOperation.newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation)).withYieldAllowed(
				isYieldAllowed);
	}

	public static void syncStatus(Context context, String account, long rawContactId, BareJID buddyJid,
			tigase.jaxmpp.core.client.xmpp.stanzas.Presence p, BatchOperation batchOperation) {
		try {
			String status = null;
			int state = StatusUpdates.OFFLINE;

			if (p != null) {
				state = StatusUpdates.AVAILABLE;

				if (p.getShow() == tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show.away) {
					state = StatusUpdates.AWAY;
				} else if (p.getShow() == tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show.xa) {
					state = StatusUpdates.IDLE;
				} else if (p.getShow() == tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show.dnd) {
					state = StatusUpdates.DO_NOT_DISTURB;
				}

				status = p.getStatus();
				if (status != null) {
					status = EscapeUtils.unescape(status);
				}
			}

			final ContentValues values = new ContentValues();

			// this should fix use of incorrect contact
			// or it will create problem - insert failure
			// values.put(StatusUpdates.DATA_ID, rawContactId);
			values.put(StatusUpdates.PRESENCE, state);
			values.put(StatusUpdates.STATUS, status);
			values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_JABBER);

			values.put(StatusUpdates.IM_ACCOUNT, account);
			values.put(StatusUpdates.IM_HANDLE, buddyJid.toString());
			values.put(StatusUpdates.STATUS_RES_PACKAGE, context.getPackageName());
			values.put(StatusUpdates.STATUS_ICON, R.drawable.icon);
			values.put(StatusUpdates.STATUS_LABEL, R.string.app_name);

			batchOperation.add(ContactOperations.newInsertCpo(StatusUpdates.CONTENT_URI, true, true).withValues(values).build());
		} catch (XMLException e) {
			Log.e(TAG, "WTF??", e);
		}
	}

	public static ContactOperations updateExistingContact(Context context, long rawContactId, boolean isSyncOperation,
			BatchOperation batchOperation) {
		return new ContactOperations(context, rawContactId, isSyncOperation, batchOperation);
	}

	private int mBackReference;
	private final BatchOperation mBatchOperation;

	private final Context mContext;

	private boolean mIsNewContact;

	private boolean mIsSyncOperation;

	private boolean mIsYieldAllowed;

	private long mRawContactId;

	private final ContentValues mValues;

	public ContactOperations(Context context, boolean isSyncOperation, BatchOperation batchOperation) {
		mValues = new ContentValues();
		mIsYieldAllowed = true;
		mIsSyncOperation = isSyncOperation;
		mContext = context;
		mBatchOperation = batchOperation;
	}

	public ContactOperations(Context context, long rawContactId, boolean isSyncOperation, BatchOperation batchOperation) {
		this(context, isSyncOperation, batchOperation);
		mIsNewContact = false;
		mRawContactId = rawContactId;
	}

	public ContactOperations(Context context, long userId, String accountName, boolean isSyncOperation,
			BatchOperation batchOperation) {
		this(context, isSyncOperation, batchOperation);
		mBackReference = mBatchOperation.size();
		mIsNewContact = true;
		mValues.put(RawContacts.SOURCE_ID, userId);
		mValues.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		mValues.put(RawContacts.ACCOUNT_NAME, accountName);
		ContentProviderOperation.Builder builder = newInsertCpo(RawContacts.CONTENT_URI, mIsSyncOperation, true).withValues(
				mValues);
		mBatchOperation.add(builder.build());
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

	private void addInsertOp() {

		if (!mIsNewContact) {
			mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
		}
		ContentProviderOperation.Builder builder = newInsertCpo(Data.CONTENT_URI, mIsSyncOperation, mIsYieldAllowed);
		builder.withValues(mValues);
		if (mIsNewContact) {
			builder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
		}
		mIsYieldAllowed = false;
		mBatchOperation.add(builder.build());
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

	private void addUpdateOp(Uri uri) {
		ContentProviderOperation.Builder builder = newUpdateCpo(uri, mIsSyncOperation, mIsYieldAllowed).withValues(mValues);
		mIsYieldAllowed = false;
		mBatchOperation.add(builder.build());
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

	public ContactOperations updateName(Uri uri, String existingFirstName, String existingLastName, String existingFullName,
			String firstName, String lastName, String fullName) {

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

	public ContactOperations updateServerId(long serverId, Uri uri) {
		mValues.clear();
		mValues.put(RawContacts.SOURCE_ID, serverId);
		addUpdateOp(uri);
		return this;
	}

}
