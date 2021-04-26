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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class EditContactActivity
		extends AppCompatActivity {

	private final ArrayList accountsList = new ArrayList<>();
	Spinner mAccountSelector;
	EditText mContactName;
	EditText mContactXMPPID;
	ProgressBar progressBar;
	private XMPPService mService;
	private ArrayAdapter<Object> sa;
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			XMPPService.LocalBinder binder = (XMPPService.LocalBinder) service;
			mService = binder.getService();

			sa.clear();
			for (JaxmppCore j : mService.getMultiJaxmpp().get()) {
				sa.add(j.getSessionObject().getUserBareJid());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_contact);

		mAccountSelector = findViewById(R.id.contact_account);
		mContactXMPPID = findViewById(R.id.contact_xmppid);
		mContactName = findViewById(R.id.contact_display_name);
		progressBar = findViewById(R.id.contact_progress);

		if (mContactXMPPID != null) {
			mContactXMPPID.setOnEditorActionListener((textView, i, keyEvent) -> {
				onAddButtonClick();
				return false;
			});
		}
		if (mContactName != null) {
			mContactName.setOnEditorActionListener((textView, i, keyEvent) -> {
				onAddButtonClick();
				return false;
			});
		}

		Button contactAddButton = findViewById(R.id.contact_add_button);
		contactAddButton.setOnClickListener(view -> onAddButtonClick());

		this.sa = new ArrayAdapter<>(getBaseContext(), R.layout.account_list_item, R.id.account_name, accountsList);
		mAccountSelector.setAdapter(sa);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent service = new Intent(getApplicationContext(), XMPPService.class);
		bindService(service, mServiceConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mServiceConnection);
	}

	private void onAddButtonClick() {
		mContactXMPPID.setError(null);
		BareJID jid;
		try {
			if (mContactXMPPID.getText().toString().isEmpty()) {
				mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
				return;
			}
			if (mAccountSelector.getSelectedItem() == null) {
				return;
			}
			jid = BareJID.bareJIDInstance(mContactXMPPID.getText().toString());
		} catch (Exception e) {
			mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
			return;
		}
		if (mService != null) {
			Editable v = mContactName.getText();
			AddContactTask task = new AddContactTask(this, mService, BareJID.bareJIDInstance(
					mAccountSelector.getSelectedItem().toString()), jid, v == null ? null : v.toString());
			task.execute();
		}
	}

	private static class AddContactTask
			extends AsyncTask<Void, Integer, Boolean> {

		private final BareJID account;
		private final BareJID jid;
		private final String name;
		private final WeakReference<EditContactActivity> weakActivity;
		private final XMPPException.ErrorCondition error = null;
		private final XMPPService mService;
		private boolean result = false;

		public AddContactTask(EditContactActivity activity, XMPPService mService, BareJID account, BareJID jid,
							  String name) {
			weakActivity = new WeakReference<>(activity);
			this.mService = mService;
			this.jid = jid;
			this.name = name;
			this.account = account;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				final Jaxmpp jaxmpp = mService.getJaxmpp(account);
				RosterStore rosterStore = RosterModule.getRosterStore(jaxmpp.getSessionObject());
				rosterStore.add(jid, name, new AsyncCallback() {
					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						error = error;
						synchronized (AddContactTask.this) {
							AddContactTask.this.notify();
						}
					}

					@Override
					public void onSuccess(Stanza responseStanza) throws JaxmppException {
						result = true;
						synchronized (AddContactTask.this) {
							AddContactTask.this.notify();
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						synchronized (AddContactTask.this) {
							AddContactTask.this.notify();
						}
					}
				});

				jaxmpp.getModule(PresenceModule.class).subscribed(JID.jidInstance(jid));
				jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));

				synchronized (AddContactTask.this) {
					AddContactTask.this.wait();
				}

				return result;
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Can't add contact to roster", e);
				final EditContactActivity activity = weakActivity.get();
				if (activity != null) {
					activity.runOnUiThread(() -> {
						try {
							Toast.makeText(activity, "ERROR " + e.getMessage(), Toast.LENGTH_SHORT).show();
						} catch (Exception ignore) {
						}
					});
				}
				return Boolean.FALSE;
			}
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
			final EditContactActivity activity = weakActivity.get();
			if (activity != null) {
				activity.runOnUiThread(() -> {
					try {
						activity.progressBar.setVisibility(View.GONE);
						if (result) {
							activity.finish();
						} else if (error != null) {
							Toast.makeText(activity, "Error " + error, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(activity, "Timeout ", Toast.LENGTH_SHORT).show();
						}
					} catch (Exception ignore) {
					}
				});
			}
		}

		@Override
		protected void onPreExecute() {
			final EditContactActivity activity = weakActivity.get();
			if (activity != null) {
				activity.progressBar.setVisibility(View.VISIBLE);
			}
			super.onPreExecute();
		}
	}
}
