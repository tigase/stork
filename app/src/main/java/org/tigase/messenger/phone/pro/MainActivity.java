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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.account.NewAccountActivity;
import org.tigase.messenger.phone.pro.conenctionStatus.ConnectionStatusesFragment;
import org.tigase.messenger.phone.pro.conversations.muc.JoinMucActivity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.openchats.OpenChatItemFragment;
import org.tigase.messenger.phone.pro.roster.RosterItemFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.SettingsActivity;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;

import java.util.Collection;

import static org.tigase.messenger.phone.pro.service.XMPPService.ACCOUNT_TMP_DISABLED_KEY;
import static org.tigase.messenger.phone.pro.service.XMPPService.CONNECT_ALL;

public class MainActivity
		extends AbstractServiceActivity
		implements NavigationView.OnNavigationItemSelectedListener, OpenChatItemFragment.OnAddChatListener {

	static final int STORAGE_ACCESS_REQUEST = 112;
	public static String[] STORAGE_PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
												  Manifest.permission.READ_EXTERNAL_STORAGE};
	private final StatusHandler statusHandler = new StatusHandler();
	public Toolbar toolbar;
	Spinner statusSelector;
	private TextView connectionStatus;
	private Menu navigationMenu;

	private static Connector.State getState(JaxmppCore j) {
		Connector.State st = j.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		if (st == null) {
			return Connector.State.disconnected;
		} else if (st == Connector.State.connected && j.isConnected()) {
			return Connector.State.connected;
		} else if (st == Connector.State.connected && !j.isConnected()) {
			return Connector.State.connecting;
		} else {
			return st;
		}
	}

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
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.main, menu);
	// return true;
	// }

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

		this.connectionStatus = (TextView) headerLayout.findViewById(R.id.connection_status);

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

		Intent ssIntent = new Intent(this, XMPPService.class);
		ssIntent.setAction(CONNECT_ALL);
		startService(ssIntent);
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
	protected void onResume() {
		super.onResume();

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean itemVisible = sharedPref.getBoolean("nav_connectionstatus", false);

		MenuItem conStat = this.navigationMenu.findItem(R.id.nav_connectionstatus);
		conStat.setVisible(itemVisible);

		updateConnectionStatus();
	}

	@Override
	protected void onXMPPServiceConnected() {
		getServiceConnection().getService()
				.getMultiJaxmpp()
				.addHandler(Connector.StateChangedHandler.StateChangedEvent.class, this.statusHandler);
		getServiceConnection().getService()
				.getMultiJaxmpp()
				.addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, this.statusHandler);
		getServiceConnection().getService()
				.getMultiJaxmpp()
				.addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, this.statusHandler);
		updateConnectionStatus();
	}

	@Override
	protected void onXMPPServiceDisconnected() {
		getServiceConnection().getService().getMultiJaxmpp().remove(this.statusHandler);
	}

	private void setStatusText(final String text, final int backgroundColor, final int textColor) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				connectionStatus.setText(text);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					connectionStatus.setBackgroundColor(getResources().getColor(backgroundColor, null));
					connectionStatus.setTextColor(getResources().getColor(textColor, null));
				} else {
					connectionStatus.setBackgroundColor(getResources().getColor(backgroundColor));
					connectionStatus.setTextColor(getResources().getColor(textColor));
				}
			}
		});
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
						.replace(R.id.flContent, RosterItemFragment.newInstance(getServiceConnection()))
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
						.replace(R.id.flContent, OpenChatItemFragment.newInstance(getServiceConnection()))
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

	private void updateConnectionStatus() {
		if (connectionStatus == null) {
			return;
		}
		if (getServiceConnection().getService() == null) {
			setStatusText("Disconnected", R.color.status_background_disconnected,
						  R.color.status_foreground_disconnected);
			return;
		}

		Collection<JaxmppCore> accounts = getServiceConnection().getService().getMultiJaxmpp().get();
		if (accounts.size() == 0) {
			connectionStatus.setVisibility(View.VISIBLE);
			setStatusText("Disconnected", R.color.status_background_disconnected,
						  R.color.status_foreground_disconnected);
		} else if (accounts.size() == 1) {
			Connector.State st = getState(accounts.iterator().next());
			connectionStatus.setVisibility(View.VISIBLE);
			switch (st) {
				case connected:
					setStatusText("Connected", R.color.status_background_connected,
								  R.color.status_foreground_connected);
					break;
				case disconnected:
					setStatusText("Disconnected", R.color.status_background_disconnected,
								  R.color.status_foreground_disconnected);
					break;
				case connecting:
					setStatusText("Connecting…", R.color.status_background_connecting,
								  R.color.status_foreground_connecting);
					break;
				case disconnecting:
					setStatusText("Disconnecting…", R.color.status_background_disconnecting,
								  R.color.status_foreground_disconnecting);
					break;
			}
		} else {
			int connected = 0;
			int disconnected = 0;
			int connecting = 0;
			int disconnecting = 0;
			int accountsAmount = 0;

			for (JaxmppCore account : accounts) {
				boolean disabled = account.getSessionObject().getProperty(ACCOUNT_TMP_DISABLED_KEY);

				if (disabled) {
					continue;
				}

				++accountsAmount;
				switch (getState(account)) {
					case connected:
						++connected;
						break;
					case disconnected:
						++disconnected;
						break;
					case connecting:
						++connecting;
						break;
					case disconnecting:
						++disconnecting;
						break;
				}
			}
			connectionStatus.setVisibility(View.VISIBLE);

			if (connected == accountsAmount) {
				setStatusText("Connected", R.color.status_background_connected, R.color.status_foreground_connected);
			} else if (connecting > 0) {
				setStatusText("Connecting…", R.color.status_background_connecting,
							  R.color.status_foreground_connecting);
			} else if (disconnecting > 0) {
				setStatusText("Disconnecting…", R.color.status_background_disconnecting,
							  R.color.status_foreground_disconnecting);
			} else {
				setStatusText("Disconnected " + connected + " " + disconnected + " " + connecting + " " + disconnecting,
							  R.color.status_background_disconnected, R.color.status_foreground_disconnected);
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

	private class StatusHandler
			implements Connector.StateChangedHandler, JaxmppCore.LoggedInHandler, JaxmppCore.LoggedOutHandler {

		@Override
		public void onLoggedIn(SessionObject sessionObject) {
			updateConnectionStatus();
		}

		@Override
		public void onLoggedOut(SessionObject sessionObject) {
			updateConnectionStatus();
		}

		@Override
		public void onStateChanged(SessionObject sessionObject, Connector.State oldState, Connector.State newState) {
			updateConnectionStatus();
		}
	}
}
