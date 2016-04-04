/*
 * LoginActivity.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.MobileModeFeature;
import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

	public static final String ACCOUNT_MODIFIED_MSG = "org.tigase.messenger.phone.pro.ACCOUNT_MODIFIED_MSG";

	/**
	 * Id to identity READ_CONTACTS permission request.
	 */
	private static final int REQUEST_READ_CONTACTS = 0;
	private static final String TAG = "LoginActivity";
	@Bind(R.id.account_active)
	Switch mActiveView;
	@Bind(R.id.email_sign_in_button)
	Button mEmailSignInButton;
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;
	// UI references.
	private EditText mXMPPIDView;
	private EditText mPasswordView;
	private EditText mResourceView;
	private EditText mHostnameView;
	private EditText mNicknameView;
	private View mProgressView;
	private View mLoginFormView;
	private AccountManager mAccountManager;
	private boolean mUpdateAccountMode = false;

	private static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		StringBuilder result = new StringBuilder();
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			result.append(hexArray[v >>> 4]);
			result.append(hexArray[v & 0x0F]);
			if (j < bytes.length)
				result.append(":");
		}
		return result.toString();
	}

	private void acceptCertificate(X509Certificate x509Certificate) {
		SecureTrustManagerFactory.addCertificate(LoginActivity.this, x509Certificate);
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mXMPPIDView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		String xmppID = mXMPPIDView.getText().toString();
		String password = mPasswordView.getText().toString();
		String hostname = mHostnameView.getText().toString();
		String nickname = mNicknameView.getText().toString();
		String resource = mResourceView.getText().toString();
		boolean active = this.mActiveView.isChecked();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password, if the user entered one.
		if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(xmppID)) {
			mXMPPIDView.setError(getString(R.string.error_field_required));
			focusView = mXMPPIDView;
			cancel = true;
		} else if (!isEmailValid(xmppID)) {
			mXMPPIDView.setError(getString(R.string.error_invalid_email));
			focusView = mXMPPIDView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {

			boolean skipLoginTest = false;
			Account account = getAccount(xmppID);
			if (account != null) {
				skipLoginTest = password.equals(mAccountManager.getPassword(account));
				skipLoginTest &= hostname.equals(mAccountManager.getUserData(account, AccountsConstants.FIELD_HOSTNAME));
			}

			skipLoginTest = false;

			mAuthTask = new UserLoginTask(skipLoginTest, xmppID, password, hostname, resource, nickname, active);
			mAuthTask.execute((Void) null);
		}
	}

	private Account getAccount(String name) {
		for (Account account : mAccountManager.getAccounts()) {
			if (account.name.equals(name))
				return account;
		}
		return null;
	}

	private boolean isEmailValid(String email) {
		// TODO: Replace this with your own logic
		return email.contains("@");
	}

	private boolean isPasswordValid(String password) {
		// TODO: Replace this with your own logic
		return password.length() > 4;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		ButterKnife.bind(this);

		mAccountManager = AccountManager.get(this);

		// Set up the login form.
		mXMPPIDView = (EditText) findViewById(R.id.xmppid);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mResourceView = (EditText) findViewById(R.id.resource);
		mHostnameView = (EditText) findViewById(R.id.host);

		mNicknameView = (EditText) findViewById(R.id.nickname);
		mNicknameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});
		mEmailSignInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);

		if (getIntent().getStringExtra("account_name") != null) {
			// edit existing account mode
			Account account = getAccount(getIntent().getStringExtra("account_name"));
			mXMPPIDView.setEnabled(false);
			mUpdateAccountMode = true;
			mXMPPIDView.setText(account.name);
			String tmp = mAccountManager.getUserData(account, AccountsConstants.FIELD_ACTIVE);
			Log.i("LoginActivity", AccountsConstants.FIELD_ACTIVE + " = " + tmp);
			mActiveView.setChecked(Boolean.parseBoolean(tmp == null ? "true" : tmp));
			mPasswordView.setText(mAccountManager.getPassword(account));
			mHostnameView.setText(mAccountManager.getUserData(account, AccountsConstants.FIELD_HOSTNAME));
			mNicknameView.setText(mAccountManager.getUserData(account, AccountsConstants.FIELD_NICKNAME));
			mResourceView.setText(mAccountManager.getUserData(account, AccountsConstants.FIELD_RESOURCE));

			mEmailSignInButton.setText(R.string.action_update_account);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(this,
				// Retrieve data rows for the device user's 'profile' contact.
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
				ProfileQuery.PROJECTION,

				// Select only email addresses.
				ContactsContract.Contacts.Data.MIMETYPE + " = ?",
				new String[] { ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE },

				// Show primary email addresses first. Note that there won't be
				// a primary email address if the user hasn't specified one.
				ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}

	/**
	 * Callback received when a permissions request has been completed.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
	}

	private void showInvalidCertificateDialog(final X509Certificate[] chain) {
		StringBuilder msg = new StringBuilder(100);

		MessageDigest sha1 = null;
		MessageDigest md5 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			Log.wtf(TAG, "SHA1 should be here!", e);
		}
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.wtf(TAG, "MD5 should be here!", e);
		}

		for (int i = 0; i < chain.length; i++) {
			msg.append(getString(R.string.account_certificate_info_chain, i));
			msg.append(getString(R.string.account_certificate_info_subject, chain[i].getSubjectDN().toString()));
			msg.append(getString(R.string.account_certificate_info_issuer, chain[i].getIssuerDN().toString()));
			if (sha1 != null) {
				sha1.reset();
				try {
					msg.append(getString(R.string.account_certificate_info_fingerprint_sha,
							bytesToHex(sha1.digest(chain[i].getEncoded()))));

				} catch (CertificateEncodingException e) {
					Log.e(TAG, "Cannot add SHA1 to info", e);
				}
			}
			if (md5 != null) {
				md5.reset();
				try {
					msg.append(getString(R.string.account_certificate_info_fingerprint_md5,
							bytesToHex(md5.digest(chain[i].getEncoded()))));

				} catch (CertificateEncodingException e) {
					Log.e(TAG, "Cannot add MD5 to info", e);
				}
			}
		}

		new AlertDialog.Builder(LoginActivity.this).setTitle(getString(R.string.account_certificate_info_title)).setMessage(
				getString(R.string.account_certificate_info_description) + " " + msg.toString()).setCancelable(
						true).setPositiveButton(getString(R.string.account_certificate_info_button_accept),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										acceptCertificate(chain[0]);
										attemptLogin();
									}
								}).setNegativeButton(getString(R.string.account_certificate_info_button_reject),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
											}
										}).show();
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	private interface ProfileQuery {
		String[] PROJECTION = { ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY, };

		int ADDRESS = 0;
		int IS_PRIMARY = 1;
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

		private final String mXmppId;
		private final String mPassword;
		private final String mNickname;
		private final String mHostname;
		private final String mResource;
		private final boolean mActive;
		private final ConnectionChecker checker;
		private final boolean skipLoginTest;

		UserLoginTask(boolean skipLoginTest, String xmppId, String password, String hostname, String resource, String nickname,
				boolean active) {
			this.skipLoginTest = skipLoginTest;
			mXmppId = xmppId;
			mPassword = password;
			mNickname = nickname;
			mHostname = hostname;
			mResource = resource;
			mActive = active;
			this.checker = new ConnectionChecker(mXmppId, mPassword, mHostname);
			Log.i(TAG, "Connection checker created");

		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (skipLoginTest)
				return Boolean.TRUE;

			// TODO: attempt authentication against a network service.
			try {
				boolean result = checker.check(getApplicationContext());
				// Simulate network access.
				Log.i(TAG, "Connection checking result: " + result);
				return result;
			} catch (Exception e) {
				Log.e(TAG, "What the hell?", e);
				return false;
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				try {
					Account account = getAccount(mXmppId);
					if (account == null) {
						account = new Account(mXmppId, Authenticator.ACCOUNT_TYPE);
						Log.d(TAG, "Adding account " + mXmppId + ":" + mPassword);
						mAccountManager.addAccountExplicitly(account, mPassword, null);
					} else {
						Log.d(TAG, "Updating account " + mXmppId + ":" + mPassword);
						mAccountManager.setPassword(account, mPassword);
					}
					mAccountManager.setUserData(account, AccountsConstants.FIELD_NICKNAME, mNickname);
					mAccountManager.setUserData(account, AccountsConstants.FIELD_HOSTNAME, mHostname);
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
					i.setAction(ACCOUNT_MODIFIED_MSG);
					i.putExtra(AccountManager.KEY_ACCOUNT_NAME, mXmppId);
					sendBroadcast(i);

				} catch (Exception e) {
					Log.e("LoginActivity", "Can't add account", e);
				}
				finish();
			} else {
				Log.i(TAG, "Any problem?", checker.getException());
				if (checker.getException() != null
						&& checker.getException().getCause() instanceof SecureTrustManagerFactory.DataCertificateException) {
					X509Certificate[] chain = ((SecureTrustManagerFactory.DataCertificateException) checker.getException().getCause()).getChain();

					showInvalidCertificateDialog(chain);
				} else if (checker.isPasswordInvalid()) {
					mPasswordView.setError(getString(R.string.error_incorrect_password));
					mPasswordView.requestFocus();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
					builder.setMessage("Connection error.").setPositiveButton(android.R.string.ok, null).show();
				}
			}
		}

		@Override
		protected void onPreExecute() {
			showProgress(true);
			super.onPreExecute();
		}
	}

}
