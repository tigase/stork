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

package org.tigase.messenger.phone.pro.conenctionStatus;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;

public class ViewHolder
		extends RecyclerView.ViewHolder {

	TextView mConnected;
	TextView mResumption;
	TextView mServerName;
	TextView mStage;

	public ViewHolder(final View itemView) {
		super(itemView);

		this.mServerName = (TextView) itemView.findViewById(R.id.server_name);
		this.mStage = (TextView) itemView.findViewById(R.id.server_status_stage);
		this.mConnected = (TextView) itemView.findViewById(R.id.server_status_connected);
		this.mResumption = (TextView) itemView.findViewById(R.id.server_status_sessionresumption);
	}

	public void setContextMenu(final int menuId, final PopupMenu.OnMenuItemClickListener menuClick) {
		itemView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
				popup.inflate(menuId);
				popup.setOnMenuItemClickListener(menuClick);
				popup.show();
				return true;
			}
		});
	}

}
