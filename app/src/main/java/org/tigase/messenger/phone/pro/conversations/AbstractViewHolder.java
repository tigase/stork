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

package org.tigase.messenger.phone.pro.conversations;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectViewHolder;

public abstract class AbstractViewHolder
		extends MultiSelectViewHolder {

	protected final ImageView mAvatar;
	protected final TextView mContentView;
	protected final ImageView mDeliveryStatus;
	protected final TextView mNickname;
	protected final TextView mTimestamp;
	protected String ownNickname;

	public AbstractViewHolder(View itemView, MultiSelectFragment fragment) {
		super(itemView, fragment);
		mContentView = (TextView) itemView.findViewById(R.id.content);
		mTimestamp = (TextView) itemView.findViewById(R.id.chat_timestamp);
		mDeliveryStatus = (ImageView) itemView.findViewById(R.id.chat_delivery_status);
		mAvatar = (ImageView) itemView.findViewById(R.id.contact_avatar);
		mNickname = (TextView) itemView.findViewById(R.id.nickname);

		addClickable(mContentView);
		addClickable(mTimestamp);
		addClickable(mDeliveryStatus);
		addClickable(mAvatar);
		addClickable(mNickname);
	}

	public abstract void bind(Context context, Cursor cursor);

	public String getOwnNickname() {
		return ownNickname;
	}

	public void setOwnNickname(String ownNickname) {
		this.ownNickname = ownNickname;
	}
}
