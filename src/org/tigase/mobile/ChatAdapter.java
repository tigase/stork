package org.tigase.mobile;

import java.sql.Date;

import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChatAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_TYPE, ChatTableMetaData.FIELD_JID };
	private final static int[] names = new int[] { R.id.chat_item_body };

	protected int[] mFrom;

	public ChatAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		TextView nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
		TextView webview = (TextView) view.findViewById(R.id.chat_item_body);
		TextView timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);

		final int type = cursor.getInt(mFrom[3]);

		if (type == 0) {
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(mFrom[4]));
			RosterItem ri = XmppService.jaxmpp().getRoster().get(jid);
			nickname.setText(MessengerDatabaseHelper.getDisplayName(ri));
		} else if (type == 1) {
			nickname.setText("Ja");
		} else {
			nickname.setText("?");
		}

		java.text.DateFormat df = DateFormat.getTimeFormat(context);

		final String txt = cursor.getString(mFrom[1]);

		webview.setText(txt);
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		Date t = new Date(cursor.getLong(mFrom[0]));
		timestamp.setText(df.format(t));

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
