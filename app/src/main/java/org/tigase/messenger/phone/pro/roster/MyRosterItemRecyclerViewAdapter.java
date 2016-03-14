/*
 * MyRosterItemRecyclerViewAdapter.java
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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;

public class MyRosterItemRecyclerViewAdapter extends CursorRecyclerViewAdapter<ViewHolder> {

	private final RosterItemFragment.OnRosterItemIteractionListener mListener;

	public MyRosterItemRecyclerViewAdapter(Context context, Cursor cursor,
										   RosterItemFragment.OnRosterItemIteractionListener mListener) {
		super(cursor);
		this.mListener = mListener;
	}

	@Override
	public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));
		final String account = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
		String name = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
		int status = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_STATUS));

		holder.mJidView.setText(jid);
		holder.mContactNameView.setText(name);

		switch (status) {
			case CPresence.OFFLINE:
				holder.mContactPresence.setImageResource(R.drawable.presence_offline);
				break;
			case CPresence.ERROR:
				holder.mContactPresence.setImageResource(R.drawable.presence_error);
				break;
			case CPresence.DND:
				holder.mContactPresence.setImageResource(R.drawable.presence_busy);
				break;
			case CPresence.XA:
				holder.mContactPresence.setImageResource(R.drawable.presence_xa);
				break;
			case CPresence.AWAY:
				holder.mContactPresence.setImageResource(R.drawable.presence_away);
				break;
			case CPresence.ONLINE:
				holder.mContactPresence.setImageResource(R.drawable.presence_available);
				break;
			case CPresence.CHAT: // chat
				holder.mContactPresence.setImageResource(R.drawable.presence_chat);
				break;
		}

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if
					// the
					// fragment is attached to one) that an item has been
					// selected.
					mListener.onListFragmentInteraction(id, account, jid);
				}
			}
		});

		AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), holder.mContactAvatar);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_rosteritem, parent, false);
		return new ViewHolder(view);
	}

}
