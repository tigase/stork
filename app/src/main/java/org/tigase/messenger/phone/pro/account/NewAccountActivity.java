package org.tigase.messenger.phone.pro.account;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;

public class NewAccountActivity
		extends AppCompatActivity {

	public static final int LOGIN_REQUEST = 1;

	public static final int CREATE_ACCOUNT_REQUEST = 2;

	TextView tv;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("NewAccountActivity", "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");
		// Check which request we're responding to
		if (requestCode == LOGIN_REQUEST) {
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_account);

		tv = (TextView) findViewById(R.id.textView3);

		Button createNewAccountButton = (Button) findViewById(R.id.createNewAccountButton);
		createNewAccountButton.setOnClickListener(view -> {
			Intent intent = new Intent(NewAccountActivity.this, CreateAccountActivity.class);
			startActivityForResult(intent, CREATE_ACCOUNT_REQUEST);
		});

		Button useExistingAccountButton = (Button) findViewById(R.id.useExistingAccountButton);
		useExistingAccountButton.setOnClickListener(view -> {
			Intent intent = new Intent(NewAccountActivity.this, LoginActivity.class);
			startActivityForResult(intent, LOGIN_REQUEST);
		});
		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = pinfo.versionName;
			tv.setText(getString(R.string.about_version_name, versionName));
		} catch (Exception e) {
		}
	}

}
