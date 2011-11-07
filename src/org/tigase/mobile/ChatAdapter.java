package org.tigase.mobile;

import java.sql.Date;

import org.tigase.mobile.db.ChatTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChatAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID };
	private final static int[] names = new int[] { R.id.chat_item_body };

	protected int[] mFrom;

	private final RosterDisplayTools rdt;

	public ChatAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
		this.rdt = new RosterDisplayTools(context);
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		TextView nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
		TextView webview = (TextView) view.findViewById(R.id.chat_item_body);
		TextView timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
		ImageView msgStatus = (ImageView) view.findViewById(R.id.msgStatus);

		final int state = cursor.getInt(mFrom[2]);

		if (state == ChatTableMetaData.STATE_INCOMING) {
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(mFrom[3]));
			RosterItem ri = XmppService.jaxmpp(context).getRoster().get(jid);
			nickname.setText(ri == null ? jid.toString() : rdt.getDisplayName(ri));

			nickname.setTextColor(context.getResources().getColor(R.color.message_his_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_his_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_his_text));

			view.setBackgroundColor(context.getResources().getColor(R.color.message_his_background));
			msgStatus.setVisibility(View.GONE);
		} else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT || state == ChatTableMetaData.STATE_OUT_SENT) {
			nickname.setText("Ja");

			nickname.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_mine_text));

			if (state == ChatTableMetaData.STATE_OUT_SENT)
				msgStatus.setVisibility(View.GONE);
			else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT)
				msgStatus.setVisibility(View.VISIBLE);

			view.setBackgroundColor(context.getResources().getColor(R.color.message_mine_background));
		} else {
			msgStatus.setVisibility(View.GONE);
			nickname.setText("?");
		}

		java.text.DateFormat df = DateFormat.getTimeFormat(context);
		final String txt = EscapeUtils.escape(cursor.getString(mFrom[1]));
		webview.setText(Html.fromHtml(txt));
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		Date t = new Date(cursor.getLong(mFrom[0]));
		timestamp.setText(df.format(t));

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
