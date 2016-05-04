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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class MyOpenChatItemRecyclerViewAdapter extends CursorRecyclerViewAdapter<ViewHolder> {

	private final OpenChatItemFragment.OnListFragmentInteractionListener mListener;
	private final Context context;

	public MyOpenChatItemRecyclerViewAdapter(Context context, Cursor cursor, OpenChatItemFragment.OnListFragmentInteractionListener listener) {
		super(cursor);
		mListener = listener;
		this.context = context;
	}

	@Override
	public void onBindViewHolderCursor(final ViewHolder holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
		final String account = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
		final String name = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_NAME));
		final String lastMessage = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE));
		final int presence = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_CONTACT_PRESENCE));
		final int lastMessageState = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE_STATE));
		final int type = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_TYPE));

		int presenceIconResource;
		switch (presence) {
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

		holder.mLastMessage.setText(lastMessage);

		if (lastMessageState == DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD) {
			holder.mLastMessage.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.mLastMessage.setTypeface(Typeface.DEFAULT);
		}

		switch (lastMessageState) {
			case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
				holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_not_sent_24dp);
				holder.mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			case DatabaseContract.ChatHistory.STATE_OUT_SENT:
				holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_sent_24dp);
				holder.mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
				holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_delivered_24dp);
				holder.mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			default:
				holder.mDeliveryStatus.setVisibility(View.GONE);
		}

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if
					// the
					// fragment is attached to one) that an item has been
					// selected.
					mListener.onEnterToChat(type, id, jid, account);
				}
			}
		});

		PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_chat_archive) {
					mListener.onArchiveChat(id, jid, account);
					return true;
				} else if (item.getItemId() == R.id.menu_chat_leaveroom) {
					mListener.onLeaveMucRoom(id, jid, account);
					return true;
				} else
					return false;
			}
		};

		switch (type) {
			case DatabaseContract.OpenChats.TYPE_MUC:
				holder.mStatus.setVisibility(View.INVISIBLE);
				holder.mContactName.setText(context.getString(R.string.openchats_room, name));
				holder.setContextMenu(R.menu.openchat_groupchat_context, menuListener);
				holder.mContactAvatar.setImageResource(R.drawable.ic_groupchat_24dp);
				break;
			case DatabaseContract.OpenChats.TYPE_CHAT:
				holder.mStatus.setImageResource(presenceIconResource);
				holder.mContactName.setText(context.getString(R.string.openchats_chat, name));
				holder.setContextMenu(R.menu.openchat_chat_context, menuListener);
				AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), holder.mContactAvatar);
				break;
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_openchatitem, parent, false);
		return new ViewHolder(view);
	}

}
