package org.tigase.mobile.sync;

import java.util.Date;

import org.tigase.mobile.Constants;
import org.tigase.mobile.db.RosterCacheTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.BareJID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";
    
    private static final String SYNC_MARKER_KEY = "org.tigase.mobile.sync.marker";
    
	private final Context context;

	private final AccountManager accountManager;
	
	private final MessengerDatabaseHelper dbHelper; 
	
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);

        this.context = context;
        this.dbHelper = new MessengerDatabaseHelper(getContext());
        this.accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
	        ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, "starting contacts synchronization for account = "+account.name);

		long newMarker = new Date().getTime();
		long oldMarker = getSyncMarker(account);
//		final MultiJaxmpp multiJaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();

//		BareJID bareAccountJid = BareJID.bareJIDInstance(account.name);
//		JaxmppCore jaxmpp = multiJaxmpp.get(bareAccountJid);

		Log.v(TAG, "getting items in roster of account = "+account.name);
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor c = db.rawQuery("SELECT roster." + RosterCacheTableMetaData.FIELD_ID + ", roster." + RosterCacheTableMetaData.FIELD_JID + ","
				+ " roster." + RosterCacheTableMetaData.FIELD_NAME + ", roster." + RosterCacheTableMetaData.FIELD_GROUP_NAME + ","
				+ " vcard." + VCardsCacheTableMetaData.FIELD_DATA
				+ " FROM " + RosterCacheTableMetaData.TABLE_NAME + " roster"
				+ " LEFT JOIN " + VCardsCacheTableMetaData.TABLE_NAME + " vcard ON roster." + RosterCacheTableMetaData.FIELD_JID + " = vcard." + VCardsCacheTableMetaData.FIELD_JID 
				+ " WHERE " + RosterCacheTableMetaData.FIELD_ACCOUNT + "=? AND ( roster." + RosterCacheTableMetaData.FIELD_TIMESTAMP+">?"
				+ " OR vcard." + VCardsCacheTableMetaData.FIELD_TIMESTAMP + ">?)", new String[] { account.name, String.valueOf(oldMarker), String.valueOf(oldMarker) });
		
//		Cursor c =  context.getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), new String[] {
//			RosterTableMetaData.FIELD_ID,
//			RosterTableMetaData.FIELD_JID,
//			RosterTableMetaData.FIELD_NAME,
//			RosterTableMetaData.FIELD_AVATAR
//		}, RosterTableMetaData.FIELD_ACCOUNT + "=?", new String[] { account.name }, null);
		Log.v(TAG, "adding or updating in contacts based on roster of account = "+account.name);
		
		try {
			BatchOperation batchOperation = new BatchOperation(context, context.getContentResolver());
			while (c.moveToNext()) {
				long userId = c.getInt(c.getColumnIndex(RosterCacheTableMetaData.FIELD_ID));
				long id = lookupRawContact(context.getContentResolver(), userId);
				String group = null;
				String groupsStr = c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_GROUP_NAME));
				if (groupsStr != null && !TextUtils.isEmpty(groupsStr)) {
					String[] groups = groupsStr.split(";");
					if (groups != null) {
						group = groups[0];
					}
				}
				BareJID jid = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_JID)));
				if (id == 0) {
					final ContactOperations contactOps = ContactOperations.createNewContact(context, userId, account.name, true, batchOperation);
					contactOps.addName(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME)), null, null).addJID(jid)
						.addAvatar(c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA)));
					contactOps.addProfile(jid.toString());
					if (group != null) {
						long groupId = ensureGroupExists(context, account, group);
						contactOps.addGroupMembership(groupId);
					}
				}
				else {
//					final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
//					final ContactOperations contactOps = ContactOperations.updateExistingContact(context, id, true, batchOperation);
//					contactOps.updateName(uri, c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME)), null, null)
//						.updateAvatar(uri, c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA)));
					updateContact(context, context.getContentResolver(), account, jid.toString(), c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME)),
							c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA)), group, true, id, batchOperation);
				}
			}
			batchOperation.execute();
		} finally {
			c.close();
		}
		
		Log.v(TAG, "deleting in contacts removed from roster of account = "+account.name);
		BatchOperation batchOperation = new BatchOperation(context, context.getContentResolver());
//		for (RosterItem ri : jaxmpp.getRoster().getAll()) {			
//			// lets synchronize item with contact for account
//			final ContactOperations contactOps = ContactOperations.createNewContact(context, ri.getId(), account.name, true, batchOperation);
//			contactOps.addName(ri.getName(), null, null).addJID(ri.getJid());
//			
//		}
		deleteContacts(context.getContentResolver(), account, batchOperation);
		batchOperation.execute();
		setSyncMarker(account, newMarker);
		Log.i(TAG, "finished contacts synchronization for account = "+account.name);		
	}

	private static void updateContact(Context context, ContentResolver resolver, Account account,
			String jid, String fullName, byte[] avatar, String group,
			boolean inSync, long rawContactId, BatchOperation batchOperation) {
		
        boolean existingAvatar = false;
        boolean existingGroup = false;
//        boolean existingProfile = false;
        
        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                new String[] {String.valueOf(rawContactId)}, null);
        final ContactOperations contactOp =
                ContactOperations.updateExistingContact(context, rawContactId,
                inSync, batchOperation);		
        
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    contactOp.updateName(uri,
                            c.getString(DataQuery.COLUMN_GIVEN_NAME),
                            c.getString(DataQuery.COLUMN_FAMILY_NAME),
                            c.getString(DataQuery.COLUMN_FULL_NAME),
                            null,
                            null,
                            fullName);                	
                } else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                    existingAvatar = true;
                    contactOp.updateAvatar(uri,avatar);
                }            
                else if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE) && group != null) {
                	existingGroup = true;
                	long groupId = ensureGroupExists(context, account, group);
                	if (groupId != 0) {
                		contactOp.updateGroupMembership(uri,groupId);
                	}
                }
                // we do not use profile for now
//                else if (mimeType.equals(Constants.PROFILE_MIMETYPE)) {
//                	existingProfile = true;
//                	contactOp.updateProfile(uri, jid);
//                }
            } // while
        } finally {
            c.close();
        }   
        
        if (!existingAvatar) {
        	contactOp.addAvatar(avatar);
        }
        if (!existingGroup && group != null) {
        	long groupId = ensureGroupExists(context, account, group);
        	if (groupId != 0) {
        		contactOp.addGroupMembership(groupId);
        	}        	
        }
        // we do not use profile for now
//        if (!existingProfile) {
//        	contactOp.addProfile(jid);
//        }
	}
	
	private static long lookupRawContact(ContentResolver resolver, long userId) {
			long id = 0;
			final Cursor c = resolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
					RawContacts.ACCOUNT_TYPE+"='"+Constants.ACCOUNT_TYPE+"' AND "+RawContacts.SOURCE_ID+"=?",
					new String[] { String.valueOf(userId) }, null);
			
			try {
				if (c.moveToFirst()) {
					id = c.getLong(0);
				}
			}
			finally {
				if (c != null) {
					c.close();
				}
			}
			return id;
	}
	
	private void deleteContacts(ContentResolver resolver, Account account, BatchOperation batchOperation) {
//		final Cursor c = resolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID, RawContacts.SOURCE_ID},
//				RawContacts.ACCOUNT_TYPE+"='"+Constants.ACCOUNT_TYPE+"'", null, null);
//		StringBuilder builder = new StringBuilder(1024);
//		builder.append("(");
//		HashMap<Long,Long> map = new HashMap<Long,Long>();
//		boolean first = true;
//		try {
//			while (c.moveToNext()) {
//				map.put(c.getLong(1), c.getLong(0));
//				if (first) {
//					first = false;
//				}			
//				else {
//					builder.append(",");
//				}
//				builder.append(c.getLong(1));
//			}
//		}
//		finally {
//			if (c != null) {
//				c.close();
//			}
//		}
//		builder.append(")");

		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor c = db.rawQuery("SELECT roster." + RosterCacheTableMetaData.FIELD_ID 
				+ " FROM " + RosterCacheTableMetaData.TABLE_NAME + " roster"
				+ " WHERE roster."+ RosterCacheTableMetaData.FIELD_ACCOUNT + "=?",
				new String[] { account.name });

		StringBuilder builder = new StringBuilder(1024);
		builder.append("(0");
//		boolean first = true;
//		boolean exit = true;
		try {
			while (c.moveToNext()) {
//				exit = false;
//				if (first) {
//					first = false;
//				}			
//				else {
				builder.append(",");
//				}
				builder.append(c.getLong(0));
			}
		}
		finally {
			if (c != null) {
				c.close();
			}
		}
		builder.append(")");		
		
//		if (exit) {
//			return;
//		}
			
		
		final Cursor c1 = resolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
				RawContacts.ACCOUNT_TYPE+"='"+Constants.ACCOUNT_TYPE+"' AND "+RawContacts.SOURCE_ID+" NOT IN " + builder.toString(), null, null);

		try {
			while (c1.moveToNext()) {
		        batchOperation.add(ContactOperations.newDeleteCpo(
		                ContentUris.withAppendedId(RawContacts.CONTENT_URI, c1.getLong(0)),
		                true, true).build());				
			}
		}
		finally {
			if (c1 != null) {
				c1.close();
			}
		}
		
		
	}
	
    public static long ensureGroupExists(Context context, Account account, String group) {
        final ContentResolver resolver = context.getContentResolver();

        // Lookup the sample group
        long groupId = 0;
        final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] { Groups._ID },
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " +
                Groups.TITLE + "=?",
                new String[] { account.name, account.type, group }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    groupId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        if (groupId == 0) {
            // Sample group doesn't exist yet, so create it
            final ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, account.name);
            contentValues.put(Groups.ACCOUNT_TYPE, account.type);
            contentValues.put(Groups.TITLE, group);
            contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

            final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
            groupId = ContentUris.parseId(newGroupUri);
        }
        
        return groupId;
    }
    
    private long getSyncMarker(Account account) {
        String markerString = accountManager.getUserData(account, SYNC_MARKER_KEY);
        if (!TextUtils.isEmpty(markerString)) {
            return Long.parseLong(markerString);
        }
        return 0;
    }

    private void setSyncMarker(Account account, long marker) {
        accountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
    }
    
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
            new String[] {Data._ID, RawContacts.SOURCE_ID, Data.MIMETYPE, Data.DATA1,
            Data.DATA2, Data.DATA3, Data.DATA15 };

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_SERVER_ID = 1;
        public static final int COLUMN_MIMETYPE = 2;
        public static final int COLUMN_DATA1 = 3;
        public static final int COLUMN_DATA2 = 4;
        public static final int COLUMN_DATA3 = 5;
        public static final int COLUMN_DATA15 = 6;

        public static final Uri CONTENT_URI = Data.CONTENT_URI;

        public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }	
}
