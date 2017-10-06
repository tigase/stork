/*
 * MainActivity.java
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

package org.tigase.messenger.phone.pro;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.account.NewAccountActivity;
import org.tigase.messenger.phone.pro.conenctionStatus.ConnectionStatusesFragment;
import org.tigase.messenger.phone.pro.conversations.muc.JoinMucActivity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.openchats.OpenChatItemFragment;
import org.tigase.messenger.phone.pro.roster.RosterItemFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.SettingsActivity;

public class MainActivity
		extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener, OpenChatItemFragment.OnAddChatListener {

	static final int STORAGE_ACCESS_REQUEST = 112;
	public static String[] STORAGE_PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
												  Manifest.permission.READ_EXTERNAL_STORAGE};
	public Toolbar toolbar;
	Spinner statusSelector;
	private XMPPServiceConnection mServiceConnection = new XMPPServiceConnection();
	private Menu navigationMenu;

	public static boolean hasPermissions(Context context, String... permissions) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	private void doPresenceChange(long presenceId) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong("presence", presenceId);
		editor.commit();

		Intent action = new Intent(XMPPService.CLIENT_PRESENCE_CHANGED_ACTION);
		action.putExtra("presence", presenceId);
		sendBroadcast(action);
	}

	@Override
	public void onAddChatClick() {
		switchMainFragment(R.id.nav_roster);
	}
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.main, menu);
	// return true;
	// }

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		/*
		 * FloatingActionButton fab = (FloatingActionButton)
		 * findViewById(R.id.fab); fab.setOnClickListener(new
		 * View.OnClickListener() {
		 *
		 * @Override public void onClick(View view) { Snackbar.make(view,
		 * "Replace with your own action", Snackbar.LENGTH_LONG)
		 * .setAction("Action", null).show(); } });
		 */

		final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
																 R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
		this.navigationMenu = navigationView.getMenu();

		View headerLayout = navigationView.getHeaderView(0);
		this.statusSelector = (Spinner) headerLayout.findViewById(R.id.status_selector);

		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		switch (sharedPref.getString("menu", "roster")) {
			case "roster":
				switchMainFragment(R.id.nav_roster);
				break;
			case "connectionstatus":
				switchMainFragment(R.id.nav_connectionstatus);
				break;
			default:
				switchMainFragment(R.id.nav_chats);
				break;
		}

		StatusSelectorAdapter statusAdapter = StatusSelectorAdapter.instance(this);
		statusSelector.setAdapter(statusAdapter);
		statusSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				doPresenceChange(id);
				drawer.closeDrawers();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		statusSelector.setSelection(statusAdapter.getPosition(sharedPref.getLong("presence", CPresence.OFFLINE)));

		final AccountManager am = AccountManager.get(this);
		Account[] accounts = am.getAccountsByType(Authenticator.ACCOUNT_TYPE);
		if (accounts == null || accounts.length == 0) {
			Intent intent = new Intent(this, NewAccountActivity.class);
			startActivity(intent);
		}

		if (Build.VERSION.SDK_INT >= 23) {
			if (!hasPermissions(this, STORAGE_PERMISSIONS)) {
				ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, STORAGE_ACCESS_REQUEST);
			} else {
				//do here
			}
		} else {
			//do here
		}
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		// Handle navigation view item clicks here.
		int id = menuItem.getItemId();

		switchMainFragment(id);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case STORAGE_ACCESS_REQUEST: {

			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean itemVisible = sharedPref.getBoolean("nav_connectionstatus", false);

		MenuItem conStat = this.navigationMenu.findItem(R.id.nav_connectionstatus);
		conStat.setVisible(itemVisible);
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// // Handle action bar item clicks here. The action bar will
	// // automatically handle clicks on the Home/Up button, so long
	// // as you specify a parent activity in AndroidManifest.xml.
	// int id = item.getItemId();
	//
	// // noinspection SimplifiableIfStatement
	// if (id == R.id.action_settings) {
	// return true;
	// }
	//
	// return super.onOptionsItemSelected(item);
	// }

	@Override
	protected void onStart() {
		super.onStart();
		Intent service = new Intent(getApplicationContext(), XMPPService.class);
		bindService(service, mServiceConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mServiceConnection);
	}

	private void switchMainFragment(final int id) {
		final MenuItem menuItem = navigationMenu.findItem(id);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		switch (id) {
			case R.id.nav_about: {
				Intent intent = new Intent(this, AboutActivity.class);
				startActivity(intent);
				break;
			}
			case R.id.nav_connectionstatus: {
				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.flContent, ConnectionStatusesFragment.newInstance())
						.commit();

				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("menu", "connectionstatus");
				editor.commit();
				menuItem.setChecked(true);
				setTitle(menuItem.getTitle());

				break;
			}
			case R.id.nav_roster: {
				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.flContent, RosterItemFragment.newInstance(mServiceConnection))
						.commit();

				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("menu", "roster");
				editor.commit();
				menuItem.setChecked(true);
				setTitle(menuItem.getTitle());

				break;
			}
			case R.id.nav_chats: {
				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.flContent, OpenChatItemFragment.newInstance(mServiceConnection))
						.commit();

				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString("menu", "chats");
				editor.commit();
				menuItem.setChecked(true);
				setTitle(menuItem.getTitle());

				break;
			}
			case R.id.nav_joinmuc: {
				Intent intent = new Intent(this, JoinMucActivity.class);
				startActivity(intent);
				break;
			}
			case R.id.nav_settings: {
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			}
		}
	}

	public static class XMPPServiceConnection
			implements ServiceConnection {

		private XMPPService mService;

		public XMPPServiceConnection() {
		}

		public XMPPService getService() {
			return mService;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			XMPPService.LocalBinder binder = (XMPPService.LocalBinder) service;
			mService = binder.getService();
			Log.i("MainActivity", "Service binded");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i("MainActivity", "Service unbinded");
			mService = null;
		}
	}
}
