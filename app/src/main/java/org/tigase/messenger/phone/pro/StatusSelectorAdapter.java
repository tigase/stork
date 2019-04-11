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

package org.tigase.messenger.phone.pro;

import android.content.Context;
import android.widget.SimpleAdapter;
import org.tigase.messenger.phone.pro.db.CPresence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusSelectorAdapter
		extends SimpleAdapter {

	private static Map<String, ?> c(String name, String label, Integer value, Integer imgId) {
		HashMap<String, Object> tmp = new HashMap<>();
		tmp.put("name", name);
		tmp.put("label", label);
		tmp.put("_id", value);
		tmp.put("img", imgId);
		return tmp;
	}

	public static StatusSelectorAdapter instance(Context context) {
		List<Map<String, ?>> data = new ArrayList<>();
		data.add(c("chat", context.getString(R.string.status_chat), CPresence.CHAT, R.drawable.presence_chat));
		data.add(c("online", context.getString(R.string.status_online), CPresence.ONLINE, R.drawable.presence_online));
		data.add(c("away", context.getString(R.string.status_away), CPresence.AWAY, R.drawable.presence_away));
		data.add(c("xa", context.getString(R.string.status_xa), CPresence.XA, R.drawable.presence_xa));
		data.add(c("dnd", context.getString(R.string.status_dnd), CPresence.DND, R.drawable.presence_dnd));
		data.add(c("offline", context.getString(R.string.status_offline), CPresence.OFFLINE,
				   R.drawable.presence_offline));
		return new StatusSelectorAdapter(context, data, R.layout.status_selectoritem, new String[]{"label", "img"},
										 new int[]{R.id.content, R.id.id});
	}

	/**
	 * Constructor
	 *
	 * @param context The context where the View associated with this SimpleAdapter is running
	 * @param data A List of Maps. Each entry in the List corresponds to one row in the list. The Maps contain the data
	 * for each row, and should include all the entries specified in "from"
	 * @param resource Resource identifier of a view layout that defines the views for this list item. The layout file
	 * should include at least those named views defined in "to"
	 * @param from A list of column names that will be added to the Map associated with each item.
	 * @param to The views that should display column in the "from" parameter. These should all be TextViews. The first
	 * N views in this list are given the values of the first N columns
	 */
	private StatusSelectorAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from,
								  int[] to) {
		super(context, data, resource, from, to);
	}

	@Override
	public long getItemId(int position) {
		return ((Integer) ((Map<String, ?>) getItem(position)).get("_id")).longValue();
	}

	public int getPosition(final long itemId) {
		for (int i = 0; i < getCount(); i++) {
			long x = getItemId(i);
			if (x == itemId) {
				return i;
			}
		}
		return 5;
	}
}
