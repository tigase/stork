package org.tigase.mobile.authenticator;

import org.tigase.mobile.Constants;
import org.tigase.mobile.R;
import org.tigase.mobile.db.AccountsTableMetaData;

import tigase.jaxmpp.core.client.JID;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	public class UserLoginTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			try {
				return mPassword;
			} catch (Exception ex) {
				Log.e(TAG, "UserLoginTask.doInBackground: failed to authenticate");
				Log.i(TAG, ex.toString());
				return null;
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

	private static final boolean FREE_VERSION = true;

	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	private static final String TAG = "AuthenticatorActivity";

	private AccountManager mAccountManager;

	private UserLoginTask mAuthTask = null;

	private Boolean mConfirmCredentials = false;

	private final Handler mHandler = new Handler();

	private String mHostname;

	private EditText mHostnameEdit;

	private Spinner mHostnameSelector;

	private String mNickname;

	private EditText mNicknameEdit;

	private String mPassword;

	private EditText mPasswordEdit;

	private ProgressDialog mProgressDialog = null;

	protected boolean mRequestNewAccount = false;

	private String mResource;

	private EditText mResourceEdit;

	private String mUsername;

	private EditText mUsernameEdit;

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
		finish();
	}

	private CharSequence getMessage() {
		getString(R.string.label);
		if (TextUtils.isEmpty(mUsername)) {
			// If no username, then we ask the user to log in using an
			// appropriate service.
			final CharSequence msg = getText(R.string.login_activity_newaccount_text);
			return msg;
		}
		if (TextUtils.isEmpty(mPassword)) {
			return getText(R.string.login_activity_loginfail_text_pwmissing);
		}
		return null;
	}

	public void handleLogin(View view) {
		if (mRequestNewAccount) {
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
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage("Enter correct username").setCancelable(true).setIcon(android.R.drawable.ic_dialog_alert);
					builder.create();
				}
			} catch (Exception e) {
			}

		}

		mPassword = mPasswordEdit.getText().toString();
		mNickname = mNicknameEdit.getText().toString();
		mHostname = mHostnameEdit.getText().toString();
		mResource = mResourceEdit.getText().toString();
		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Please set login data").setCancelable(true).setIcon(android.R.drawable.ic_dialog_alert);
			builder.create();
		} else {
			showProgress();
			mAuthTask = new UserLoginTask();
			mAuthTask.execute();
		}
	}

	private void hideProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	public void onAuthenticationCancel() {
		Log.i(TAG, "onAuthenticationCancel()");

		mAuthTask = null;

		hideProgress();
	}

	public void onAuthenticationResult(String authToken) {

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
			if (mRequestNewAccount) {
				// mMessage.setText(getText(R.string.login_activity_loginfail_text_both));
			} else {
				// mMessage.setText(getText(R.string.login_activity_loginfail_text_pwonly));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {

		Log.i(TAG, "onCreate(" + icicle + ")");
		super.onCreate(icicle);

		mAccountManager = AccountManager.get(this);
		Log.i(TAG, "loading data from Intent");
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
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);
		Log.i(TAG, "    request new: " + mRequestNewAccount);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.account_edit_dialog);
		mUsernameEdit = (EditText) findViewById(R.id.newAccountUsername);
		mHostnameSelector = (Spinner) findViewById(R.id.newAccountHostnameSelector);
		mPasswordEdit = (EditText) findViewById(R.id.newAccountPassowrd);
		mResourceEdit = (EditText) findViewById(R.id.newAccountResource);
		mNicknameEdit = (EditText) findViewById(R.id.newAccountNickname);
		mHostnameEdit = (EditText) findViewById(R.id.newAccountHostname);
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
		// Disable posibility to change username of existing account
		// because after editing account settings we are back to account
		// page with old username presented as account name!!
		mUsernameEdit.setEnabled(mUsername == null);
		mHostnameSelector.setVisibility(FREE_VERSION && mUsername == null ? View.VISIBLE : View.GONE);
		mHostnameEdit.setVisibility(!FREE_VERSION ? View.VISIBLE : View.GONE);

		final Button addButton = (Button) findViewById(R.id.newAccountAddButton);
		final Button cancelButton = (Button) findViewById(R.id.newAccountcancelButton);
		addButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleLogin(v);
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
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

	private void showProgress() {
		try {
			showDialog(0);
		} catch (Exception ex) {
			// Error in Android SDK 7
		}
	}
}
