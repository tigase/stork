/*
 * RosterItemFragment.java
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

package org.tigase.messenger.phone.pro.roster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.contact.EditContactActivity;
import org.tigase.messenger.phone.pro.service.XMPPService;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the
 * {@link OnRosterItemIteractionListener} interface.
 */
public class RosterItemFragment extends Fragment {

    private static final boolean SHOW_OFFLINE_DEFAULT = true;
    @Bind(R.id.roster_list)
    RecyclerView recyclerView;
    private OnRosterItemIteractionListener mListener;
    private MyRosterItemRecyclerViewAdapter adapter;
    private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection();
    private OnRosterItemDeleteListener mItemLongClickListener = new OnRosterItemDeleteListener() {

        @Override
        public void onRosterItemDelete(int id, final String account, final String jid, final String name) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            (new RemoveContactTask(BareJID.bareJIDInstance(account), BareJID.bareJIDInstance(jid))).execute();
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(getContext().getString(R.string.roster_delete_contact, jid, name))
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener)
                    .show();
        }
    };
    private SharedPreferences sharedPref;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RosterItemFragment() {
        super();
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RosterItemFragment newInstance(MainActivity.XMPPServiceConnection mServiceConnection) {
        RosterItemFragment fragment = new RosterItemFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @OnClick(R.id.roster_add_contact)
    void onAddContactClick() {
        Intent intent = new Intent(getContext(), EditContactActivity.class);
        startActivity(intent);
    }

    @Override
    public void onAttach(Context context) {
        this.sharedPref = getContext().getSharedPreferences("RosterPreferences", Context.MODE_PRIVATE);

        super.onAttach(context);

        if (context instanceof OnRosterItemIteractionListener) {
            mListener = (OnRosterItemIteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnRosterItemIteractionListener");
        }

        Intent intent = new Intent(context, XMPPService.class);

        getActivity().bindService(intent, mConnection, 0);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.roster_context, menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.roster_fragment, menu);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_rosteritem_list, container, false);
        ButterKnife.bind(this, root);

        // Set the adapter
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

        this.adapter = new MyRosterItemRecyclerViewAdapter(getContext(), null, mListener, mItemLongClickListener) {
            @Override
            protected void onContentChanged() {
                refreshRoster();
            }
        };
        recyclerView.setAdapter(adapter);

        registerForContextMenu(recyclerView);

        refreshRoster();

        return root;
    }

    @Override
    public void onDetach() {
        sharedPref = null;
        recyclerView.setAdapter(null);
        adapter.changeCursor(null);
        getActivity().unbindService(mConnection);
        mListener = null;
        super.onDetach();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        SharedPreferences.Editor editor;
        switch (item.getItemId()) {
            case R.id.menu_roster_sort_presence:
                item.setChecked(true);

                editor = sharedPref.edit();
                editor.putString("roster_sort", "presence");
                editor.commit();
                refreshRoster();
                return true;
            case R.id.menu_roster_sort_name:
                item.setChecked(true);
                editor = sharedPref.edit();
                editor.putString("roster_sort", "name");
                editor.commit();
                refreshRoster();
                return true;
            case R.id.menu_roster_show_offline:
                final boolean v = !item.isChecked();
                item.setChecked(v);
                editor = sharedPref.edit();
                editor.putBoolean("show_offline", v);
                editor.commit();
                refreshRoster();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean v = sharedPref.getBoolean("show_offline", SHOW_OFFLINE_DEFAULT);
        menu.findItem(R.id.menu_roster_show_offline).setChecked(v);

        String sort = sharedPref.getString("roster_sort", "presence");
        switch (sort) {
            case "name":
                menu.findItem(R.id.menu_roster_sort_name).setChecked(v);
                break;
            case "presence":
                menu.findItem(R.id.menu_roster_sort_presence).setChecked(v);
                break;
        }
    }

    private void refreshRoster() {
        (new DBUpdateTask()).execute();
    }


    public interface OnRosterItemIteractionListener {
        void onListFragmentInteraction(int id, String account, String jid);

    }

    public interface OnRosterItemDeleteListener {
        void onRosterItemDelete(int id, String account, String jid, String name);

    }

    private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {
        @Override
        protected Cursor doInBackground(Void... params) {
            if (sharedPref == null) {
                Log.e("RosterItemFragment", "Shared preferences are empty?");
                return null;
            }
            String[] columnsToReturn = new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
                    DatabaseContract.RosterItemsCache.FIELD_ACCOUNT, DatabaseContract.RosterItemsCache.FIELD_JID,
                    DatabaseContract.RosterItemsCache.FIELD_NAME, DatabaseContract.RosterItemsCache.FIELD_STATUS};

            boolean showOffline = sharedPref.getBoolean("show_offline", SHOW_OFFLINE_DEFAULT);
            String selection = "1=1 ";

            if (!showOffline) {
                selection += " AND " + DatabaseContract.RosterItemsCache.FIELD_STATUS + ">=5 ";
            }

            String sort;
            String s = sharedPref.getString("roster_sort", "presence");
            switch (s) {
                case "name":
                    sort = DatabaseContract.RosterItemsCache.FIELD_NAME + " COLLATE NOCASE ASC";
                    break;
                case "jid":
                    sort = DatabaseContract.RosterItemsCache.FIELD_JID + " ASC";
                    break;
                case "presence":
                    sort = DatabaseContract.RosterItemsCache.FIELD_STATUS + " DESC," + DatabaseContract.RosterItemsCache.FIELD_NAME
                            + " COLLATE NOCASE ASC";
                    break;
                default:
                    sort = "";
            }
            Cursor cursor = getContext().getContentResolver().query(RosterProvider.ROSTER_URI, columnsToReturn, selection, null,
                    sort);

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            adapter.changeCursor(cursor);
        }
    }

    private class RemoveContactTask extends AsyncTask<Void, Void, Void> {

        private final BareJID jid;
        private final BareJID account;

        public RemoveContactTask(BareJID account, BareJID jid) {
            this.jid = jid;
            this.account = account;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final XMPPService mService = mConnection.getService();
            if (mService == null) {
                Log.i("RosterItemFragment", "Service is disconnected!!!");
                return null;
            }
            try {
                RosterStore store = RosterModule.getRosterStore(mService.getJaxmpp(account).getSessionObject());
                store.remove(jid);
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Can't remove contact from roster", e);
                Toast.makeText(getContext(), "ERROR " + e.getMessage(), Toast.LENGTH_SHORT);
            }
            return null;
        }
    }


}
