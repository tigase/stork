package org.tigase.mobile;

import org.tigase.mobile.authenticator.AuthenticatorActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_screen);

		Button addAccountsButton = (Button) findViewById(R.id.welcomeScreenAddAccountsButtom);
		addAccountsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(WelcomeActivity.this, AuthenticatorActivity.class);
				startActivity(intent);

			}
		});
	}

	@Override
	protected void onStart() {
		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		if (accounts != null && accounts.length != 0) {
			Intent intent = new Intent(this, TigaseMobileMessengerActivity.class);
			startActivity(intent);
			finish();
		}
		super.onStart();
	}

}
