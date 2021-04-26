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
package org.tigase.messenger.phone.pro.roster.view;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.jetbrains.annotations.NotNull;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.roster.contact.EditContactActivity;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionFragment;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.tigase.messenger.phone.pro.service.XMPPService.ACCOUNT_TMP_DISABLED_KEY;

public class RosterFragment
		extends SelectionFragment<RosterAdapter> {

	static final boolean SHOW_OFFLINE_DEFAULT = true;

	private final MainActivity.XMPPServiceConnection mServiceConnection = new MainActivity.XMPPServiceConnection();
	private RefreshRosterTask dbUpdateTask;
	private SearchActionMode searchActionMode;
	private SharedPreferences sharedPref;

	public RosterFragment() {
		super(R.layout.fragment_rosteritem_list);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NotNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.roster_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		SharedPreferences.Editor editor;
		int itemId = item.getItemId();
		if (itemId == R.id.ac_serach) {
			requireActivity().startActionMode(this.searchActionMode);
			return true;
		} else if (itemId == R.id.menu_roster_sort_presence) {
			item.setChecked(true);

			editor = sharedPref.edit();
			editor.putString("roster_sort", "presence");
			editor.commit();
			refreshRoster();
			return true;
		} else if (itemId == R.id.menu_roster_sort_name) {
			item.setChecked(true);
			editor = sharedPref.edit();
			editor.putString("roster_sort", "name");
			editor.commit();
			refreshRoster();
			return true;
		} else if (itemId == R.id.menu_roster_show_offline) {
			final boolean v = !item.isChecked();
			item.setChecked(v);
			editor = sharedPref.edit();
			editor.putBoolean("show_offline", v);
			editor.commit();
			refreshRoster();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireActivity().bindService(new Intent(requireContext(), XMPPService.class), mServiceConnection, 0);

		this.searchActionMode = new SearchActionMode(getActivity(), txt -> refreshRoster());
		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy() {
		requireActivity().unbindService(mServiceConnection);

		super.onDestroy();
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
	public void onViewCreated(final @NonNull @NotNull View view,
							  final @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		FloatingActionButton rosterAddContact = view.findViewById(R.id.roster_add_contact);
		rosterAddContact.setOnClickListener(v -> {
			Intent intent = new Intent(getContext(), EditContactActivity.class);
			startActivity(intent);
		});

		this.sharedPref = getContext().getSharedPreferences("RosterPreferences", Context.MODE_PRIVATE);
		super.onViewCreated(view, savedInstanceState);
		refreshRoster();
	}

	@Override
	protected void doOnSelectionChange() {
		super.doOnSelectionChange();
		final ActionMode actionMode = getActionMode();
		if (actionMode != null) {
			int count = getSelection().size();
			actionMode.setTitle(getContext().getResources().getQuantityString(R.plurals.roster_selected, count, count));
		}
	}

	@Override
	protected ActionMode.Callback getActionModeCallback() {
		return new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.roster_context, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				return RosterFragment.this.onActionItemClicked(mode, item);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}
		};
	}

	@Override
	protected @NotNull RecyclerView findRecyclerView(@NotNull View view) {
		RecyclerView recyclerView = view.findViewById(R.id.roster_list);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		return recyclerView;
	}

	@Override
	protected @NotNull RosterAdapter createAdapterInstance() {
		RosterAdapter adapter = new RosterAdapter() {
			@Override
			protected void onContentChanged() {
				refreshRoster();
			}
		};
		adapter.setOnItemClickListener(RosterFragment.this::onItemClick);
		return adapter;
	}

	private boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (item.getItemId() == R.id.ac_delete) {
			AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
			builder.setMessage(R.string.roster_delete_contact).setPositiveButton(R.string.yes, (dialog, which) -> {

				final Map<String, ArrayList<String>> toRemove = new HashMap<>();

				for (long key : getSelection()) {
					int pos = getStableIdKeyProvider().getPosition(key);
					Cursor c = getAdapter().getItem(pos);
					String account = c.getString(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
					String jid = c.getString(c.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));

					ArrayList<String> jids = toRemove.get(account);
					if (jids == null) {
						jids = new ArrayList<>();
						toRemove.put(account, jids);
					}
					jids.add(jid);
				}

				for (Map.Entry<String, ArrayList<String>> entry : toRemove.entrySet()) {
					Intent ssIntent = new Intent(getContext(), XMPPService.class);
					ssIntent.setAction(XMPPService.DELETE_ROSTER_ITEMS_COMMAND);
					ssIntent.putExtra("account", entry.getKey());
					ssIntent.putStringArrayListExtra("jids", entry.getValue());
					requireActivity().startService(ssIntent);
				}

				mode.finish();
			}).setNegativeButton(R.string.no, null).show();

			return true;
		} else {
			return false;
		}
	}

	private void onItemClick(View v, RosterAdapter.RosterViewHolder item) {
		Intent intent = new Intent(getContext(), ChatActivity.class);
		intent.putExtra(ChatActivity.JID_KEY, item.getJid());
		intent.putExtra(ChatActivity.ACCOUNT_KEY, item.getAccount());
		requireActivity().startActivity(intent);
	}

	private void refreshRoster() {
		if (dbUpdateTask == null || dbUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
			String txt = searchActionMode.getSearchText();
			dbUpdateTask = new RefreshRosterTask(getAdapter(), sharedPref, enabledAccounts(),
												 getActivity().getContentResolver());
			dbUpdateTask.execute(txt);
		}
	}

	private String enabledAccounts() {
		final StringBuilder sb = new StringBuilder();
		sb.append("'-'");
		final AccountManager am = AccountManager.get(getContext());
		for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
			if (mServiceConnection.getService() != null) {
				Jaxmpp j = mServiceConnection.getService().getJaxmpp(account.name);
				if (j == null) {
					continue;
				}
				Boolean disabled = j.getSessionObject().getUserProperty(ACCOUNT_TMP_DISABLED_KEY);
				if (disabled == null || disabled) {
					continue;
				}
			}
			sb.append(",");
			sb.append("'").append(account.name).append("'");
		}
		return sb.toString();
	}

}
