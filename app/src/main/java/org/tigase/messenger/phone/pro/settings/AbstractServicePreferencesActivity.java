/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;

public abstract class AbstractServicePreferencesActivity
		extends AppCompatActivity
		implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
				   FragmentManager.OnBackStackChangedListener {

	private final static String SCREEN_TITLE = "SCREEN_TITLE";
	protected final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			AbstractServicePreferencesActivity.this.onServiceConnected();
			pushJaxmppToCurrentFragment();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			AbstractServicePreferencesActivity.this.onServiceDisconnected();
			super.onServiceDisconnected(name);
		}

		@Override
		public void destroy() {
			AbstractServicePreferencesActivity.this.onServiceConnectionDestroy();
		}
	};

	public Jaxmpp getJaxmpp(String name) {
		return mConnection.getService().getJaxmpp(name);
	}

	@Override
	public void onBackPressed() {
		if (backFromSettingsFragment()) {
			super.onBackPressed();
		}
	}

	@Override
	public void onBackStackChanged() {
		androidx.fragment.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);
		pushJaxmpp(fragment);
		CharSequence xx = fragment.getArguments().getCharSequence(SCREEN_TITLE);
		if (xx != null) {
			getSupportActionBar().setTitle(xx);
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (backFromSettingsFragment()) {
			this.finish();
			return super.onSupportNavigateUp();
		} else {
			return false;
		}
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, androidx.preference.Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		args.putCharSequence(SCREEN_TITLE, pref.getTitle());
		final androidx.fragment.app.Fragment fragment = getSupportFragmentManager().getFragmentFactory()
				.instantiate(getClassLoader(), pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);
		// Replace the existing Fragment with the new Fragment
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.settings_container, fragment)
				.addToBackStack("settings")
				.commit();
		getSupportActionBar().setTitle(pref.getTitle());
//		pushJaxmpp(fragment);
		return true;
	}

	protected void onServiceConnectionDestroy() {
	}

	protected void onServiceConnected() {
	}

	protected void onServiceDisconnected() {
	}

	abstract protected PreferenceFragmentCompat getDefaultFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(getApplicationContext(), XMPPService.class), mConnection, 0);
		setContentView(R.layout.activity_settings);

		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportFragmentManager().addOnBackStackChangedListener(this);

		internalOnCreate(savedInstanceState);

		PreferenceFragmentCompat fragment = getDefaultFragment();
		final Bundle args = new Bundle();
		args.putCharSequence(SCREEN_TITLE, getSupportActionBar().getTitle());
		fragment.setArguments(args);

		getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, fragment).commit();
		pushJaxmpp(fragment);
	}

	protected void internalOnCreate(Bundle savedInstanceState) {
	}

	@Override
	protected void onDestroy() {
		mConnection.destroy();
		unbindService(mConnection);
		getSupportFragmentManager().removeOnBackStackChangedListener(this);
		super.onDestroy();
	}

	private void pushJaxmppToCurrentFragment() {
		final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.settings_container);
		pushJaxmpp(fragment);
	}

	private void pushJaxmpp(Fragment fragment) {
		if (fragment instanceof XmppServiceAware && mConnection != null && mConnection.getService() != null) {
			((XmppServiceAware) fragment).setXmppService(mConnection.getService());
		}
	}

	private boolean backFromSettingsFragment() {
		int xx = getSupportFragmentManager().getBackStackEntryCount();
		if (xx > 0) {
			getSupportFragmentManager().popBackStack();
			return false;
		} else {
			return true;
		}
	}

	public interface XmppServiceAware {

		void setXmppService(XMPPService service);

	}

}


