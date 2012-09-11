package org.tigase.mobile.authenticator;

import java.util.HashMap;
import java.util.Map;

import org.tigase.mobile.Constants;
import org.tigase.mobile.Features;
import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.preferences.AccountAdvancedPreferencesActivity;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule.RegistrationEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	public class UserCreateAccountTask extends AsyncTask<String, Void, String> {

		private final Jaxmpp contact = new Jaxmpp();
		private Map<String, String> data = new HashMap<String, String>();

		private String errorMessage;

		private String token;

		/**
		 * @param params
		 *            mUsername, mPassword, mHostname, email
		 */
		@Override
		protected String doInBackground(final String... params) {

			final InBandRegistrationModule reg = contact.getModule(InBandRegistrationModule.class);
			contact.getProperties().setUserProperty(InBandRegistrationModule.IN_BAND_REGISTRATION_MODE_KEY, Boolean.TRUE);
			contact.getProperties().setUserProperty(SessionObject.SERVER_NAME, BareJID.bareJIDInstance(params[0]).getDomain());

			reg.addListener(new Listener<RegistrationEvent>() {

				@Override
				public void handleEvent(RegistrationEvent be) throws JaxmppException {
					if (be.getType() == InBandRegistrationModule.NotSupportedError) {
						token = null;
						errorMessage = "Registration not supported!";
						wakeup();
					} else if (be.getType() == InBandRegistrationModule.ReceivedError) {
						final ErrorCondition error = be.getStanza().getErrorCondition();
						if (error == null)
							errorMessage = "Registration error";
						else
							switch (error) {
							default:
								errorMessage = error.name();
								break;
							}
						wakeup();
					} else if (be.getType() == InBandRegistrationModule.ReceivedTimeout) {
						errorMessage = "Server doesn't responses";
						wakeup();
					}
				}
			});
			reg.addListener(InBandRegistrationModule.ReceivedRequestedFields,
					new Listener<InBandRegistrationModule.RegistrationEvent>() {

						@Override
						public void handleEvent(RegistrationEvent be) throws JaxmppException {
							reg.register(params[0], params[1], params[3], new AsyncCallback() {

								@Override
								public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
									token = null;
									if (error == null)
										errorMessage = "Registration error";
									else
										switch (error) {
										case conflict:
											errorMessage = "Username is not available. Choose another one.";
											break;
										default:
											errorMessage = error.name();
											break;
										}
									wakeup();
								}

								@Override
								public void onSuccess(Stanza responseStanza) throws JaxmppException {
									token = params[1];
									wakeup();
								}

								@Override
								public void onTimeout() throws JaxmppException {
									token = null;
									errorMessage = "Server doesn't responses";
									wakeup();
								}
							});
						}
					});

			try {
				contact.login(true);

				// here we process features available for account
				processJaxmppForFeatures(contact, data);

				return token;
			} catch (JaxmppException e) {
				Log.e(TAG, "Problem on password check", e);
				return null;
			} finally {
				try {
					contact.disconnect();
				} catch (Exception e) {
					Log.e(TAG, "Disconnect problem on password check", e);
				}
			}

		}

		@Override
		protected void onCancelled() {
			onAuthenticationCancel();
		}

		@Override
		protected void onPostExecute(final String authToken) {
			if (errorMessage != null)
				onCreationError(errorMessage);
			else
				onAuthenticationResult(authToken, data);
		}

		private void wakeup() {
			synchronized (contact) {
				contact.notify();
			}
		}
	}

	public class UserLoginTask extends AsyncTask<String, Void, String> {

		private Map<String, String> data = new HashMap<String, String>();

		/**
		 * @param params
		 *            mUsername, mPassword, mHostname
		 */
		@Override
		protected String doInBackground(String... params) {
			final Jaxmpp contact = new Jaxmpp();
			contact.getProperties().setUserProperty(SessionObject.USER_BARE_JID, BareJID.bareJIDInstance(params[0]));
			contact.getProperties().setUserProperty(SessionObject.PASSWORD, params[1]);
			try {
				contact.login(true);

				// here we process features available for account
				processJaxmppForFeatures(contact, data);

				return params[1];
			} catch (JaxmppException e) {
				Log.e(TAG, "Problem on password check", e);
				return null;
			} finally {
				try {
					contact.disconnect();
				} catch (Exception e) {
					Log.e(TAG, "Disconnect problem on password check", e);
				}
			}
		}

		@Override
		protected void onCancelled() {
			onAuthenticationCancel();
		}

		@Override
		protected void onPostExecute(final String authToken) {
			onAuthenticationResult(authToken, data);
		}
	}

	public static final String ACCOUNT_MODIFIED_MSG = "org.tigase.mobile.ACCOUNT_MODIFIED_MSG";

	private static final int CREATION_ERROR_DIALOG = 3;

	private static final int LOGIN_ERROR_DIALOG = 2;

	private static final int PAGE_ADD = 1;

	private static final int PAGE_CREATE = 2;

	private static final int PAGE_WELCOME = 0;

	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	private static final int PICK_ACCOUNT = 1;

	private final static int PROGRESS_DIALOG = 1;

	private static final String TAG = "AuthenticatorActivity";

	public static void processJaxmppForFeatures(JaxmppCore contact, Map<String, String> data) {
		String mobile = null;
		boolean mobileV1 = AccountAdvancedPreferencesActivity.isMobileAvailable(contact, Features.MOBILE_V1);
		if (mobileV1) {
			mobile = Features.MOBILE_V1;
		}
		boolean mobileV2 = AccountAdvancedPreferencesActivity.isMobileAvailable(contact, Features.MOBILE_V2);
		if (mobileV2) {
			mobile = Features.MOBILE_V2;
		}
		data.put(Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY, mobile);
	}

	private ViewFlipper flipper;

	private AccountManager mAccountManager;

	private AsyncTask<String, Void, String> mAuthTask;

	private boolean mConfirmCredentials;

	private String mHostname;

	private String mNickname;

	private String mPassword;

	private ProgressDialog mProgressDialog;

	private boolean mRequestNewAccount;

	private String mResource;

	private String mUsername;

	private TextView screenTitle;

	private void finishConfirmCredentials(boolean result) {
		Log.i(TAG, "finishConfirmCredentials()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
		mAccountManager.setPassword(account, mPassword);
		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_NICKNAME, mNickname);
		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_HOSTNAME, mHostname);
		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_RESOURCE, mResource);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	private void finishLogin(String authToken, Map<String, String> data) {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
		if (mRequestNewAccount) {
			mAccountManager.addAccountExplicitly(account, mPassword, null);
			// Set contacts sync for this account.
			ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
			// ContentResolver.setSyncAutomatically(account,
			// ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, mPassword);
			ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
			// ContentResolver.setSyncAutomatically(account,
			// ContactsContract.AUTHORITY, true);
		}

		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_NICKNAME, mNickname);
		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_HOSTNAME, mHostname);
		mAccountManager.setUserData(account, AccountsTableMetaData.FIELD_RESOURCE, mResource);

		if (data != null) {
			for (String key : data.keySet()) {
				String value = data.get(key);
				mAccountManager.setUserData(account, key, value);
			}
		}

		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);

		Intent i = new Intent();
		i.setAction(ACCOUNT_MODIFIED_MSG);
		i.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		sendBroadcast(i);

		finish();
	}

	private JID getJID(EditText mUsernameEdit, View v) {
		try {
			if (mUsernameEdit.getText().toString().contains("@")) {
				return JID.jidInstance(mUsernameEdit.getText().toString());
			}
		} catch (Exception e) {
		}

		return JID.jidInstance(mUsernameEdit.getText().toString(), getServerName(v));
	}

	protected String getServerName(View v) {
		final Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		final String[] accounts = getResources().getStringArray(R.array.free_account_hostnames);

		if (mHostnameSelector.getVisibility() == View.VISIBLE
				&& mHostnameSelector.getSelectedItemPosition() < accounts.length - 1) {
			return mHostnameSelector.getSelectedItem().toString();
		}

		final EditText mHostnameSelectorEdit = (EditText) v.findViewById(R.id.newAccountHostnameSelectorEdit);
		return mHostnameSelectorEdit.getText().toString();
	}

	protected void handleLogin(View v, boolean requestNewAccount, final AsyncTask<String, Void, String> authTask) {
		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mPasswordConfirmEdit = (EditText) v.findViewById(R.id.newAccountPassowrdConfirm);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);
		EditText mEmailEdit = (EditText) v.findViewById(R.id.newAccountEmail);

		if (TextUtils.isEmpty(mUsernameEdit.getText().toString())) {
			mUsernameEdit.setError("Field can't be empty");
			return;
		}
		try {
			JID j = getJID(mUsernameEdit, v);

			if (j.getLocalpart() != null && j.getLocalpart().length() > 0 && j.getDomain() != null
					&& j.getDomain().length() > 0) {
				mUsername = j.toString();
			} else {
				mUsernameEdit.setError("Invalid username");
				return;
			}
		} catch (Exception e) {
			mUsernameEdit.setError("Invalid username");
			return;
		}

		if (mPasswordConfirmEdit != null
				&& !TextUtils.equals(mPasswordEdit.getText().toString(), mPasswordConfirmEdit.getText().toString())) {
			mPasswordConfirmEdit.setError("Passwords are not match");
			return;
		}

		if (TextUtils.isEmpty(mPasswordEdit.getText().toString())) {
			mPasswordEdit.setError("Field can't be empty");
			return;
		}

		// if (mEmailEdit != null &&
		// TextUtils.isEmpty(mEmailEdit.getText().toString())) {
		// mEmailEdit.setError("Field can't be empty");
		// return;
		// }

		mPassword = mPasswordEdit.getText().toString();
		mNickname = mNicknameEdit.getText().toString();
		mHostname = mHostnameEdit.getText().toString();
		mResource = mResourceEdit.getText().toString();
		final String mEmail = mEmailEdit == null ? null : mEmailEdit.getText().toString();

		showDialog(PROGRESS_DIALOG);

		mAuthTask = authTask;
		mAuthTask.execute(mUsername, mPassword, mHostname, mEmail);
	}

	private void hideProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_ACCOUNT) {
			if (resultCode == RESULT_OK) {
				Intent intent = getIntent();
				if (intent == null)
					return;
				String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accName == null) {
					this.finish();
					return;
				}
				Account account = null;
				for (Account acc : mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
					if (acc.name.equals(accName)) {
						account = acc;
						break;
					}
				}

				intent = intent.putExtra("account", account);
				this.setIntent(intent);

				final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				flipper.addView(prepareWelcomeScreen(inflater));
				flipper.addView(prepareAddEditAccount(account, inflater));
				flipper.addView(prepareCreateAccount(inflater));

				flipper.setDisplayedChild(account == null ? PAGE_CREATE : PAGE_ADD);
			} else {
				this.finish();
			}
		}
	}

	protected void onAuthenticationCancel() {
		Log.i(TAG, "onAuthenticationCancel()");

		mAuthTask = null;

		hideProgress();
	}

	protected void onAuthenticationResult(String authToken, Map<String, String> data) {
		boolean success = ((authToken != null) && (authToken.length() > 0));
		Log.i(TAG, "onAuthenticationResult(" + success + ")");

		// Our task is complete, so clear it out
		mAuthTask = null;

		// Hide the progress dialog
		hideProgress();

		if (success) {
			if (!mConfirmCredentials) {
				finishLogin(authToken, data);
			} else {
				finishConfirmCredentials(success);
			}
		} else {
			Log.e(TAG, "onAuthenticationResult: failed to authenticate");
			showDialog(LOGIN_ERROR_DIALOG);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mAccountManager = AccountManager.get(this);

		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.account_add_screen);

		this.screenTitle = (TextView) findViewById(R.id.screenTitle);
		this.flipper = (ViewFlipper) findViewById(R.id.accounts_flipper);

		final Intent intent = getIntent();

		final Account account;
		if (intent.getParcelableExtra("account") != null) {
			account = (Account) intent.getParcelableExtra("account");
		} else if (intent.getStringExtra("account_jid") != null) {
			String jid = intent.getStringExtra("account_jid");
			Account[] accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
			Account acc = null;
			for (Account tmpacc : accounts) {
				if (tmpacc.name.equals(jid)) {
					acc = tmpacc;
				}
			}
			account = acc;
		} else {
			account = null;
		}

		if (account != null) {
			screenTitle.setText("Account edit");
			if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
				startChooseAccountIceCream(account);
			} else {
				final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				flipper.addView(prepareWelcomeScreen(inflater));
				flipper.addView(prepareAddEditAccount(account, inflater));
				flipper.addView(prepareCreateAccount(inflater));
				flipper.setDisplayedChild(PAGE_ADD);
			}
		} else {
			final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			flipper.addView(prepareWelcomeScreen(inflater));
			flipper.addView(prepareAddEditAccount(account, inflater));
			flipper.addView(prepareCreateAccount(inflater));
		}

	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case CREATION_ERROR_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String msg = args.getString("msg");
			if (msg == null)
				msg = "unknown";
			builder.setMessage(msg).setCancelable(true);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle("Error");
			builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			return builder.create();

		}
		case LOGIN_ERROR_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Invalid username or password.").setCancelable(true);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle("Error");
			builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			return builder.create();
		}
		case PROGRESS_DIALOG: {
			final ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getText(R.string.ui_activity_authenticating));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.i(TAG, "user cancelling authentication");
					if (mAuthTask != null) {
						mAuthTask.cancel(true);
					}
				}
			});

			mProgressDialog = dialog;
			return dialog;
		}
		default:
			return null;
		}

	}

	protected void onCreationError(String errorMessage) {
		Bundle b = new Bundle();
		b.putString("msg", errorMessage);
		hideProgress();

		Dialog dlg = onCreateDialog(CREATION_ERROR_DIALOG, b);
		if (dlg != null) {
			dlg.show();
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Read values from the "savedInstanceState"-object and put them in your
		// textview
		String tmp;
		flipper.setDisplayedChild(savedInstanceState.getInt("page", PAGE_WELCOME));
		tmp = savedInstanceState.getString("");
		if (tmp != null)
			screenTitle.setText(tmp);

		mUsername = savedInstanceState.getString("mUsername");

		EditText newAccountUsername = (EditText) findViewById(R.id.newAccountUsername);
		tmp = savedInstanceState.getString("newAccountUsername");
		if (newAccountUsername != null && tmp != null)
			newAccountUsername.setText(tmp);

		Spinner newAccountHostnameSelector = (Spinner) findViewById(R.id.newAccountHostnameSelector);
		int i = savedInstanceState.getInt("newAccountHostnameSelector", 0);
		if (newAccountHostnameSelector != null)
			newAccountHostnameSelector.setSelection(i);

		EditText newAccountHostnameSelectorEdit = (EditText) findViewById(R.id.newAccountHostnameSelectorEdit);
		tmp = savedInstanceState.getString("newAccountHostnameSelectorEdit");
		if (newAccountHostnameSelectorEdit != null && tmp != null)
			newAccountHostnameSelectorEdit.setText(tmp);

		EditText newAccountPassowrd = (EditText) findViewById(R.id.newAccountPassowrd);
		tmp = savedInstanceState.getString("newAccountPassowrd");
		if (newAccountPassowrd != null && tmp != null)
			newAccountPassowrd.setText(tmp);
		EditText newAccountPassowrdConfirm = (EditText) findViewById(R.id.newAccountPassowrdConfirm);
		tmp = savedInstanceState.getString("newAccountPassowrdConfirm");
		if (newAccountPassowrdConfirm != null && tmp != null)
			newAccountPassowrdConfirm.setText(tmp);
		EditText newAccountResource = (EditText) findViewById(R.id.newAccountResource);
		tmp = savedInstanceState.getString("newAccountResource");
		if (newAccountResource != null && tmp != null)
			newAccountResource.setText(tmp);
		EditText newAccountHostname = (EditText) findViewById(R.id.newAccountHostname);
		tmp = savedInstanceState.getString("newAccountHostname");
		if (newAccountHostname != null && tmp != null)
			newAccountHostname.setText(tmp);
		EditText newAccountNickname = (EditText) findViewById(R.id.newAccountNickname);
		tmp = savedInstanceState.getString("newAccountNickname");
		if (newAccountNickname != null && tmp != null)
			newAccountNickname.setText(tmp);
		EditText newAccountEmail = (EditText) findViewById(R.id.newAccountEmail);
		tmp = savedInstanceState.getString("newAccountEmail");
		if (newAccountEmail != null && tmp != null)
			newAccountEmail.setText(tmp);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the values you need from your textview into "outState"-object

		outState.putInt("page", flipper.getDisplayedChild());
		outState.putCharSequence("pageTitle", screenTitle.getText());
		outState.putString("mUsername", mUsername);

		EditText newAccountUsername = (EditText) findViewById(R.id.newAccountUsername);
		if (newAccountUsername != null)
			outState.putString("newAccountUsername", newAccountUsername.getText().toString());

		Spinner newAccountHostnameSelector = (Spinner) findViewById(R.id.newAccountHostnameSelector);
		if (newAccountHostnameSelector != null)
			outState.putInt("newAccountHostnameSelector", newAccountHostnameSelector.getSelectedItemPosition());

		EditText newAccountHostnameSelectorEdit = (EditText) findViewById(R.id.newAccountHostnameSelectorEdit);
		if (newAccountHostnameSelectorEdit != null)
			outState.putString("newAccountHostnameSelectorEdit", newAccountHostnameSelectorEdit.getText().toString());

		EditText newAccountPassowrd = (EditText) findViewById(R.id.newAccountPassowrd);
		if (newAccountPassowrd != null)
			outState.putString("newAccountPassowrd", newAccountPassowrd.getText().toString());

		EditText newAccountPassowrdConfirm = (EditText) findViewById(R.id.newAccountPassowrdConfirm);
		if (newAccountPassowrdConfirm != null)
			outState.putString("newAccountPassowrdConfirm", newAccountPassowrdConfirm.getText().toString());

		EditText newAccountResource = (EditText) findViewById(R.id.newAccountResource);
		if (newAccountResource != null)
			outState.putString("newAccountResource", newAccountResource.getText().toString());

		EditText newAccountHostname = (EditText) findViewById(R.id.newAccountHostname);
		if (newAccountHostname != null)
			outState.putString("newAccountHostname", newAccountHostname.getText().toString());

		EditText newAccountNickname = (EditText) findViewById(R.id.newAccountNickname);
		if (newAccountNickname != null)
			outState.putString("newAccountNickname", newAccountNickname.getText().toString());

		EditText newAccountEmail = (EditText) findViewById(R.id.newAccountEmail);
		if (newAccountEmail != null)
			outState.putString("newAccountEmail", newAccountEmail.getText().toString());

		super.onSaveInstanceState(outState);
	}

	private View prepareAddEditAccount(final Account account, final LayoutInflater inflater) {
		final View v = inflater.inflate(R.layout.account_edit_dialog, null);

		final Intent intent = getIntent();
		if (account != null) {
			mUsername = account.name;
			mPassword = mAccountManager.getPassword(account);
			mNickname = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_NICKNAME);
			mHostname = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_HOSTNAME);
			mResource = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_RESOURCE);
		}

		final Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		final String[] accounts = getResources().getStringArray(R.array.free_account_hostnames);

		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		final EditText mHostnameSelectorEdit = (EditText) v.findViewById(R.id.newAccountHostnameSelectorEdit);

		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);
		if (!TextUtils.isEmpty(mUsername)) {
			BareJID j = BareJID.bareJIDInstance(mUsername);
			mHostnameSelector.setVisibility(View.GONE);
			mHostnameSelectorEdit.setVisibility(View.VISIBLE);
			mHostnameSelectorEdit.setText(j.getDomain());
			mHostnameSelectorEdit.setEnabled(false);
			mUsernameEdit.setText(j.getLocalpart());
		} else {
			mHostnameSelector.setVisibility(View.VISIBLE);
			mHostnameSelectorEdit.setVisibility(View.GONE);
			mHostnameSelectorEdit.setEnabled(true);
		}
		if (!TextUtils.isEmpty(mPassword))
			mPasswordEdit.setText(mPassword);
		if (!TextUtils.isEmpty(mNickname))
			mNicknameEdit.setText(mNickname);
		if (!TextUtils.isEmpty(mHostname))
			mHostnameEdit.setText(mHostname);
		if (!TextUtils.isEmpty(mResource))
			mResourceEdit.setText(mResource);

		mUsernameEdit.setEnabled(mUsername == null);
		mHostnameSelector.setEnabled(mUsername == null);

		mUsernameEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.toString().contains("@")) {
					updateVisibility(View.GONE, mHostnameSelector, mHostnameSelectorEdit);
					try {
						JID j = JID.jidInstance(s.toString());
						if (j.getLocalpart() != null && j.getLocalpart().length() > 0 && j.getDomain() != null
								&& j.getDomain().length() > 0) {

							final String[] accounts = getResources().getStringArray(R.array.free_account_hostnames);
							mHostnameSelector.setSelection(accounts.length - 1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					updateVisibility(View.VISIBLE, mHostnameSelector, mHostnameSelectorEdit);
					int position = mHostnameSelector.getSelectedItemPosition();
					if (position != accounts.length - 1) {
						mHostnameSelectorEdit.setVisibility(View.GONE);
					} else {
						mHostnameSelectorEdit.setVisibility(View.VISIBLE);
					}
				}
			}
		});

		mHostnameSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (position != accounts.length - 1) {
					mHostnameSelectorEdit.setVisibility(View.GONE);
				} else {
					mHostnameSelectorEdit.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});

		Button cancelButton = (Button) v.findViewById(R.id.newAccountcancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onAuthenticationCancel();
				finish();
			}
		});
		Button loginButton = (Button) v.findViewById(R.id.newAccountAddButton);
		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View vv) {
				handleLogin(v, mRequestNewAccount, new UserLoginTask());
			}
		});

		return v;
	}

	private View prepareCreateAccount(LayoutInflater inflater) {
		final View v = inflater.inflate(R.layout.account_create_dialog, null);

		final EditText mHostnameSelectorEdit = (EditText) v.findViewById(R.id.newAccountHostnameSelectorEdit);
		final Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		final String[] accounts = getResources().getStringArray(R.array.free_account_hostnames);

		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		mUsernameEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s != null && s.toString().contains("@")) {
					updateVisibility(View.GONE, mHostnameSelector, mHostnameSelectorEdit);
					try {
						JID j = JID.jidInstance(s.toString());
						if (j.getLocalpart() != null && j.getLocalpart().length() > 0 && j.getDomain() != null
								&& j.getDomain().length() > 0) {

							final String[] accounts = getResources().getStringArray(R.array.free_account_hostnames);
							mHostnameSelector.setSelection(accounts.length - 1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					updateVisibility(View.VISIBLE, mHostnameSelector, mHostnameSelectorEdit);
					int position = mHostnameSelector.getSelectedItemPosition();
					if (position != accounts.length - 1) {
						mHostnameSelectorEdit.setVisibility(View.GONE);
					} else {
						mHostnameSelectorEdit.setVisibility(View.VISIBLE);
					}
				}
			}
		});
		mHostnameSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (position != accounts.length - 1) {
					mHostnameSelectorEdit.setVisibility(View.GONE);
				} else {
					mHostnameSelectorEdit.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});

		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);

		mHostnameSelector.setVisibility(View.VISIBLE);
		mHostnameSelectorEdit.setVisibility(View.GONE);
		mHostnameSelectorEdit.setEnabled(true);

		// mHostnameSelector.setVisibility(FREE_VERSION && mUsername == null ?
		// View.VISIBLE : View.GONE);
		// mHostnameEdit.setVisibility(!FREE_VERSION ? View.VISIBLE :
		// View.GONE);

		Button cancelButton = (Button) v.findViewById(R.id.newAccountcancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onAuthenticationCancel();
				finish();
			}
		});
		Button loginButton = (Button) v.findViewById(R.id.newAccountAddButton);
		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View vv) {
				handleLogin(v, mRequestNewAccount, new UserCreateAccountTask());
			}
		});

		return v;
	}

	private View prepareWelcomeScreen(final LayoutInflater inflater) {
		View v = inflater.inflate(R.layout.welcome_screen, null);

		TextView tos = (TextView) v.findViewById(R.id.aboutTermsOfService);
		tos.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.termsOfServiceURL) + "'>"
				+ getResources().getString(R.string.termsOfService) + "</a>"));
		tos.setMovementMethod(LinkMovementMethod.getInstance());

		TextView pp = (TextView) v.findViewById(R.id.aboutPrivacyPolicy);
		pp.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.privacyPolicyURL) + "'>"
				+ getResources().getString(R.string.privacyPolicy) + "</a>"));
		pp.setMovementMethod(LinkMovementMethod.getInstance());

		Button b = (Button) v.findViewById(R.id.welcomeScreenAddAccountsButton);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				screenTitle.setText("Login");
				mRequestNewAccount = mUsername == null;
				flipper.setDisplayedChild(PAGE_ADD);
			}
		});
		b = (Button) v.findViewById(R.id.welcomeScreenCreateAccountsButton);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				screenTitle.setText("Create account");
				mRequestNewAccount = true;
				flipper.setDisplayedChild(PAGE_CREATE);
			}
		});

		return v;
	}

	@TargetApi(14)
	private void startChooseAccountIceCream(final Account account) {
		Intent intentChooser = AccountManager.newChooseAccountIntent(account, null, new String[] { Constants.ACCOUNT_TYPE },
				false, null, null, null, null);
		this.startActivityForResult(intentChooser, PICK_ACCOUNT);
	}

	private void updateVisibility(int visibility, View... views) {
		for (View view : views) {
			if (view.getVisibility() != visibility) {
				view.setVisibility(visibility);
			}
		}
	}

}
