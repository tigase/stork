package org.tigase.mobile.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.view.Menu;
import android.view.MenuItem;

public class IconContextMenu {
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info);
	}

	public static final int ICON_LEFT = 0;
	public static final int ICON_RIGHT = 1;

	private final AlertDialog dialog;
	private IconContextItemSelectedListener iconContextItemSelectedListener;

	private Object info;
	private final Menu menu;

	public IconContextMenu(Context context, Menu menu, String title, int iconPosition) {
		this.menu = menu;

		final IconContextMenuAdapter adapter = new IconContextMenuAdapter(context, menu, iconPosition);

		this.dialog = new AlertDialog.Builder(context).setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (iconContextItemSelectedListener != null) {
					iconContextItemSelectedListener.onIconContextItemSelected(adapter.getItem(which), info);
				}
				finishMenu();
			}
		}).setInverseBackgroundForced(true).create();
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				finishMenu();
			}
		});
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finishMenu();
			}
		});
		dialog.setTitle(title);
	}

	public void cancel() {
		finishMenu();
		dialog.cancel();
	}

	public void dismiss() {
		finishMenu();
		dialog.dismiss();
	}

	private void finishMenu() {
		menu.clear();
		menu.close();
	}

	public AlertDialog getDialog() {
		return dialog;
	}

	public Object getInfo() {
		return info;
	}

	public Menu getMenu() {
		return menu;
	}

	public void setInfo(Object info) {
		this.info = info;
	}

	public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
		dialog.setOnCancelListener(onCancelListener);
	}

	public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
		dialog.setOnDismissListener(onDismissListener);
	}

	public void setOnIconContextItemSelectedListener(IconContextItemSelectedListener iconContextItemSelectedListener) {
		this.iconContextItemSelectedListener = iconContextItemSelectedListener;
	}

	public void setTitle(CharSequence title) {
		dialog.setTitle(title);
	}

	public void setTitle(int titleId) {
		dialog.setTitle(titleId);
	}

	public void show() {
		dialog.show();
	}
}
