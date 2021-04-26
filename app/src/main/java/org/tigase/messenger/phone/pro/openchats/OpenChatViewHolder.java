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
package org.tigase.messenger.phone.pro.openchats;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.abdularis.civ.StorkAvatarView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.roster.PresenceIconMapper;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionViewHolder;
import tigase.jaxmpp.core.client.BareJID;

class OpenChatViewHolder
		extends SelectionViewHolder {

	private final StorkAvatarView mContactAvatar;
	private final TextView mContactName;
	private final ImageView mDeliveryStatus;
	private final TextView mLastMessage;
	private String account;
	private int id;
	private String jid;
	private int type;

	public OpenChatViewHolder(View itemView) {
		super(itemView);

		this.mContactName = itemView.findViewById(R.id.contact_display_name);
		this.mLastMessage = itemView.findViewById(R.id.last_message);
		this.mContactAvatar = itemView.findViewById(R.id.contact_avatar);
		this.mDeliveryStatus = itemView.findViewById(R.id.chat_delivery_status);
	}

	public String getJid() {
		return jid;
	}

	public String getAccount() {
		return account;
	}

	public int getId() {
		return id;
	}

	public int getType() {
		return type;
	}

	@Override
	protected void bind(int adapterPosition, Cursor cursor, boolean selected) {
		this.id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
		this.jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
		this.account = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
		this.type = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_TYPE));
		final String name = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_NAME));
		final String lastMessage = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE));
		final int presence = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_CONTACT_PRESENCE));
		final int lastMessageState = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE_STATE));
		final int unreadCount = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_UNREAD_COUNT));
		final int lastMessageType = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE_TYPE));

		int presenceIconResource = PresenceIconMapper.getPresenceResource(presence);
		itemView.setSelected(selected);

		if (lastMessageType == DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE ||
				lastMessageType == DatabaseContract.ChatHistory.ITEM_TYPE_FILE ||
				lastMessageType == DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO) {
			mLastMessage.setText("\uD83D\uDCCE Attachment");
		} else {
			mLastMessage.setText(lastMessage);
		}

		if (unreadCount > 0) {
			mLastMessage.setTextColor(0xFF000000);
			mLastMessage.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			mLastMessage.setTextColor(0xFF6B6666);
			mLastMessage.setTypeface(Typeface.DEFAULT);
		}

		switch (lastMessageState) {
			case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
				mDeliveryStatus.setImageResource(R.drawable.ic_message_not_sent_24dp);
				mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			case DatabaseContract.ChatHistory.STATE_OUT_SENT:
				mDeliveryStatus.setImageResource(R.drawable.ic_message_sent_24dp);
				mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
				mDeliveryStatus.setImageResource(R.drawable.ic_message_delivered_24dp);
				mDeliveryStatus.setVisibility(View.VISIBLE);
				break;
			default:
				mDeliveryStatus.setVisibility(View.GONE);
		}

		final Context context = itemView.getContext();

		switch (type) {
			case DatabaseContract.OpenChats.TYPE_MUC:
				mContactName.setText(context.getString(R.string.openchats_room, name));
				mContactAvatar.setJID(BareJID.bareJIDInstance(jid), name);
//				holder.setContextMenu(R.menu.openchat_groupchat_context, menuListener);
				mContactAvatar.setImageResource(R.drawable.ic_menu_groupchat);
				break;
			case DatabaseContract.OpenChats.TYPE_CHAT:
				String n = context.getString(R.string.openchats_chat, name);
				mContactName.setText(n);
				mContactAvatar.setJID(BareJID.bareJIDInstance(jid), n, presenceIconResource);
//					AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), mContactAvatar);
				break;
		}
	}

//		@Override
//		protected void onItemClick(View v) {
//			onEnterToChat(this.type, this.id, this.jid, this.account);
//		}

}
