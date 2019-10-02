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
package org.tigase.messenger.phone.pro.conversations.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.omemo.FingerprintView;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.JaXMPPSignalProtocolStore;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.OmemoModule;
import tigase.jaxmpp.core.client.xmpp.modules.omemo.XmppOMEMOSession;

import java.util.List;

public class OMEMOStartActivity
		extends AbstractServiceActivity {

	public static final String ACCOUNT_KEY = "ACCOUNT_KEY";
	public static final String JID_KEY = "JID_KEY";
	public static final String CHAT_ID_KEY = "CHAT_ID_KEY";
	public static final String STATUS_KEY = "STATUS_KEY";

	public static final int RESULT_ENABLED = 5;
	public static final int RESULT_DISABLED = 1;
	private static final String TAG = "OMEMOStartActivity";

	private final Handler handler = new Handler();
	private View cardError;
	private View cardFingerprint;
	private View cardProgress;

	@Override
	public boolean onSupportNavigateUp() {
		setResult(RESULT_DISABLED);
		finish();

		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_omemo_start);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		this.cardFingerprint = findViewById(R.id.card_omemo_fingerprint);
		cardFingerprint.setVisibility(View.GONE);
		this.cardError = findViewById(R.id.card_omemo_error);
		cardError.setVisibility(View.GONE);
		this.cardProgress = findViewById(R.id.card_omemo_progress);
		cardProgress.setVisibility(View.VISIBLE);

		((Button) findViewById(R.id.button_close_error)).setOnClickListener(v -> disableAndClose());

	}

	@Override
	protected void onXMPPServiceConnected() {
		turnItOn();
	}

	@Override
	protected void onXMPPServiceDisconnected() {
	}

	private void disableAndClose() {
		final Intent intent = getIntent();
		try {
			String ac = intent.getStringExtra(ACCOUNT_KEY);
			JaxmppCore jaxmpp = getJaxmpp(ac);
			BareJID jid = BareJID.bareJIDInstance(intent.getStringExtra(JID_KEY));
			final JaXMPPSignalProtocolStore store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
			store.setOMEMORequired(jid, false);
		} catch (Exception e) {
			Log.w(TAG, "Cannot disable OMEMO session", e);
		}
		setResult(RESULT_DISABLED);
		finish();
	}

	private void enableAndClose() {
		final Intent intent = getIntent();
		try {
			String ac = intent.getStringExtra(ACCOUNT_KEY);
			JaxmppCore jaxmpp = getJaxmpp(ac);
			BareJID jid = BareJID.bareJIDInstance(intent.getStringExtra(JID_KEY));
			final JaXMPPSignalProtocolStore store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
			store.setOMEMORequired(jid, true);
		} catch (Exception e) {
			Log.w(TAG, "Cannot enable OMEMO session", e);
		}
		setResult(RESULT_ENABLED);
		finish();
	}

	private void showErrorCard(String errorMessage) {
		handler.post(() -> {
			cardProgress.setVisibility(View.GONE);
			cardError.setVisibility(View.VISIBLE);
			TextView errorText = findViewById(R.id.omemo_error_message);
		});
	}

	private void showFingerprintCard(final XmppOMEMOSession session, final JaXMPPSignalProtocolStore store,
									 final BareJID jid, final String account) {
		if (!session.hasCiphers()) {
			showErrorCard("Can't create session.");
		} else {
			handler.post(() -> {
				cardProgress.setVisibility(View.GONE);
				cardFingerprint.setVisibility(View.VISIBLE);
				final LinearLayout fgpsPanel = findViewById(R.id.omemo_fingerprints_panel);

				List<Integer> ids = store.getSubDeviceSessions(jid.toString());

				for (Integer id : ids) {
					final SignalProtocolAddress addr = new SignalProtocolAddress(jid.toString(), id);
					IdentityKey identity = store.getIdentity(addr);

					if (identity == null) {
						continue;
					}

					FingerprintView f = new FingerprintView(this);
					f.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					f.setFingerprint(identity.getPublicKey().serialize(), 1);

					fgpsPanel.addView(f);

					f.requestLayout();
					fgpsPanel.requestLayout();
				}

				enableAndClose();
			});
		}
	}

	private void turnItOn() {
		final Intent intent = getIntent();
		boolean omemoEnable = intent.getBooleanExtra(STATUS_KEY, false);

		if (!omemoEnable) {
			disableAndClose();
			return;
		}

		String ac = intent.getStringExtra(ACCOUNT_KEY);
		BareJID jid = BareJID.bareJIDInstance(intent.getStringExtra(JID_KEY));
		int chatId = intent.getIntExtra(CHAT_ID_KEY, -1);

		JaxmppCore jaxmpp = getJaxmpp(ac);

		final JaXMPPSignalProtocolStore store = OmemoModule.getSignalProtocolStore(jaxmpp.getSessionObject());
		final XmppOMEMOSession session = store.getSession(jid);

		if (session != null) {
			enableAndClose();
			return;
		}

		try {
			jaxmpp.getModule(OmemoModule.class).createOMEMOSession(jid, new OmemoModule.CreateOMEMOSessionHandler() {
				@Override
				public void onError() {
					Log.w(TAG, "OMEMO Session is not created");
					showErrorCard("");
				}

				@Override
				public void onSessionCreated(XmppOMEMOSession session) {
					Log.i(TAG, "OMEMO session is created");
//								store.setOMEMORequired(jid, omemoEnable);

//					addOwnKeys(session, store, jaxmpp);

					showFingerprintCard(session, store, jid, ac);
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "Cannot create OMEMO session", e);
			showErrorCard(e.getMessage());
		}
	}

	private void addOwnKeys(XmppOMEMOSession session, JaXMPPSignalProtocolStore store, JaxmppCore jaxmpp) {
		final String jid = jaxmpp.getSessionObject().getUserBareJid().toString();
		for (int id : store.getSubDevice(jid)) {
			SignalProtocolAddress addr = new SignalProtocolAddress(jid, id);
			session.addDeviceCipher(store, addr);
		}
	}
}
