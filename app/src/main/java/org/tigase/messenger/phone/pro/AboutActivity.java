/*
 * Tigase Android Messenger
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

package org.tigase.messenger.phone.pro;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity
		extends AppCompatActivity {

	TextView tv;

	private int clickCounter = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		tv = (TextView) findViewById(R.id.textView3);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		ImageView logo = (ImageView) findViewById(R.id.app_logo);
		logo.setClickable(true);
		logo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				++clickCounter;
				if (clickCounter % 5 == 0) {
					SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
							getApplicationContext());
					boolean itemVisible = sharedPref.getBoolean("nav_connectionstatus", false);

					itemVisible = !itemVisible;

					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putBoolean("nav_connectionstatus", itemVisible);
					editor.commit();

					Toast.makeText(getApplicationContext(),
								   itemVisible ? "Connection info enabled" : "Connection info disabled",
								   Toast.LENGTH_SHORT).show();
				}
			}
		});

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = pinfo.versionName;
			tv.setText(getString(R.string.about_version_name, versionName));
		} catch (Exception e) {
		}
	}

}
