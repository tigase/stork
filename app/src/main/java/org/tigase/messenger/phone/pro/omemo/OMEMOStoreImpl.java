/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.omemo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.Hex;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.XmppOMEMOSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OMEMOStoreImpl
		implements OMEMOStore {

	private final static String TAG = "OMEMOStore";
	private final BareJID account;
	private final XMPPService context;
	private final DatabaseHelper helper;
	private final Map<BareJID, XmppOMEMOSession> sesssions = new ConcurrentHashMap<>();
	private IdentityKeyPair identityKeyPair;
	private int localRegistrationId;

	public static OMEMOStore create(XMPPService context, DatabaseHelper dbHelper, BareJID accountJid,
									int omemoRegistrationId) {
		OMEMOStore store = new OMEMOStoreImpl(context, dbHelper, accountJid);
		try {
			store.init(omemoRegistrationId);
		} catch (Exception e) {
			throw new RuntimeException("Cannot create OMEMO Store", e);
		}

		return store;
	}

	private OMEMOStoreImpl(XMPPService context, DatabaseHelper dbHelper, BareJID accountJid) {
		this.context = context;
		this.helper = dbHelper;
		this.account = accountJid;
	}

	@Override
	public XmppOMEMOSession getSession(BareJID jid) {
		return this.sesssions.get(jid);
	}

	@Override
	public void storeSession(XmppOMEMOSession session) {
		this.sesssions.put(session.getJid(), session);
	}

	@Override
	public void removeSession(XmppOMEMOSession session) {
		this.sesssions.remove(session.getJid());
	}

	@Override
	public boolean isOMEMORequired(BareJID jid) {
		try (Cursor c = helper.getReadableDatabase()
				.query(DatabaseContract.OpenChats.TABLE_NAME, new String[]{DatabaseContract.OpenChats.FIELD_ENCRYPTION},
					   DatabaseContract.OpenChats.FIELD_ACCOUNT + "=? AND " + DatabaseContract.OpenChats.FIELD_JID +
							   "=? AND " + DatabaseContract.OpenChats.FIELD_TYPE + "=" +
							   DatabaseContract.OpenChats.TYPE_CHAT, new String[]{account.toString(), jid.toString()},
					   null, null, null)) {
			if (c.moveToNext()) {
				int tr = c.getInt(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ENCRYPTION));
				return tr == 1;
			} else {
				return false;
			}
		}
	}

	@Override
	public void setOMEMORequired(final BareJID jid, final boolean required) {
		final SQLiteDatabase db = helper.getWritableDatabase();
		try (Cursor c = db.query(DatabaseContract.OpenChats.TABLE_NAME,
								 new String[]{DatabaseContract.OpenChats.FIELD_ID},
								 DatabaseContract.OpenChats.FIELD_ACCOUNT + "=? AND " +
										 DatabaseContract.OpenChats.FIELD_JID + "=? AND " +
										 DatabaseContract.OpenChats.FIELD_TYPE + "=" +
										 DatabaseContract.OpenChats.TYPE_CHAT,
								 new String[]{account.toString(), jid.toString()}, null, null, null)) {

			if (c.moveToNext()) {
				int id = c.getInt(c.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
				ContentValues values = new ContentValues();
				values.put(DatabaseContract.OpenChats.FIELD_ENCRYPTION, required ? 1 : 0);
				db.update(DatabaseContract.OpenChats.TABLE_NAME, values, DatabaseContract.OpenChats.FIELD_ID + "=" + id,
						  null);
			}
		}
	}

	@Override
	public IdentityKeyPair getIdentityKeyPair() {
		return this.identityKeyPair;
	}

	@Override
	public int getLocalRegistrationId() {
		return this.localRegistrationId;
	}

	@Override
	public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
		ContentValues values = new ContentValues();
		values.put(OMEMOContract.Identities.FIELD_ACCOUNT, account.toString());
		values.put(OMEMOContract.Identities.FIELD_JID, address.getName());
		values.put(OMEMOContract.Identities.FIELD_DEVICE_ID, address.getDeviceId());
		values.put(OMEMOContract.Identities.FIELD_ACTIVE, 1);
		values.put(OMEMOContract.Identities.FIELD_LAST_USAGE, System.currentTimeMillis());
		values.put(OMEMOContract.Identities.FIELD_HAS_KEYPAIR, 0);
		values.put(OMEMOContract.Identities.FIELD_TRUST, OMEMOContract.Identities.TRUST_TRUSTED);
		values.put(OMEMOContract.Identities.FIELD_KEY, Base64.encode(identityKey.serialize()));
		values.put(OMEMOContract.Identities.FIELD_FINGERPRINT, Hex.encode(identityKey.getPublicKey().serialize(), 1));

		helper.getWritableDatabase().insert(OMEMOContract.Identities.TABLE_NAME, null, values);

		return true;
	}

	public void setIdentityTrust(SignalProtocolAddress address, IdentityKey identityKey,
								 IdentityKeyStore.Direction direction, boolean trusted) {
		String hash = Hex.encode(identityKey.getPublicKey().serialize(), 1);
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		int v = trusted ? OMEMOContract.Identities.TRUST_TRUSTED : OMEMOContract.Identities.TRUST_UNTRUSTED;
		values.put(OMEMOContract.Identities.FIELD_TRUST, v);

		db.update(OMEMOContract.Identities.TABLE_NAME, values,
				  OMEMOContract.Identities.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Identities.FIELD_JID + "=? AND " +
						  OMEMOContract.Identities.FIELD_DEVICE_ID + "=? AND " + OMEMOContract.Identities.FIELD_ACTIVE +
						  "=1 AND " + OMEMOContract.Identities.FIELD_FINGERPRINT + "=?",
				  new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId()), hash});

		if (!trusted) {
			XmppOMEMOSession s = getSession(BareJID.bareJIDInstance(address.getName()));
			s.getDeviceCiphers().remove(address.getDeviceId());
		}
	}

	@Override
	public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey,
									 IdentityKeyStore.Direction direction) {
		String hash = Hex.encode(identityKey.getPublicKey().serialize(), 1);
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Identities.TABLE_NAME, new String[]{OMEMOContract.Identities.FIELD_TRUST},
					   OMEMOContract.Identities.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Identities.FIELD_JID +
							   "=? AND " + OMEMOContract.Identities.FIELD_DEVICE_ID + "=? AND " +
							   OMEMOContract.Identities.FIELD_ACTIVE + "=1 AND " +
							   OMEMOContract.Identities.FIELD_FINGERPRINT + "=?",
					   new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId()), hash},
					   null, null, null)) {
			if (c.moveToNext()) {
				int tr = c.getInt(c.getColumnIndex(OMEMOContract.Identities.FIELD_TRUST));
				return tr != OMEMOContract.Identities.TRUST_UNTRUSTED;
			} else {
				return true;
			}
		}
	}

	@Override
	public IdentityKey getIdentity(SignalProtocolAddress address) {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Identities.TABLE_NAME, new String[]{OMEMOContract.Identities.FIELD_KEY},
					   OMEMOContract.Identities.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Identities.FIELD_JID +
							   "=? AND " + OMEMOContract.Identities.FIELD_DEVICE_ID + "=? AND " +
							   OMEMOContract.Identities.FIELD_ACTIVE + "=1",
					   new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId())}, null,
					   null, null)) {
			if (c.moveToNext()) {
				String k = c.getString(c.getColumnIndex(OMEMOContract.Identities.FIELD_KEY));
				return new IdentityKey(Base64.decode(k), 0);
			} else {
				return null;
			}
		} catch (InvalidKeyException e) {
			Log.e(TAG, "Cannot get Identity " + address, e);
		}
		return null;
	}

	@Override
	public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.PreKeys.TABLE_NAME, new String[]{OMEMOContract.PreKeys.FIELD_KEY},
					   OMEMOContract.PreKeys.FIELD_ACCOUNT + "=? AND " + OMEMOContract.PreKeys.FIELD_ID + "=?",
					   new String[]{account.toString(), String.valueOf(preKeyId)}, null, null, null)) {
			if (c.moveToNext()) {
				return new PreKeyRecord(Base64.decode(c.getString(c.getColumnIndex(OMEMOContract.PreKeys.FIELD_KEY))));
			} else {
				throw new InvalidKeyIdException("No PreKey with id " + preKeyId);
			}
		} catch (IOException e) {
			Log.e(TAG, "Cannot load PreKey " + preKeyId, e);
		}
		return null;
	}

	@Override
	public void storePreKey(int preKeyId, PreKeyRecord record) {
		ContentValues values = new ContentValues();
		values.put(OMEMOContract.PreKeys.FIELD_ACCOUNT, account.toString());
		values.put(OMEMOContract.PreKeys.FIELD_ID, preKeyId);
		values.put(OMEMOContract.PreKeys.FIELD_KEY, Base64.encode(record.serialize()));
		helper.getWritableDatabase().insert(OMEMOContract.PreKeys.TABLE_NAME, null, values);
	}

	@Override
	public boolean containsPreKey(int preKeyId) {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.PreKeys.TABLE_NAME, new String[]{OMEMOContract.PreKeys.FIELD_KEY},
					   OMEMOContract.PreKeys.FIELD_ACCOUNT + "=? AND " + OMEMOContract.PreKeys.FIELD_ID + "=?",
					   new String[]{account.toString(), String.valueOf(preKeyId)}, null, null, null)) {
			if (c.moveToNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void removePreKey(int preKeyId) {
		helper.getWritableDatabase()
				.delete(OMEMOContract.PreKeys.TABLE_NAME,
						OMEMOContract.PreKeys.FIELD_ACCOUNT + "=? AND " + OMEMOContract.PreKeys.FIELD_ID + "=?",
						new String[]{account.toString(), String.valueOf(preKeyId)});
	}

	@Override
	public SessionRecord loadSession(SignalProtocolAddress address) {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Sessions.TABLE_NAME, new String[]{OMEMOContract.Sessions.FIELD_KEY},
					   OMEMOContract.Sessions.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Sessions.FIELD_JID + "=? AND " +
							   OMEMOContract.Sessions.FIELD_DEVICE_ID + "=?",
					   new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId())}, null,
					   null, null)) {
			if (c.moveToNext()) {
				return new SessionRecord(
						Base64.decode(c.getString(c.getColumnIndex(OMEMOContract.Sessions.FIELD_KEY))));
			} else {
				return new SessionRecord();
			}
		} catch (IOException e) {
			Log.e(TAG, "Cannot load Session " + address, e);
			throw new AssertionError(e);
		}
	}

	@Override
	public List<Integer> getSubDeviceSessions(String name) {
		final ArrayList<Integer> result = new ArrayList<>();
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Sessions.TABLE_NAME, new String[]{OMEMOContract.Sessions.FIELD_DEVICE_ID},
					   OMEMOContract.Sessions.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Sessions.FIELD_JID + "=?",
					   new String[]{account.toString(), name}, null, null, null)) {
			while (c.moveToNext()) {
				result.add(c.getInt(c.getColumnIndex(OMEMOContract.Sessions.FIELD_DEVICE_ID)));
			}
		}
		return result;
	}

	@Override
	public void storeSession(SignalProtocolAddress address, SessionRecord record) {
		ContentValues values = new ContentValues();
		values.put(OMEMOContract.Sessions.FIELD_ACCOUNT, account.toString());
		values.put(OMEMOContract.Sessions.FIELD_JID, address.getName());
		values.put(OMEMOContract.Sessions.FIELD_DEVICE_ID, address.getDeviceId());
		values.put(OMEMOContract.Sessions.FIELD_KEY, Base64.encode(record.serialize()));
		helper.getWritableDatabase().insert(OMEMOContract.Sessions.TABLE_NAME, null, values);
	}

	@Override
	public boolean containsSession(SignalProtocolAddress address) {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Sessions.TABLE_NAME, new String[]{OMEMOContract.Sessions.FIELD_KEY},
					   OMEMOContract.Sessions.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Sessions.FIELD_JID + "=? AND " +
							   OMEMOContract.Sessions.FIELD_DEVICE_ID + "=?",
					   new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId())}, null,
					   null, null)) {
			if (c.moveToNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void deleteSession(SignalProtocolAddress address) {
		helper.getWritableDatabase()
				.delete(OMEMOContract.Sessions.TABLE_NAME,
						OMEMOContract.Sessions.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Sessions.FIELD_JID + "=? AND" +
								OMEMOContract.Sessions.FIELD_DEVICE_ID + "=?",
						new String[]{account.toString(), address.getName(), String.valueOf(address.getDeviceId())});
	}

	@Override
	public void deleteAllSessions(String name) {
		this.sesssions.remove(BareJID.bareJIDInstance(name));
		helper.getWritableDatabase()
				.delete(OMEMOContract.Sessions.TABLE_NAME,
						OMEMOContract.Sessions.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Sessions.FIELD_JID + "=?",
						new String[]{account.toString(), name});
	}

	@Override
	public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.SignedPreKeys.TABLE_NAME, new String[]{OMEMOContract.SignedPreKeys.FIELD_KEY},
					   OMEMOContract.SignedPreKeys.FIELD_ACCOUNT + "=? AND " + OMEMOContract.SignedPreKeys.FIELD_ID +
							   "=?", new String[]{account.toString(), String.valueOf(signedPreKeyId)}, null, null,
					   null)) {
			if (c.moveToNext()) {
				return new SignedPreKeyRecord(
						Base64.decode(c.getString(c.getColumnIndex(OMEMOContract.SignedPreKeys.FIELD_KEY))));
			} else {
				throw new InvalidKeyIdException("No SignedPreKey with id " + signedPreKeyId);
			}
		} catch (IOException e) {
			Log.e(TAG, "Cannot load SignedPreKey " + signedPreKeyId, e);
		}
		return null;
	}

	@Override
	public List<PreKeyRecord> loadPreKeys() {
		ArrayList<PreKeyRecord> result = new ArrayList<>();
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.PreKeys.TABLE_NAME, new String[]{OMEMOContract.PreKeys.FIELD_KEY},
					   OMEMOContract.PreKeys.FIELD_ACCOUNT + "=?", new String[]{account.toString()}, null, null,
					   null)) {
			if (c.moveToNext()) {
				result.add(new PreKeyRecord(
						Base64.decode(c.getString(c.getColumnIndex(OMEMOContract.PreKeys.FIELD_KEY)))));
			}
		} catch (IOException e) {
			Log.e(TAG, "Cannot load SignedPreKeys", e);
		}
		return result;
	}

	@Override
	public List<Integer> getSubDevice(final String name) {
		ArrayList<Integer> result = new ArrayList<>();

		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Identities.TABLE_NAME, new String[]{OMEMOContract.Identities.FIELD_DEVICE_ID},
					   OMEMOContract.Identities.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Identities.FIELD_JID +
							   "=? AND " + OMEMOContract.Identities.FIELD_ACTIVE + "=1",
					   new String[]{account.toString(), name}, null, null, null)) {
			while (c.moveToNext()) {
				int k = c.getInt(c.getColumnIndex(OMEMOContract.Identities.FIELD_DEVICE_ID));
				result.add(k);
			}
		}

		return result;
	}

	@Override
	public List<SignedPreKeyRecord> loadSignedPreKeys() {
		ArrayList<SignedPreKeyRecord> result = new ArrayList<>();
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.SignedPreKeys.TABLE_NAME, new String[]{OMEMOContract.SignedPreKeys.FIELD_KEY},
					   OMEMOContract.SignedPreKeys.FIELD_ACCOUNT + "=?", new String[]{account.toString()}, null, null,
					   null)) {
			if (c.moveToNext()) {
				result.add(new SignedPreKeyRecord(
						Base64.decode(c.getString(c.getColumnIndex(OMEMOContract.SignedPreKeys.FIELD_KEY)))));
			}
		} catch (IOException e) {
			Log.e(TAG, "Cannot load SignedPreKeys", e);
		}
		return result;
	}

	@Override
	public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
		ContentValues values = new ContentValues();
		values.put(OMEMOContract.SignedPreKeys.FIELD_ACCOUNT, account.toString());
		values.put(OMEMOContract.SignedPreKeys.FIELD_ID, signedPreKeyId);
		values.put(OMEMOContract.SignedPreKeys.FIELD_KEY, Base64.encode(record.serialize()));
		helper.getWritableDatabase().insert(OMEMOContract.SignedPreKeys.TABLE_NAME, null, values);
	}

	@Override
	public boolean containsSignedPreKey(int signedPreKeyId) {
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.SignedPreKeys.TABLE_NAME, new String[]{OMEMOContract.SignedPreKeys.FIELD_KEY},
					   OMEMOContract.SignedPreKeys.FIELD_ACCOUNT + "=?", new String[]{account.toString()}, null, null,
					   null)) {
			if (c.moveToNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void removeSignedPreKey(int signedPreKeyId) {
		helper.getWritableDatabase()
				.delete(OMEMOContract.SignedPreKeys.TABLE_NAME,
						OMEMOContract.SignedPreKeys.FIELD_ACCOUNT + "=? AND " + OMEMOContract.SignedPreKeys.FIELD_ID +
								"=?", new String[]{account.toString(), String.valueOf(signedPreKeyId)});

	}

	public void reset() {
		helper.getWritableDatabase().delete(OMEMOContract.SignedPreKeys.TABLE_NAME, null, null);
		helper.getWritableDatabase().delete(OMEMOContract.PreKeys.TABLE_NAME, null, null);
		helper.getWritableDatabase().delete(OMEMOContract.Identities.TABLE_NAME, null, null);
		helper.getWritableDatabase().delete(OMEMOContract.Sessions.TABLE_NAME, null, null);
	}

	public void init(int omemoRegistrationId) throws InvalidKeyException {
		this.localRegistrationId = omemoRegistrationId;

		boolean created = false;
		try (Cursor c = helper.getReadableDatabase()
				.query(OMEMOContract.Identities.TABLE_NAME, new String[]{OMEMOContract.Identities.FIELD_KEY},
					   OMEMOContract.Identities.FIELD_ACCOUNT + "=? AND " + OMEMOContract.Identities.FIELD_HAS_KEYPAIR +
							   "=1 AND " + OMEMOContract.Identities.FIELD_ACTIVE + "=1",
					   new String[]{account.toString()}, null, null, null)) {
			if (c.moveToNext()) {
				String k = c.getString(c.getColumnIndex(OMEMOContract.Identities.FIELD_KEY));
				this.identityKeyPair = new IdentityKeyPair(Base64.decode(k));
			} else {
				this.identityKeyPair = KeyHelper.generateIdentityKeyPair();
				created = true;
			}
		}
		if (created) {
			storeIdentityKeyPair();
		}

		if (created) {
			List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(0, 128);
			SignedPreKeyRecord signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 1);

			for (PreKeyRecord preKey : preKeys) {
				storePreKey(preKey.getId(), preKey);
			}

			storeSignedPreKey(signedPreKey.getId(), signedPreKey);
		}

	}

	private void storeIdentityKeyPair() {
		ContentValues values = new ContentValues();

		values.put(OMEMOContract.Identities.FIELD_ACCOUNT, account.toString());
		values.put(OMEMOContract.Identities.FIELD_JID, account.toString());
		values.put(OMEMOContract.Identities.FIELD_DEVICE_ID, this.localRegistrationId);
		values.put(OMEMOContract.Identities.FIELD_ACTIVE, 1);
		values.put(OMEMOContract.Identities.FIELD_LAST_USAGE, System.currentTimeMillis());
		values.put(OMEMOContract.Identities.FIELD_HAS_KEYPAIR, 1);
		values.put(OMEMOContract.Identities.FIELD_TRUST, OMEMOContract.Identities.TRUST_ULTIMATE);

		values.put(OMEMOContract.Identities.FIELD_KEY, Base64.encode(this.identityKeyPair.serialize()));
		values.put(OMEMOContract.Identities.FIELD_FINGERPRINT,
				   Hex.encode(this.identityKeyPair.getPublicKey().serialize(), 1));

		helper.getWritableDatabase().insert(OMEMOContract.Identities.TABLE_NAME, null, values);
	}

}
