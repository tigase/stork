package org.tigase.mobile;

import org.tigase.mobile.db.ChatTableMetaData;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChatAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE };
	private final static int[] names = new int[] { R.id.chat_item_body };

	protected int[] mFrom;

	public ChatAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		TextView webview = (TextView) view.findViewById(R.id.chat_item_body);
		TextView timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);

		final String txt = cursor.getString(mFrom[1]);

		webview.setText(txt);
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		timestamp.setText(cursor.getString(mFrom[0]));

	}

	private String buildHtmlCode(int type, String articleDescription) {
		String html = "";
		String style = "";

		style += "<style>" + "body { color: ";
		if (type == 0)
			style += "blue";
		else if (type == 0)
			style += "red";
		else
			style += "#f0f0f0";
		style += "; font-size: 14px;}" + "</style>";

		html += "<html>" + "<head>" + style + "</head>" + "<body>" + articleDescription + "</body>" + "</html>";

		return html;
	}

	private void findColumns(String[] from, Cursor mCursor) {
		int i;
		int count = from.length;
		if (mFrom == null) {
			mFrom = new int[count];
		}
		if (mCursor != null) {
			for (i = 0; i < count; i++) {
				mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
			}
		} else {
			for (i = 0; i < count; i++) {
				mFrom[i] = -1;
			}
		}
	}
}
