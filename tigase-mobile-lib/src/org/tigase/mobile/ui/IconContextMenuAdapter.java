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