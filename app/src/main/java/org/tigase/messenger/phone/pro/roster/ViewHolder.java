/*
 * ViewHolder.java
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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tigase.messenger.phone.pro.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ViewHolder extends RecyclerView.ViewHolder {

	@Bind(R.id.contact_jid)
	TextView mJidView;

	@Bind(R.id.contact_display_name)
	TextView mContactNameView;

	@Bind(R.id.contact_presence)
	ImageView mContactPresence;

	@Bind(R.id.contact_avatar)
	ImageView mContactAvatar;

	public ViewHolder(View itemView) {
		super(itemView);
		ButterKnife.bind(this, itemView);
	}

	@Override
	public String toString() {
		return super.toString() + " '" + mContactNameView.getText() + "'";
	}
}
