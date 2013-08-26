/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class IconContextMenuAdapter extends BaseAdapter {
	private Context context;
	private int iconPosition;
	private Menu menu;

	public IconContextMenuAdapter(Context context, Menu menu, int iconPosition) {
		this.context = context;
		this.menu = menu;
		this.iconPosition = iconPosition;
	}

	@Override
	public int getCount() {
		return menu.size();
	}

	@Override
	public MenuItem getItem(int position) {
		return menu.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getItemId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		MenuItem item = getItem(position);

		TextView res = (TextView) convertView;
		if (res == null) {
			res = (TextView) LayoutInflater.from(context).inflate(android.R.layout.select_dialog_item, null);
		}

		res.setTag(item);
		res.setText(item.getTitle());

		res.setPadding(8, 8, 8, 8);

		switch (iconPosition) {
		case IconContextMenu.ICON_RIGHT:
			res.setCompoundDrawablesWithIntrinsicBounds(null, null, item.getIcon(), null);
			break;
		case IconContextMenu.ICON_LEFT:
		default:
			res.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
			break;
		}

		return res;
	}
}