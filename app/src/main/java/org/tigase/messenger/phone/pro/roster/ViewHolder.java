/*
 * Tigase Android Messenger
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

package org.tigase.messenger.phone.pro.roster;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectViewHolder;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class ViewHolder
		extends MultiSelectViewHolder {

	private final Context context;
	ImageView mContactAvatar;
	TextView mContactNameView;

	ImageView mContactPresence;
	TextView mJidView;
	private String account;
	private String jid;

	public ViewHolder(final Context context, final View itemView, MultiSelectFragment fragment) {
		super(itemView, fragment);
		this.context = context;

		mJidView = (TextView) itemView.findViewById(R.id.contact_jid);
		mContactNameView = (TextView) itemView.findViewById(R.id.contact_display_name);
		mContactPresence = (ImageView) itemView.findViewById(R.id.contact_presence);
		mContactAvatar = (ImageView) itemView.findViewById(R.id.contact_avatar);

		addClickable(mJidView);
		addClickable(mContactNameView);
		addClickable(mContactPresence);
		addClickable(mContactAvatar);
	}

	public void bind(final Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
		this.jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));
		this.account = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
		final String name = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
		int status = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_STATUS));

		mContactNameView.setText(name);
		mContactPresence.setImageResource(PresenceIconMapper.getPresenceResource(status));
		mJidView.setText(jid);

		AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), mContactAvatar);

	}

	@Override
	public String toString() {
		return super.toString() + " '" + mContactNameView.getText() + "'";
	}

//	@Deprecated
//	public void setContextMenu(final int menuId, final PopupMenu.OnMenuItemClickListener menuClick) {
//		itemView.setOnLongClickListener(new View.OnLongClickListener() {
//			@Override
//			public boolean onLongClick(View v) {
//				PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
//				popup.inflate(menuId);
//				popup.setOnMenuItemClickListener(menuClick);
//				popup.show();
//				return true;
//			}
//		});
//
//	}

	@Override
	protected void onItemClick(View v) {
		Intent intent = new Intent(context, ChatActivity.class);
		intent.putExtra(ChatActivity.JID_KEY, jid);
		intent.putExtra(ChatActivity.ACCOUNT_KEY, account);
		context.startActivity(intent);
	}
}
