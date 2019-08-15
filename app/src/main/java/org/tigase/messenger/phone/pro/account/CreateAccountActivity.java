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

package org.tigase.messenger.phone.pro.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
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
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.TextPrivateField;
import tigase.jaxmpp.core.client.xmpp.forms.TextSingleField;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.UnifiedRegistrationForm;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.lang.ref.WeakReference;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_account_activity);
		mAccountManager = AccountManager.get(this);

		mHostname = findViewById(R.id.hostname);
		dynamicForm = findViewById(R.id.registrationForm);
		hostSelectorPanel = findViewById(R.id.hostSelectPanel);
		registrationFormPanel = findViewById(R.id.registrationFormPanel);

		prevButton = findViewById(R.id.prev_button);
		prevButton.setOnClickListener(v -> showPage1());

		nextButton = findViewById(R.id.next_button);
		nextButton.setOnClickListener(v -> onNextButton());

		trustedServers = findViewById(R.id.trustedServersList);

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
		if (mAuthTask == null && (t == null || t.isEmpty())) {
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
		mAuthTask = new AccountCreationTask(this, getApplicationContext(), mAccountManager);
		mAuthTask.execute(host);
	}

	public static class AccountCreationTask
			extends AsyncTask<String, Integer, Boolean> {

		private final WeakReference<CreateAccountActivity> activity;
		private final Context context;
		private final AccountManager mAccountManager;
		private AccountCreator accountCreator;
		private String hostname;
		private ProgressDialog progress;

		public AccountCreationTask(CreateAccountActivity activity, Context context, AccountManager mAccountManager) {
			this.activity = new WeakReference<>(activity);
			this.context = context;
			this.mAccountManager = mAccountManager;
		}

		public void useForm(UnifiedRegistrationForm jabberDataElement) throws JaxmppException {
			showProgress("Registering");

			InBandRegistrationModule m = accountCreator.getJaxmpp().getModule(InBandRegistrationModule.class);

			try {
				if (jabberDataElement == null) {
					runOnUiThread(() -> {
						progress.dismiss();
						progress = null;
						AlertDialog.Builder builder = new AlertDialog.Builder(activity.get());
						builder.setMessage("Server doesn't support registration.")
								.setPositiveButton(android.R.string.ok, null)
								.show();
					});
					return;
				}

				m.register(jabberDataElement, new AsyncCallback() {
					@Override
					public void onError(Stanza stanza, final XMPPException.ErrorCondition errorCondition)
							throws JaxmppException {
						Log.e(TAG, "Error: " + errorCondition);

						runOnUiThread(() -> {
							progress.dismiss();
							progress = null;
							AlertDialog.Builder builder = new AlertDialog.Builder(activity.get());
							builder.setMessage("Registration error: " + errorCondition)
									.setPositiveButton(android.R.string.ok, null)
									.show();
						});

					}

					@Override
					public void onSuccess(Stanza stanza) throws JaxmppException {
						Log.e(TAG, "Success ");
						accountCreator.success();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						runOnUiThread(() -> {
							progress.dismiss();
							progress = null;
							AlertDialog.Builder builder = new AlertDialog.Builder(activity.get());
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
									runInActivity(ac -> ac.runOnUiThread(() -> {
										ac.showPage2();
										ac.dynamicForm.setJabberDataElement(unifiedRegistrationForm);
										if (progress != null) {
											progress.dismiss();
											progress = null;
										}
									}));
								}
							});
			return accountCreator.register(context);
		}

		@Override
		protected void onPostExecute(Boolean success) {
			Log.i(TAG, "Registration status= " + success);
			if (progress != null) {
				progress.dismiss();
				progress = null;
			}

			if (!success) {
				runInActivity(CreateAccountActivity::showPage1);

				SecureTrustManagerFactory.DataCertificateException deepException = LoginActivity.getCertException(
						accountCreator.getException());

				if (deepException != null) {
					X509Certificate[] chain = deepException.getChain();
					LoginActivity.showInvalidCertificateDialog(activity.get(), chain,
															   () -> runInActivity(a -> a.startConnection(hostname)));
				} else {
					final String msg;
					if (accountCreator.getErrorMessage() == null || accountCreator.getErrorMessage().isEmpty()) {
						msg = "Connection error.";
					} else {
						msg = accountCreator.getErrorMessage();
					}
					runOnUiThread(() -> {
						AlertDialog.Builder builder = new AlertDialog.Builder(activity.get());
						builder.setMessage(msg).setPositiveButton(android.R.string.ok, null).show();
					});
				}
				return;
			}

			runInActivity(a -> {
				try {
					UnifiedRegistrationForm form = (UnifiedRegistrationForm) a.dynamicForm.getJabberDataElement();
					final String username = ((TextSingleField) form.getField("username")).getFieldValue();
					final String mPassword = ((TextPrivateField) form.getField("password")).getFieldValue();
					final String mResource = "mobile";
					final Boolean mActive = true;

					String mXmppId = BareJID.bareJIDInstance(username, accountCreator.getJaxmpp()
							.getSessionObject()
							.getProperty(SessionObject.DOMAIN_NAME)).toString();

					Account account = AccountHelper.getAccount(mAccountManager, mXmppId);
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
					a.setResult(RESULT_OK, intent);

					Intent i = new Intent();
					i.setAction(LoginActivity.ACCOUNT_MODIFIED_MSG);
					i.putExtra(AccountManager.KEY_ACCOUNT_NAME, mXmppId);
					a.sendBroadcast(i);
				} catch (Exception e) {
					Log.e("LoginActivity", "Can't add account", e);
				}
				a.finish();
			});
		}

		@Override
		protected void onPreExecute() {
			showProgress(context.getResources().getString(R.string.login_checking));
		}

		private void runInActivity(Fnc<CreateAccountActivity> f) {
			final CreateAccountActivity x = activity.get();
			if (x != null && !x.isFinishing()) {
				f.run(x);
			}
		}

		private void runOnUiThread(Runnable runnable) {
			runInActivity(activity1 -> activity1.runOnUiThread(runnable));
		}

		private void showProgress(String msg) {
			if (this.progress == null) {
				runInActivity(a -> {
					this.progress = new ProgressDialog(a);
					progress.setMessage(msg);
					progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progress.setIndeterminate(true);
					progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progress.show();
				});
			}
		}

		interface Fnc<T> {

			void run(T activity);
		}
	}

}
