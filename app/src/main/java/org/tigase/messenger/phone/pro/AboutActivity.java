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

package org.tigase.messenger.phone.pro;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

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

		putCredits(R.id.creditsText);

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

	private String convert(InputStream inputStream, Charset charset) {

		StringBuilder stringBuilder = new StringBuilder();
		String line = null;

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (IOException e) {
			stringBuilder.append(e.getMessage());
		}

		return stringBuilder.toString();
	}

	private void putCredits(int textViewId) {
		TextView textView = findViewById(textViewId);
		textView.setLinksClickable(true);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		String html = convert(getResources().openRawResource(R.raw.credits), Charset.forName("UTF-8"));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
		} else {
			textView.setText(Html.fromHtml(html));
		}

	}

}
