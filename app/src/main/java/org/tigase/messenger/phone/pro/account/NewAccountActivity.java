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
		} else if (requestCode == CREATE_ACCOUNT_REQUEST) {
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
