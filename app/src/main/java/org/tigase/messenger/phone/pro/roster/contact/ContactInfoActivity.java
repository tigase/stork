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
package org.tigase.messenger.phone.pro.roster.contact;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.abdularis.civ.StorkAvatarView;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.omemo.FingerprintView;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.JaXMPPSignalProtocolStore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;

import java.util.List;

public class ContactInfoActivity
		extends AbstractServiceActivity {

	public static final String ACCOUNT_KEY = "ACCOUNT_KEY";

	public static final String JID_KEY = "JID_KEY";
	private String account;
	private StorkAvatarView contactAvatar;
	private String contactDisplayName;
	private LinearLayout contactFingerprint;
	private TextView contactJid;
	private TextView contactName;
	private JaxmppCore jaxmpp;
	private BareJID jid;
	private JaXMPPSignalProtocolStore store;

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_info);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		this.contactAvatar = findViewById(R.id.contact_avatar);
		this.contactName = findViewById(R.id.contact_name);
		this.contactJid = findViewById(R.id.contact_jid);
		this.contactFingerprint = findViewById(R.id.contact_fingerprint);

		Intent intent = getIntent();
		this.jid = BareJID.bareJIDInstance(intent.getStringExtra(JID_KEY));
		this.account = intent.getStringExtra(ACCOUNT_KEY);

		this.contactJid.setText("xmpp:" + this.jid.toString());

		loadContact();

		this.contactAvatar.setJID(this.jid, contactDisplayName);
		this.contactName.setText(contactDisplayName);
	}

	@Override
	protected void onXMPPServiceConnected() {
		this.jaxmpp = getJaxmpp(account);
		this.store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());

		runOnUiThread(() -> showOMEMOFingerprints(store));
	}

	@Override
	protected void onXMPPServiceDisconnected() {

	}

	private void showOMEMOFingerprints(final JaXMPPSignalProtocolStore store) {

//		XmppOMEMOSession session = jaxmpp.getModule(OmemoModule.class).getOMEMOSession(jid, true);
//		if (session == null) {
//			return;
//		} else if (!session.hasCiphers()) {
//			OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject()).removeSession(session);
//		}
//		for (final SignalProtocolAddress addr : session.getDeviceCiphers().keySet()) {

		List<Integer> ids = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject()).getSubDevice(jid.toString());
		for (Integer id : ids) {
			final SignalProtocolAddress addr = new SignalProtocolAddress(jid.toString(), id);
			if (!addr.getName().equals(jid.toString())) {
				continue;
			}
			final IdentityKey identity = store.getIdentity(addr);
			final boolean trusted = store.isTrustedIdentity(addr, identity, IdentityKeyStore.Direction.RECEIVING);

			FingerprintView f = new FingerprintView(this);
			f.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
															LinearLayout.LayoutParams.WRAP_CONTENT));
			f.setFingerprint(identity.getPublicKey().serialize(), 1);
//			f.setChecked(trusted);
//
//			f.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//				@Override
//				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//					((OMEMOStoreImpl) store).setIdentityTrust(addr, identity, IdentityKeyStore.Direction.RECEIVING,
//															  isChecked);
//				}
//			});

			contactFingerprint.addView(f);
			f.requestLayout();
		}
		contactFingerprint.requestLayout();
	}

	private void loadContact() {
		final String[] cols = new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
										   DatabaseContract.RosterItemsCache.FIELD_NAME,
										   DatabaseContract.RosterItemsCache.FIELD_ACCOUNT,
										   DatabaseContract.RosterItemsCache.FIELD_JID};

		try (Cursor c = getContentResolver().query(Uri.parse(RosterProvider.ROSTER_URI + "/" + account + "/" + jid),
												   cols, null, null, null)) {
			if (c.moveToNext()) {
				this.contactDisplayName = c.getString(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
			} else {
				this.contactDisplayName = jid.toString();
			}
		}
	}
}
