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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;

import butterknife.Bind;
import butterknife.ButterKnife;

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

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public RosterItemFragment() {
	}

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static RosterItemFragment newInstance() {
		RosterItemFragment fragment = new RosterItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	private Cursor loadData() {
		final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

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
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnRosterItemIteractionListener) {
			mListener = (OnRosterItemIteractionListener) context;
		} else {
			throw new RuntimeException(context.toString() + " must implement OnRosterItemIteractionListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
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

		this.adapter = new MyRosterItemRecyclerViewAdapter(getContext(), loadData(), mListener) {
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
		super.onDetach();
		mListener = null;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
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
		SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

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
		adapter.changeCursor(loadData());
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnRosterItemIteractionListener {
		// TODO: Update argument type and name
		void onListFragmentInteraction(int id, String account, String jid);
	}
}
