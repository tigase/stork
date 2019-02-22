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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.contact.EditContactActivity;
import org.tigase.messenger.phone.pro.searchbar.SearchActionMode;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;

import java.util.Collection;

import static org.tigase.messenger.phone.pro.service.XMPPService.ACCOUNT_TMP_DISABLED_KEY;

/**
 * A fragment representing a list of Items.
 */
public class RosterItemFragment
		extends MultiSelectFragment {

	final static String TAG = "RosterItemFragment";
	private static final boolean SHOW_OFFLINE_DEFAULT = true;
	RecyclerView recyclerView;
	private MyRosterItemRecyclerViewAdapter adapter;
	private DBUpdateTask dbUpdateTask;
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection();
	private SearchActionMode searchActionMode;
	//	private OnRosterItemDeleteListener mItemLongClickListener = new OnRosterItemDeleteListener() {
//
//		@Override
//		public void onRosterItemDelete(int id, final String account, final String jid, final String name) {
//			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					switch (which) {
//						case DialogInterface.BUTTON_POSITIVE:
//							(new RemoveContactTask(BareJID.bareJIDInstance(account),
//												   BareJID.bareJIDInstance(jid))).execute();
//							break;
//					}
//				}
//			};
//
//			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//			builder.setMessage(getContext().getString(R.string.roster_delete_contact, jid, name))
//					.setPositiveButton(android.R.string.yes, dialogClickListener)
//					.setNegativeButton(android.R.string.no, dialogClickListener)
//					.show();
//		}
//	};
	private SharedPreferences sharedPref;

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static RosterItemFragment newInstance(MainActivity.XMPPServiceConnection mServiceConnection) {
		RosterItemFragment fragment = new RosterItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
	 * changes).
	 */
	public RosterItemFragment() {
		super();
	}

	@Override
	public void onAttach(Context context) {
		this.sharedPref = getContext().getSharedPreferences("RosterPreferences", Context.MODE_PRIVATE);

		super.onAttach(context);

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.searchActionMode = new SearchActionMode(getActivity(), txt -> refreshRoster());
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.roster_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_rosteritem_list, container, false);

		recyclerView = (RecyclerView) root.findViewById(R.id.roster_list);

		FloatingActionButton rosterAddContact = (FloatingActionButton) root.findViewById(R.id.roster_add_contact);
		rosterAddContact.setOnClickListener(view -> {
			Intent intent = new Intent(getContext(), EditContactActivity.class);
			startActivity(intent);
		});

		// Set the adapter
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

		this.adapter = new MyRosterItemRecyclerViewAdapter(getContext(), null, this) {
			@Override
			protected void onContentChanged() {
				refreshRoster();
			}
		};
		recyclerView.setAdapter(adapter);

		return root;
	}

	@Override
	public void onDetach() {
		sharedPref = null;
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		SharedPreferences.Editor editor;
		switch (item.getItemId()) {
			case R.id.ac_serach:
				ActionMode am = startActionMode(this.searchActionMode);
				return true;
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

	@Override
	public void onResume() {
		super.onResume();
		refreshRoster();
	}

	@Override
	protected boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ac_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.roster_delete_contact).setPositiveButton(R.string.yes, (dialog, which) -> {

					final Cursor c = adapter.getCursor();
					for (int pos : getMultiSelector().getSelectedPositions()) {
						c.moveToPosition(pos);
						String account = c.getString(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
						String jid = c.getString(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));
						(new RemoveContactTask(BareJID.bareJIDInstance(account),
											   BareJID.bareJIDInstance(jid))).execute();
					}

					mode.finish();
				}).setNegativeButton(R.string.no, null).show();

				return true;
			default:
				return false;
		}
	}

	@Override
	protected boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		actionMode.getMenuInflater().inflate(R.menu.roster_context, menu);
		return true;
	}

	@Override
	protected void updateActionMode(ActionMode actionMode) {
		final int count = mMultiSelector.getSelectedPositions().size();
		actionMode.setTitle(getContext().getResources().getQuantityString(R.plurals.roster_selected, count, count));
	}

	private void refreshRoster() {
		if (dbUpdateTask == null || dbUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
			String txt = searchActionMode.getSearchText();
			dbUpdateTask = new DBUpdateTask();
			dbUpdateTask.execute(txt);
		}
	}

	private String enabledAccounts() {
		final StringBuilder sb = new StringBuilder();
		sb.append("'-'");
		try {
			Collection<JaxmppCore> accounts = mConnection.getService().getMultiJaxmpp().get();
			for (JaxmppCore account : accounts) {
				Boolean disabled = (Boolean) account.getSessionObject().getUserProperty(ACCOUNT_TMP_DISABLED_KEY);
				if (disabled == null || disabled) {
					continue;
				}
				sb.append(",");
				sb.append("'").append(account.getSessionObject().getUserBareJid().toString()).append("'");
			}
		} catch (Exception e) {
			Log.wtf(TAG, "Cannot prepare list of active accounts.", e);
		}
		return sb.toString();
	}

	private class DBUpdateTask
			extends AsyncTask<String, Void, Cursor> {

		@Override
		protected Cursor doInBackground(String... params) {
			if (sharedPref == null) {
				Log.e("RosterItemFragment", "Shared preferences are empty?");
				return null;
			}
			String[] columnsToReturn = new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
													DatabaseContract.RosterItemsCache.FIELD_ACCOUNT,
													DatabaseContract.RosterItemsCache.FIELD_JID,
													DatabaseContract.RosterItemsCache.FIELD_NAME,
													DatabaseContract.RosterItemsCache.FIELD_STATUS};

			boolean showOffline = sharedPref.getBoolean("show_offline", SHOW_OFFLINE_DEFAULT);
			final String searchText = params != null ? params[0] : null;

			String selection = "1=1 ";
			String[] args = null;

			if (!showOffline) {
				selection += " AND " + DatabaseContract.RosterItemsCache.FIELD_STATUS + ">=5 ";
			}

			if (searchText != null) {
				selection += " AND (" + DatabaseContract.RosterItemsCache.FIELD_NAME + " like ? OR " +
						DatabaseContract.RosterItemsCache.FIELD_JID + " like ?" + " )";
				args = new String[]{"%" + searchText + "%", "%" + searchText + "%"};
			}

			String enabledAccounts = enabledAccounts();
			if (enabledAccounts != null) {
				selection +=
						" AND (" + DatabaseContract.RosterItemsCache.FIELD_ACCOUNT + " IN (" + enabledAccounts + "))";
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
					sort = DatabaseContract.RosterItemsCache.FIELD_STATUS + " DESC," +
							DatabaseContract.RosterItemsCache.FIELD_NAME + " COLLATE NOCASE ASC";
					break;
				default:
					sort = "";
			}

			Cursor cursor = getContext().getContentResolver()
					.query(RosterProvider.ROSTER_URI, columnsToReturn, selection, args, sort);

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
		}
	}

	private class RemoveContactTask
			extends AsyncTask<Void, Void, Void> {

		private final BareJID account;
		private final BareJID jid;

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
				Jaxmpp jaxmpp = mService.getJaxmpp(account);
				if (jaxmpp.isConnected()) {
					RosterStore store = RosterModule.getRosterStore(jaxmpp.getSessionObject());
					store.remove(jid);
				}
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Can't remove contact from roster", e);
				Toast.makeText(getContext(), "ERROR " + e.getMessage(), Toast.LENGTH_SHORT);
			}
			return null;
		}
	}

}
