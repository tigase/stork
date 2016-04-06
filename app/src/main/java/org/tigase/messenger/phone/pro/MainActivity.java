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

import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.openchats.OpenChatItemFragment;
import org.tigase.messenger.phone.pro.roster.RosterItemFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.SettingsActivity;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
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
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
		RosterItemFragment.OnRosterItemIteractionListener, OpenChatItemFragment.OnAddChatListener {

	Spinner statusSelector;
	private Menu navigationMenu;
	private XMPPServiceConnection mServiceConnection = new XMPPServiceConnection();

	private void doPresenceChange(long presenceId) {
		SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
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
		ButterKnife.bind(this);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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

		final SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
		switch (sharedPref.getString("menu", "roster")) {
		case "roster":
			switchMainFragment(R.id.nav_roster);
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

	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.main, menu);
	// return true;
	// }

	@Override
	public void onListFragmentInteraction(int id, String account, String jid) {
		Jaxmpp jaxmpp = mServiceConnection.getService().getJaxmpp(account);
		final BareJID bareJID = BareJID.bareJIDInstance(jid);
		try {
			Chat chat = null;
			for (Chat c : jaxmpp.getModule(MessageModule.class).getChats()) {
				if (c.getJid().getBareJid().equals(bareJID)) {
					chat = c;
					break;
				}
			}
			if (chat == null) {
				chat = jaxmpp.getModule(MessageModule.class).createChat(JID.jidInstance(jid));
			}

			Intent intent = new Intent(MainActivity.this, ChatActivity.class);
			intent.putExtra("openChatId", (int) chat.getId());
			intent.putExtra("jid", jid);
			intent.putExtra("account", account);
			startActivity(intent);

		} catch (JaxmppException e) {
			e.printStackTrace();
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
		MenuItem menuItem = navigationMenu.findItem(id);
		menuItem.setChecked(true);
		setTitle(menuItem.getTitle());

		SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);

		switch (id) {
		case R.id.nav_about: {
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			break;
		}
		case R.id.nav_roster: {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.flContent,
					RosterItemFragment.newInstance(mServiceConnection)).commit();

			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString("menu", "roster");
			editor.commit();

			break;
		}
		case R.id.nav_chats: {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.flContent,
					OpenChatItemFragment.newInstance(mServiceConnection)).commit();

			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString("menu", "chats");
			editor.commit();

			break;
		}
		case R.id.nav_settings: {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			break;
		}
		}
	}

	public static class XMPPServiceConnection implements ServiceConnection {
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
