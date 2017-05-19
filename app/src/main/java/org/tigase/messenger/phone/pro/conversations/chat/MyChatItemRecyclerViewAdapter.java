/*
 * MyChatItemRecyclerViewAdapter.java
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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class MyChatItemRecyclerViewAdapter
		extends CursorRecyclerViewAdapter<ViewHolder> {

	public static final int ITEM_ERROR = 1;
	public static final int ITEM_MESSAGE_IN = 20;
	public static final int ITEM_MESSAGE_OUT = 30;
	private final Context context;
	private final ChatItemFragment.ChatItemIterationListener mListener;

	public MyChatItemRecyclerViewAdapter(Context context, Cursor cursor,
										 ChatItemFragment.ChatItemIterationListener listener) {
		super(cursor);
		mListener = listener;
		this.context = context;
	}

	@Override
	public int getItemViewType(int i) {
		if (!isDataValid()) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		if (!getCursor().moveToPosition(i)) {
			throw new IllegalStateException("couldn't move cursor to position " + i);
		}

		final int state = getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
		final int type = getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE));

		switch (state) {
			case DatabaseContract.ChatHistory.STATE_INCOMING:
			case DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD:
				if (type == DatabaseContract.ChatHistory.ITEM_TYPE_ERROR) {
					return ITEM_ERROR;
				} else {
					return ITEM_MESSAGE_IN;
				}
			case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
			case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
			case DatabaseContract.ChatHistory.STATE_OUT_SENT:
				return ITEM_MESSAGE_OUT;
			default:
				return -1;
		}

	}

	@Override
	public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID));
		final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
		final long timestampt = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
		final int state = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));

		holder.mContentView.setText(body);
		holder.mTimestamp.setText(DateUtils.getRelativeDateTimeString(context, timestampt, DateUtils.MINUTE_IN_MILLIS,
																	  DateUtils.WEEK_IN_MILLIS, 0));

		if (holder.mDeliveryStatus != null) {
			switch (state) {
				case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_not_sent_24dp);
					break;
				case DatabaseContract.ChatHistory.STATE_OUT_SENT:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_sent_24dp);
					break;
				case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_delivered_24dp);
					break;
			}
		}
		if (holder.mAvatar != null) {
			AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), holder.mAvatar);
		}
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if
					// the
					// fragment is attached to one) that an item has been
					// selected.
					// mListener.onListFragmentInteraction(holder.mItem);
				}
			}
		});
		holder.setContextMenu(R.menu.chatitem_context, new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_chat_copytext) {
					mListener.onCopyChatMessage(id, jid, body);
					return true;
				} else {
					return false;
				}
			}
		});
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view;
		switch (viewType) {
			case ITEM_MESSAGE_IN:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.fragment_chatitem_received, parent, false);
				break;
			case ITEM_MESSAGE_OUT:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chatitem_sent, parent, false);
				break;
			case ITEM_ERROR:
				view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.fragment_chatitem_error, parent, false);
				break;
			default:
				throw new RuntimeException("Unknown view type " + viewType);
		}

		return new ViewHolder(view);
	}

}
