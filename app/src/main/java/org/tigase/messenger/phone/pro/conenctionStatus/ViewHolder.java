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

package org.tigase.messenger.phone.pro.conenctionStatus;

import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import org.tigase.messenger.phone.pro.R;

public class ViewHolder
		extends RecyclerView.ViewHolder {

	TextView mAuthSatus;
	TextView mConnected;
	TextView mResumption;
	TextView mServerName;
	TextView mSessionBind;
	TextView mStage;
	TextView mTlsStatus;
	TextView mZlibStatus;

	public ViewHolder(final View itemView) {
		super(itemView);

		this.mServerName = itemView.findViewById(R.id.server_name);
		this.mStage = itemView.findViewById(R.id.server_status_stage);
		this.mConnected = itemView.findViewById(R.id.server_status_connected);
		this.mResumption = itemView.findViewById(R.id.server_status_sessionresumption);
		this.mAuthSatus = itemView.findViewById(R.id.server_status_authsatus);
		this.mTlsStatus = itemView.findViewById(R.id.server_status_sslstatus);
		this.mZlibStatus = itemView.findViewById(R.id.server_status_zlibstatus);
		this.mSessionBind = itemView.findViewById(R.id.server_status_sessionbinded);
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
