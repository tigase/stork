/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
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

package org.tigase.messenger.phone.pro.conversations.muc;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractViewHolder;
import org.tigase.messenger.phone.pro.conversations.DaysInformCursor;
import org.tigase.messenger.phone.pro.conversations.ViewHolderImg;
import org.tigase.messenger.phone.pro.conversations.ViewHolderMsg;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionAdapter;

public class MucItemRecyclerViewAdapter
		extends SelectionAdapter<AbstractViewHolder> {

	private String ownNickname;

	public MucItemRecyclerViewAdapter() {
		super();
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

		return (state & 0x7fff) << 16 | (type & 0x7fff);
	}

	public String getOwnNickname() {
		return ownNickname;
	}

	public void setOwnNickname(String ownNickname) {
		this.ownNickname = ownNickname;
	}

	@Override
	public AbstractViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		AbstractViewHolder viewHolder;
		final int messageType = viewType & 0x7fff;
		final int messageState = ((viewType >> 16) & 0x7fff);

		if (messageType == DaysInformCursor.ITEM_TYPE_DAY_INFO) {
			viewHolder = new ViewHolderMsg(
					LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chatitem_newday, parent, false));
//			viewHolder.setCanBeSelected(false);
		} else {
			switch (messageState) {
				case DatabaseContract.ChatHistory.STATE_INCOMING:
				case DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD:
					switch (messageType) {
						case DatabaseContract.ChatHistory.ITEM_TYPE_ERROR:
							viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
																   .inflate(R.layout.fragment_chatitem_error, parent,
																			false));
							break;
						default:
							viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
																   .inflate(R.layout.fragment_groupchatitem_received,
																			parent, false));
					}
					break;
				case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
				case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
				case DatabaseContract.ChatHistory.STATE_OUT_SENT:
					switch (messageType) {
						case DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE:
							viewHolder = new ViewHolderImg(LayoutInflater.from(parent.getContext())
																   .inflate(R.layout.fragment_groupchatitem_sent_image,
																			parent, false));
							break;
						default:
							viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
																   .inflate(R.layout.fragment_groupchatitem_sent,
																			parent, false));
					}
					break;
				default:
					throw new RuntimeException("Unknown view type (t=" + messageType + ", s=" + messageState + ")");
			}
		}

		viewHolder.setOwnNickname(ownNickname);
		return viewHolder;
	}

}
