package org.tigase.messenger.phone.pro.roster.request;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class SubscriptionRequestActivity extends AppCompatActivity {

	@Bind(R.id.contact_xmppid)
	EditText xmppId;
	@Bind(R.id.contact_display_name)
	EditText mName;
	@Bind(R.id.user_avatar)
	ImageView avatar;
	@Bind(R.id.user_details_form)
	LinearLayout mDetailsForm;
	private BareJID jid;
	private String account;
	private BroadcastReceiver avatarUpdatedListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getStringExtra("jid").equals(jid.toString())) {
				fillAvatar();
			}
		}
	};
	private XMPPService mService;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			XMPPService.LocalBinder binder = (XMPPService.LocalBinder) service;
			mService = binder.getService();
			SubscriptionRequestActivity.this.retrieveVCard();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	private static void set(EditText field, String value) {
		field.setKeyListener(null);
		field.setText(value);
		if (field.getParent() instanceof ViewGroup) {
			if (value == null || value.trim().isEmpty()) {
				((ViewGroup) field.getParent()).setVisibility(View.GONE);
			} else {
				((ViewGroup) field.getParent()).setVisibility(View.VISIBLE);
			}
		}
	}

	private void fillAvatar() {
		AvatarHelper.setAvatarToImageView(jid, avatar);
	}

	private void fillVCard(VCard vcard) {
		String fn = vcard.getFullName();
		set(R.id.contact_vcard_name, fn);
		if (fn != null && !fn.toString().isEmpty() && mName.getText().toString().equals(jid.getLocalpart())) {
			mName.setText(fn);
		}
		set(R.id.contact_vcard_nickname, vcard.getNickName());
		set(R.id.contact_vcard_phone, vcard.getHomeTelVoice());
		set(R.id.contact_vcard_work, vcard.getOrgName());
	}

	@OnClick(R.id.contact_add_button)
	void onAddClick() {
		final String name = mName.getText().toString();
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final Jaxmpp jaxmpp = mService.getJaxmpp(account);
				final PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
				final RosterModule rosterModule = jaxmpp.getModule(RosterModule.class);

				try {
					presenceModule.subscribe(JID.jidInstance(SubscriptionRequestActivity.this.jid));
					presenceModule.subscribed(JID.jidInstance(SubscriptionRequestActivity.this.jid));
					rosterModule.getRosterStore().add(SubscriptionRequestActivity.this.jid, name, new AsyncCallback() {
						@Override
						public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
						}

						@Override
						public void onSuccess(Stanza responseStanza) throws JaxmppException {
						}

						@Override
						public void onTimeout() throws JaxmppException {
						}
					});
				} catch (JaxmppException e) {
					Log.e("SubscriptionRequest", "Problem on rejecting request", e);
				}

				return null;
			}
		}).execute();
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_subscription_request);
		ButterKnife.bind(this);

		Bundle extras = getIntent().getExtras();
		this.account = extras.getString("account_name");
		this.jid = BareJID.bareJIDInstance(extras.getString("jid"));

		xmppId.setText(jid.toString());
		xmppId.setKeyListener(null);

		registerReceiver(avatarUpdatedListener, new IntentFilter("org.tigase.messenger.phone.pro.AvatarUpdated"));

		mName.setText(jid.getLocalpart());
		fillAvatar();

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(avatarUpdatedListener);
		super.onDestroy();
	}

	@OnClick(R.id.contact_reject_button)
	void onRejectClick() {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final Jaxmpp jaxmpp = mService.getJaxmpp(account);
				final PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
				final RosterModule rosterModule = jaxmpp.getModule(RosterModule.class);

				try {
					presenceModule.unsubscribe(JID.jidInstance(SubscriptionRequestActivity.this.jid));
					presenceModule.unsubscribed(JID.jidInstance(SubscriptionRequestActivity.this.jid));
					rosterModule.getRosterStore().remove(SubscriptionRequestActivity.this.jid);
				} catch (JaxmppException e) {
					Log.e("SubscriptionRequest", "Problem on rejecting request", e);
				}

				return null;
			}
		}).execute();
		finish();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent service = new Intent(getApplicationContext(), XMPPService.class);
		bindService(service, mServiceConnection, 0);
	}

	@Override
	protected void onStop() {
		unbindService(mServiceConnection);
		super.onStop();
	}

	private void retrieveVCard() {
		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final VCardModule module = mService.getJaxmpp(account).getModule(VCardModule.class);
				try {
					module.retrieveVCard(JID.jidInstance(SubscriptionRequestActivity.this.jid),
							new VCardModule.VCardAsyncCallback() {
								@Override
								public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
									runOnUiThread(new Runnable() {
										public void run() {
											try {
												mDetailsForm.setVisibility(View.GONE);
											} catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									});
								}

								@Override
								public void onTimeout() throws JaxmppException {
									runOnUiThread(new Runnable() {
										public void run() {
											try {
												mDetailsForm.setVisibility(View.GONE);
											} catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									});
								}

								@Override
								protected void onVCardReceived(final VCard vcard) throws XMLException {
									runOnUiThread(new Runnable() {
										public void run() {
											try {
												mDetailsForm.setVisibility(View.VISIBLE);
												fillVCard(vcard);
											} catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									});
								}
							});
				} catch (JaxmppException e) {
					Log.e("SubscriptionRequest", "Cannot retrieve VCard", e);
				}
				return null;
			}
		}).execute();

	}

	private void set(int id, String value) {
		EditText f = (EditText) findViewById(id);
		set(f, value);
	}

}
