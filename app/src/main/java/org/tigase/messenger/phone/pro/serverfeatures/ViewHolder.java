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

package org.tigase.messenger.phone.pro.serverfeatures;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;

class ViewHolder
		extends RecyclerView.ViewHolder {

	ViewHolder(View itemView) {
		super(itemView);
	}

	void display(final FeatureItem item) {
		TextView name = itemView.findViewById(R.id.feature_name);
		TextView description = itemView.findViewById(R.id.feature_description);

		name.setText(item.getXep() + ": " + item.getName());

		if (item.getDescription() == null || item.getDescription().isEmpty()) {
			description.setText("");
			description.setVisibility(View.VISIBLE);
		} else {
			description.setText(item.getDescription());
			description.setVisibility(View.VISIBLE);
		}

	}
}
