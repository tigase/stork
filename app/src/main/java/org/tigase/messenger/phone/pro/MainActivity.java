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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.openchats.OpenChatItemFragment;
import org.tigase.messenger.phone.pro.roster.RosterItemFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        RosterItemFragment.OnRosterItemIteractionListener, OpenChatItemFragment.OnListFragmentInteractionListener {

    private Menu navigationMenu;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    Spinner statusSelector;

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

        switchMainFragment(R.id.nav_roster);


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
        SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
        statusSelector.setSelection(statusAdapter.getPosition(sharedPref.getLong("presence", CPresence.OFFLINE)));

        checkService();
    }

    private void checkService() {
        SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
        long presenceId = sharedPref.getLong("presence", CPresence.OFFLINE);

        if (presenceId != CPresence.OFFLINE && mServiceConnection.getService() == null) {
            Log.i("MainActivity", "Service is turnedOff. Starting!");

            Intent startServiceIntent = new Intent(this, XMPPService.class);
            startServiceIntent.setAction("connect-all");
            startService(startServiceIntent);
        }
    }

    private void doPresenceChange(long presenceId) {
        SharedPreferences sharedPref = getSharedPreferences("MainPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("presence", presenceId);
        editor.commit();

        checkService();

        Intent action = new Intent(XMPPService.CLIENT_PRESENCE_CHANGED_ACTION);
        action.putExtra("presence", presenceId);
        sendBroadcast(action);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onListFragmentInteraction(int id, String account, String jid) {
        Toast.makeText(this, "RosterDummyContent click " + jid, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onListFragmentInteraction() {
        Toast.makeText(this, "OpenChatsDummyContent click", Toast.LENGTH_SHORT).show();
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


    public static class XMPPServiceConnection implements ServiceConnection {
        private XMPPService mService;

        public XMPPServiceConnection() {
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

        public XMPPService getService() {
            return mService;
        }
    }


    private XMPPServiceConnection mServiceConnection = new XMPPServiceConnection();


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void switchMainFragment(final int id) {
        MenuItem menuItem = navigationMenu.findItem(id);
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        switch (id) {
            case R.id.nav_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_roster: {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, RosterItemFragment.newInstance(mServiceConnection)).commit();
                break;
            }
            case R.id.nav_chats: {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, OpenChatItemFragment.newInstance(mServiceConnection)).commit();
                break;
            }
            case R.id.nav_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
        }
    }
}
