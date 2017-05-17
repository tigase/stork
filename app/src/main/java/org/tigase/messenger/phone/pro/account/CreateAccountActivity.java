package org.tigase.messenger.phone.pro.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.dynaform.DynamicForm;
import org.tigase.messenger.phone.pro.service.MobileModeFeature;
import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.TextPrivateField;
import tigase.jaxmpp.core.client.xmpp.forms.TextSingleField;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.UnifiedRegistrationForm;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class CreateAccountActivity
		extends AppCompatActivity {

	private static final String TAG = "CreateAccountActivity";
	private DynamicForm dynamicForm;
	private View hostSelectorPanel;
	private AccountManager mAccountManager;
	private AccountCreationTask mAuthTask;
	private EditText mHostname;
	private Button nextButton;
	private Button prevButton;
	private View registrationFormPanel;
	private ListView trustedServers;

	private Account getAccount(String name) {
		for (Account account : mAccountManager.getAccounts()) {
			if (account.name.equals(name)) {
				return account;
			}
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_account_activity);
		mAccountManager = AccountManager.get(this);

		mHostname = (EditText) findViewById(R.id.hostname);
		dynamicForm = (DynamicForm) findViewById(R.id.registrationForm);
		hostSelectorPanel = findViewById(R.id.hostSelectPanel);
		registrationFormPanel = findViewById(R.id.registrationFormPanel);

		prevButton = (Button) findViewById(R.id.prev_button);
		prevButton.setOnClickListener(v -> showPage1());

		nextButton = (Button) findViewById(R.id.next_button);
		nextButton.setOnClickListener(v -> onNextButton());

		trustedServers = (ListView) findViewById(R.id.trustedServersList);

		final List<String> trustedServerItems = Arrays.asList(getResources().getStringArray(R.array.trusted_servers));

		ArrayAdapter<String> h = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
														  trustedServerItems);
		trustedServers.setAdapter(h);
		trustedServers.setOnItemClickListener((parent, view, position, id) -> {
			startConnection(trustedServerItems.get(position));
		});

		showPage1();
	}

	private void onNextButton() {
		String t = mHostname.getText().toString();
		if (t == null || t.isEmpty()) {
			mHostname.setError("Cannot be empty");
			return;
		}
		if (mAuthTask == null) {
			startConnection(mHostname.getText().toString().trim());
		} else {
			try {
				mAuthTask.useForm((UnifiedRegistrationForm) dynamicForm.getJabberDataElement());
			} catch (JaxmppException e) {
				Log.e(TAG, "Something goes wrong", e);
				e.printStackTrace();
			}
		}
	}

	private void showPage1() {
		if (mAuthTask != null) {
			mAuthTask.cancel(true);
			mAuthTask = null;
		}
		mHostname.setError(null);
		hostSelectorPanel.setVisibility(View.VISIBLE);
		registrationFormPanel.setVisibility(View.GONE);
		prevButton.setEnabled(false);
		nextButton.setText("Next");
	}

	private void showPage2() {
		hostSelectorPanel.setVisibility(View.GONE);
		registrationFormPanel.setVisibility(View.VISIBLE);
		prevButton.setEnabled(true);
		nextButton.setText("Register");
	}

	private void startConnection(String host) {
		mAuthTask = new AccountCreationTask();
		mAuthTask.execute(host);
	}

	public class AccountCreationTask
			extends AsyncTask<String, Integer, Boolean> {

		private AccountCreator accountCreator;
		private String hostname;
		private ProgressDialog progress;

		@Override
		protected Boolean doInBackground(String... hostname) {
			accountCreator = new AccountCreator(hostname[0]);
			this.hostname = hostname[0];
			accountCreator.getEventBus()
					.addHandler(
							InBandRegistrationModule.ReceivedRequestedFieldsHandler.ReceivedRequestedFieldsEvent.class,
							new InBandRegistrationModule.ReceivedRequestedFieldsHandler() {
								@Override
								public void onReceivedRequestedFields(SessionObject sessionObject, IQ iq,
																	  final UnifiedRegistrationForm unifiedRegistrationForm) {
									Log.w(TAG, "Registration form received");
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											showPage2();
											dynamicForm.setJabberDataElement(unifiedRegistrationForm);
											if (progress != null) {
												progress.hide();
												progress = null;
											}

										}
									});
								}
							});
			return accountCreator.register(CreateAccountActivity.this);
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
			Log.i(TAG, "Registration status= " + aBoolean);
			if (progress != null) {
				progress.hide();
				progress = null;
			}

			if (!aBoolean) {
				showPage1();

				SecureTrustManagerFactory.DataCertificateException deepException = LoginActivity.getCertException(
						accountCreator.getException());

				if (deepException != null) {
					X509Certificate[] chain = deepException.getChain();
					LoginActivity.showInvalidCertificateDialog(CreateAccountActivity.this, chain,
															   () -> startConnection(hostname));
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
					builder.setMessage("Connection error.").setPositiveButton(android.R.string.ok, null).show();
				}
				return;
			}

			try {
				UnifiedRegistrationForm form = (UnifiedRegistrationForm) dynamicForm.getJabberDataElement();
				final String username = ((TextSingleField) form.getField("username")).getFieldValue();
				final String mPassword = ((TextPrivateField) form.getField("password")).getFieldValue();
				final String mResource = "mobile";
				final Boolean mActive = true;

				String mXmppId = BareJID.bareJIDInstance(username, accountCreator.getJaxmpp()
						.getSessionObject()
						.getProperty(SessionObject.DOMAIN_NAME)).toString();

				Account account = getAccount(mXmppId);
				if (account == null) {
					account = new Account(mXmppId, Authenticator.ACCOUNT_TYPE);
					Log.d(TAG, "Adding account " + mXmppId + ":" + mPassword);
					mAccountManager.addAccountExplicitly(account, mPassword, null);
				} else {
					Log.d(TAG, "Updating account " + mXmppId + ":" + mPassword);
					mAccountManager.setPassword(account, mPassword);
				}
				// mAccountManager.setUserData(account, AccountsConstants.FIELD_NICKNAME, mNickname);
//				mAccountManager.setUserData(account, AccountsConstants.FIELD_HOSTNAME, mHostname);
				mAccountManager.setUserData(account, AccountsConstants.FIELD_RESOURCE, mResource);
				mAccountManager.setUserData(account, AccountsConstants.FIELD_ACTIVE, Boolean.toString(mActive));
				mAccountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED,
											Boolean.toString(true));

				final Intent intent = new Intent();
				intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mXmppId);
				intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
				// setAccountAuthenticatorResult(intent.getExtras());
				setResult(RESULT_OK, intent);

				Intent i = new Intent();
				i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
				i.putExtra(AccountManager.KEY_ACCOUNT_NAME, mXmppId);
				sendBroadcast(i);
			} catch (XMLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("LoginActivity", "Can't add account", e);
			}

		}

		@Override
		protected void onPreExecute() {
			showProgress(getResources().getString(R.string.login_checking));
		}

		private void showProgress(String msg) {
			this.progress = new ProgressDialog(CreateAccountActivity.this);
			progress.setMessage(msg);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progress.show();
		}

		public void useForm(UnifiedRegistrationForm jabberDataElement) throws JaxmppException {
			showProgress("Registering");

			InBandRegistrationModule m = accountCreator.getJaxmpp().getModule(InBandRegistrationModule.class);

			try {
				m.register(jabberDataElement, new AsyncCallback() {
					@Override
					public void onError(Stanza stanza, final XMPPException.ErrorCondition errorCondition)
							throws JaxmppException {
						Log.e(TAG, "Error: " + errorCondition);

						runOnUiThread(() -> {
							progress.hide();
							AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
							builder.setMessage("Registration error: " + errorCondition)
									.setPositiveButton(android.R.string.ok, null)
									.show();
						});

					}

					@Override
					public void onSuccess(Stanza stanza) throws JaxmppException {
						Log.e(TAG, "Success ");
						accountCreator.wakeup();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						runOnUiThread(() -> {
							progress.hide();
							AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
							builder.setMessage("No server response")
									.setPositiveButton(android.R.string.ok, null)
									.show();
						});
					}
				});
			} catch (JaxmppException e) {
				Log.e(TAG, "Cannot send registration form", e);
				e.printStackTrace();
			}

		}
	}

}
