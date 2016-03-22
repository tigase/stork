/*
 * OpenChatItemFragment.java
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

package org.tigase.messenger.phone.pro.openchats;

import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the
 * {@link OnListFragmentInteractionListener} interface.
 */
public class OpenChatItemFragment extends Fragment {

	@Bind(R.id.openchats_list)
	RecyclerView recyclerView;
	private OnListFragmentInteractionListener mListener;
	private OnAddChatListener mAddChatListener;
	private MyOpenChatItemRecyclerViewAdapter adapter;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public OpenChatItemFragment() {
	}

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static OpenChatItemFragment newInstance(MainActivity.XMPPServiceConnection mServiceConnection) {
		OpenChatItemFragment fragment = new OpenChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@OnClick(R.id.roster_add_chat)
	void onAddContactClick() {
		mAddChatListener.onAddChatClick();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnAddChatListener)
			mAddChatListener = (OnAddChatListener) context;
		if (context instanceof OnListFragmentInteractionListener) {
			mListener = (OnListFragmentInteractionListener) context;
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
		// TODO
		inflater.inflate(R.menu.openchat_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_openchatitem_list, container, false);
		ButterKnife.bind(this, root);

		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		this.adapter = new MyOpenChatItemRecyclerViewAdapter(null, mListener) {
			@Override
			protected void onContentChanged() {
				refreshChatlist();
			}
		};
		recyclerView.setAdapter(adapter);

		refreshChatlist();
		return root;
	}

	@Override
	public void onDetach() {
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		// getActivity().unbindService(mConnection);
		mListener = null;
		mAddChatListener = null;
		super.onDetach();
	}

	private void refreshChatlist() {
		(new DBUpdateTask()).execute();
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
	public interface OnListFragmentInteractionListener {
		// TODO: Update argument type and name
		void onOpenChatItemInteraction(int oenChatId, String jid, String account);
	}

	public interface OnAddChatListener {
		// TODO: Update argument type and name
		void onAddChatClick();
	}

	private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[] { DatabaseContract.OpenChats.FIELD_ID,
				DatabaseContract.OpenChats.FIELD_ACCOUNT, DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME,
				ChatProvider.FIELD_UNREAD_COUNT, DatabaseContract.OpenChats.FIELD_TYPE, ChatProvider.FIELD_STATE,
				ChatProvider.FIELD_LAST_MESSAGE };

		@Override
		protected Cursor doInBackground(Void... params) {
			if (getContext() == null)
				return null;
			Cursor cursor = getContext().getContentResolver().query(ChatProvider.OPEN_CHATS_URI, cols, null, null,
					ChatProvider.FIELD_NAME);

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
		}
	}
}
