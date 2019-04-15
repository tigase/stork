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

package org.tigase.messenger.phone.pro.openchats;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.abdularis.civ.AvatarImageView;
import com.github.abdularis.civ.StorkAvatarView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.conversations.muc.MucActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.roster.PresenceIconMapper;
import org.tigase.messenger.phone.pro.selectionview.CursorMultiSelectViewAdapter;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectViewHolder;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class MyOpenChatItemRecyclerViewAdapter
		extends CursorMultiSelectViewAdapter<MyOpenChatItemRecyclerViewAdapter.ViewHolder> {

	private final Context context;

	public MyOpenChatItemRecyclerViewAdapter(Context context, Cursor cursor,
											 OpenChatItemFragment openChatItemFragment) {
		super(cursor, openChatItemFragment);
		this.context = context;
	}

	@Override
	public int getItemViewType(int position) {
		if (!isDataValid()) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		if (!getCursor().moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position " + position);
		}

		final int type = getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.OpenChats.FIELD_TYPE));

		return type;
	}

	@Override
	public void onBindViewHolderCursor(final ViewHolder holder, Cursor cursor) {
		holder.bind(cursor);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_openchatitem, parent, false);
		return new ViewHolder(view);
	}

	public void onEnterToChat(final int type, final int openChatId, final String jid, final String account) {
		Intent intent;
		switch (type) {
			case DatabaseContract.OpenChats.TYPE_CHAT:
				intent = new Intent(context, ChatActivity.class);
				intent.putExtra(ChatActivity.JID_KEY, jid);
				intent.putExtra(ChatActivity.ACCOUNT_KEY, account);
				break;
			case DatabaseContract.OpenChats.TYPE_MUC:
				intent = new Intent(context, MucActivity.class);
				intent.putExtra("jid", jid);
				intent.putExtra("account", account);
				break;
			default:
				throw new RuntimeException("Unrecognized open_chat type = " + type);
		}
		intent.putExtra("openChatId", openChatId);

		context.startActivity(intent);
	}

	class ViewHolder
			extends MultiSelectViewHolder {

		private String account;
		private int id;
		private String jid;
		private StorkAvatarView mContactAvatar;
		private TextView mContactName;
		private ImageView mDeliveryStatus;
		private TextView mLastMessage;
		private ImageView mStatus;
		private int type;

		public ViewHolder(View itemView) {
			super(itemView, MyOpenChatItemRecyclerViewAdapter.this.getFragment());

			this.mContactName = (TextView) itemView.findViewById(R.id.contact_display_name);
			this.mLastMessage = (TextView) itemView.findViewById(R.id.last_message);
			this.mContactAvatar = (StorkAvatarView) itemView.findViewById(R.id.contact_avatar);
			this.mStatus = (ImageView) itemView.findViewById(R.id.contact_presence);
			this.mDeliveryStatus = (ImageView) itemView.findViewById(R.id.chat_delivery_status);

			addClickable(this.mContactName);
			addClickable(this.mLastMessage);
			addClickable(this.mContactAvatar);
			addClickable(this.mStatus);
			addClickable(this.mDeliveryStatus);
		}

		public void bind(Cursor cursor) {
			this.id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ID));
			this.jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_JID));
			this.account = cursor.getString(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_ACCOUNT));
			this.type = cursor.getInt(cursor.getColumnIndex(DatabaseContract.OpenChats.FIELD_TYPE));
			final String name = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_NAME));
			final String lastMessage = cursor.getString(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE));
			final int presence = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_CONTACT_PRESENCE));
			final int lastMessageState = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE_STATE));
			final int unreadCount = cursor.getInt(cursor.getColumnIndex(ChatProvider.FIELD_UNREAD_COUNT));

			int presenceIconResource = PresenceIconMapper.getPresenceResource(presence);

			mLastMessage.setText(lastMessage);

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

			switch (type) {
				case DatabaseContract.OpenChats.TYPE_MUC:
					mStatus.setVisibility(View.GONE);
					mContactName.setText(context.getString(R.string.openchats_room, name));
					mContactAvatar.setJID(BareJID.bareJIDInstance(jid), name);
//				holder.setContextMenu(R.menu.openchat_groupchat_context, menuListener);
					mContactAvatar.setImageResource(R.drawable.ic_menu_groupchat);
					break;
				case DatabaseContract.OpenChats.TYPE_CHAT:
					mStatus.setImageResource(presenceIconResource);
					mStatus.setVisibility(View.VISIBLE);
					String n = context.getString(R.string.openchats_chat, name);
					mContactName.setText(n);
					mContactAvatar.setJID(BareJID.bareJIDInstance(jid), n);
//					AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), mContactAvatar);
					break;
			}
		}

		@Override
		protected void onItemClick(View v) {
			onEnterToChat(this.type, this.id, this.jid, this.account);
		}

	}

}
