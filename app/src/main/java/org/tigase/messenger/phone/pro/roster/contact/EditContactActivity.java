package org.tigase.messenger.phone.pro.roster.contact;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
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
	private ServiceConnection mServiceConnection = new ServiceConnection() {
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

	private void onAddButtonClick() {
		mContactXMPPID.setError(null);
		BareJID jid;
		try {
			if (mContactXMPPID.getText().toString().isEmpty()) {
				mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
				return;
			}
			jid = BareJID.bareJIDInstance(mContactXMPPID.getText().toString());
		} catch (Exception e) {
			mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
			return;
		}
		if (mService != null) {
			AddContactTask task = new AddContactTask(this, mService, BareJID.bareJIDInstance(
					mAccountSelector.getSelectedItem().toString()), jid, mContactName.getText().toString());
			task.execute();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_contact);

		mAccountSelector = (Spinner) findViewById(R.id.contact_account);
		mContactXMPPID = (EditText) findViewById(R.id.contact_xmppid);
		mContactName = (EditText) findViewById(R.id.contact_display_name);
		progressBar = (ProgressBar) findViewById(R.id.contact_progress);

		mContactXMPPID.setOnEditorActionListener((textView, i, keyEvent) -> {
			onAddButtonClick();
			return false;
		});
		mContactName.setOnEditorActionListener((textView, i, keyEvent) -> {
			onAddButtonClick();
			return false;
		});

		Button contactAddButton = (Button) findViewById(R.id.contact_add_button);
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

	private static class AddContactTask
			extends AsyncTask<Void, Integer, Boolean> {

		private final BareJID account;
		private final BareJID jid;
		private final String name;
		private final WeakReference<EditContactActivity> weakActivity;
		private XMPPException.ErrorCondition error = null;
		private XMPPService mService;
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
