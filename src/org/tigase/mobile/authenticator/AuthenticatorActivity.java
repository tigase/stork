package org.tigase.mobile.authenticator;

import org.tigase.mobile.Constants;
import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
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

		private String errorMessage;

		private String token;

		/**
		 * @param params
		 *            mUsername, mPassword, mHostname, email
		 */
		@Override
		protected String doInBackground(final String... params) {

			final InBandRegistrationModule reg = contact.getModulesManager().getModule(InBandRegistrationModule.class);
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
				onAuthenticationResult(authToken);
		}

		private void wakeup() {
			synchronized (contact) {
				contact.notify();
			}
		}
	}

	public class UserLoginTask extends AsyncTask<String, Void, String> {

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
			onAuthenticationResult(authToken);
		}
	}

	public static final String ACCOUNT_MODIFIED_MSG = "org.tigase.mobile.ACCOUNT_MODIFIED_MSG";

	private static final int CREATION_ERROR_DIALOG = 3;

	private static final boolean FREE_VERSION = false;

	private static final int LOGIN_ERROR_DIALOG = 2;

	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	private final static int PROGRESS_DIALOG = 1;

	private static final String TAG = "AuthenticatorActivity";

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

	private void finishLogin(String authToken) {
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

	protected void handleLogin(View v, boolean requestNewAccount, final AsyncTask<String, Void, String> authTask) {
		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mPasswordConfirmEdit = (EditText) v.findViewById(R.id.newAccountPassowrdConfirm);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);
		EditText mEmailEdit = (EditText) v.findViewById(R.id.newAccountEmail);

		if (requestNewAccount) {
			if (TextUtils.isEmpty(mUsernameEdit.getText().toString())) {
				mUsernameEdit.setError("Field can't be empty");
				return;
			}
			String username = mUsernameEdit.getText().toString();
			if (FREE_VERSION) {
				username += "@" + mHostnameSelector.getSelectedItem();
			}
			try {
				JID j = JID.jidInstance(username);

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
		} else
			mUsername = mUsernameEdit.getText().toString();

		if (mPasswordConfirmEdit != null
				&& !TextUtils.equals(mPasswordEdit.getText().toString(), mPasswordConfirmEdit.getText().toString())) {
			mPasswordConfirmEdit.setError("Passwords are not match");
			return;
		}

		if (TextUtils.isEmpty(mPasswordEdit.getText().toString())) {
			mPasswordEdit.setError("Field can't be empty");
			return;
		}

		if (mEmailEdit != null && TextUtils.isEmpty(mEmailEdit.getText().toString())) {
			mEmailEdit.setError("Field can't be empty");
			return;
		}

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

	protected void onAuthenticationCancel() {
		Log.i(TAG, "onAuthenticationCancel()");

		mAuthTask = null;

		hideProgress();
	}

	protected void onAuthenticationResult(String authToken) {
		boolean success = ((authToken != null) && (authToken.length() > 0));
		Log.i(TAG, "onAuthenticationResult(" + success + ")");

		// Our task is complete, so clear it out
		mAuthTask = null;

		// Hide the progress dialog
		hideProgress();

		if (success) {
			if (!mConfirmCredentials) {
				finishLogin(authToken);
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
		final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		flipper.addView(prepareWelcomeScreen(inflater));
		flipper.addView(prepareAddAccount(inflater));
		flipper.addView(prepareCreateAccount(inflater));

		final Intent intent = getIntent();
		final Account account = intent.getExtras() == null ? null : (Account) intent.getExtras().get("account");
		if (account != null) {
			screenTitle.setText("Account edit");
			flipper.setDisplayedChild(1);
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
		showDialog(CREATION_ERROR_DIALOG, b);
	}

	private View prepareAddAccount(final LayoutInflater inflater) {
		final View v = inflater.inflate(R.layout.account_edit_dialog, null);

		final Intent intent = getIntent();
		final Account account = intent.getExtras() == null ? null : (Account) intent.getExtras().get("account");
		if (account != null) {
			mUsername = account.name;
			mPassword = mAccountManager.getPassword(account);
			mNickname = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_NICKNAME);
			mHostname = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_HOSTNAME);
			mResource = mAccountManager.getUserData(account, AccountsTableMetaData.FIELD_RESOURCE);
		}
		mRequestNewAccount = mUsername == null;

		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);
		if (!TextUtils.isEmpty(mUsername))
			mUsernameEdit.setText(mUsername);
		if (!TextUtils.isEmpty(mPassword))
			mPasswordEdit.setText(mPassword);
		if (!TextUtils.isEmpty(mNickname))
			mNicknameEdit.setText(mNickname);
		if (!TextUtils.isEmpty(mHostname))
			mHostnameEdit.setText(mHostname);
		if (!TextUtils.isEmpty(mResource))
			mResourceEdit.setText(mResource);

		mUsernameEdit.setEnabled(mUsername == null);

		mHostnameSelector.setVisibility(FREE_VERSION && mUsername == null ? View.VISIBLE : View.GONE);
		mHostnameEdit.setVisibility(!FREE_VERSION ? View.VISIBLE : View.GONE);

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

		mRequestNewAccount = true;

		EditText mUsernameEdit = (EditText) v.findViewById(R.id.newAccountUsername);
		Spinner mHostnameSelector = (Spinner) v.findViewById(R.id.newAccountHostnameSelector);
		EditText mPasswordEdit = (EditText) v.findViewById(R.id.newAccountPassowrd);
		EditText mResourceEdit = (EditText) v.findViewById(R.id.newAccountResource);
		EditText mNicknameEdit = (EditText) v.findViewById(R.id.newAccountNickname);
		EditText mHostnameEdit = (EditText) v.findViewById(R.id.newAccountHostname);

		mUsernameEdit.setEnabled(mUsername == null);

		mHostnameSelector.setVisibility(FREE_VERSION && mUsername == null ? View.VISIBLE : View.GONE);
		mHostnameEdit.setVisibility(!FREE_VERSION ? View.VISIBLE : View.GONE);

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

		Button b = (Button) v.findViewById(R.id.welcomeScreenAddAccountsButton);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				screenTitle.setText("Login");
				flipper.setDisplayedChild(1);
			}
		});
		b = (Button) v.findViewById(R.id.welcomeScreenCreateAccountsButton);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				screenTitle.setText("Create account");
				flipper.setDisplayedChild(2);
			}
		});

		return v;
	}

}
