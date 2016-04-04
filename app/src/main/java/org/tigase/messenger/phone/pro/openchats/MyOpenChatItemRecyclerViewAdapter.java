/*
 * MyOpenChatItemRecyclerViewAdapter.java
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

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

public class MyOpenChatItemRecyclerViewAdapter extends CursorRecyclerViewAdapter<ViewHolder> {

	private final OpenChatItemFragment.OnListFragmentInteractionListener mListener;

	public MyOpenChatItemRecyclerViewAdapter(Cursor cursor, OpenChatItemFragment.OnListFragmentInteractionListener listener) {
		super(cursor);
		mListener = listener;
	}

	@Override
	public void onBindViewHolderCursor(final ViewHolder holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
		final String account = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
		final String name = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_NAME));
		final String lastMessage = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE));
		final int state = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_STATE));

		int presenceIconResource;
		switch (state) {
		case CPresence.OFFLINE:
			presenceIconResource = android.R.drawable.presence_invisible;
			break;
		case CPresence.ERROR:
			presenceIconResource = android.R.drawable.presence_offline;
			break;
		case CPresence.DND:
			presenceIconResource = android.R.drawable.presence_busy;
			break;
		case CPresence.XA:
			presenceIconResource = android.R.drawable.presence_away;
			break;
		case CPresence.AWAY:
			presenceIconResource = android.R.drawable.presence_away;
			break;
		case CPresence.ONLINE:
			presenceIconResource = android.R.drawable.presence_online;
			break;
		case CPresence.CHAT: // chat
			presenceIconResource = android.R.drawable.presence_online;
			break;
		default:
			presenceIconResource = android.R.drawable.presence_offline;
		}

		holder.mStatus.setImageResource(presenceIconResource);
		holder.mContactName.setText(name);
		holder.mLastMessage.setText(lastMessage);
		AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), holder.mContactAvatar);

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if
					// the
					// fragment is attached to one) that an item has been
					// selected.
					mListener.onEnterToChat(id, jid, account);
				}
			}
		});
		holder.setContextMenu(R.menu.openchat_context, new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_chat_archive) {
					mListener.onArchiveChat(id, jid, account);
					return true;
				} else
					return false;
			}
		});

	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_openchatitem, parent, false);
		return new ViewHolder(view);
	}

}
