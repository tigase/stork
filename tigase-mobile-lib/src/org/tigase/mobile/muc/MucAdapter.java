package org.tigase.mobile.muc;

import java.sql.Date;

import org.tigase.mobile.R;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;

import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MucAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID, VCardsCacheTableMetaData.FIELD_DATA };
	private final static int[] names = new int[] { R.id.chat_item_body };

	private final Room room;

	public MucAdapter(Context context, int layout, Cursor c, Room room) {
		super(context, layout, c, cols, names);

		this.room = room;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
		TextView webview = (TextView) view.findViewById(R.id.chat_item_body);
		TextView timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);

		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		ImageView avatar = (ImageView) view.findViewById(R.id.user_avatar);
		// byte[] avatarData =
		// cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
		avatar.setVisibility(View.GONE);

		// final BareJID account =
		// BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));
		final String nick = cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));

		// JaxmppCore jaxmpp = ((MessengerApplication)
		// context.getApplicationContext()).getMultiJaxmpp().get(account);
		nickname.setText(nick);

		if (nick.equals(room.getNickname())) {
			nickname.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			view.setBackgroundColor(context.getResources().getColor(R.color.message_mine_background));
		} else {
			nickname.setTextColor(context.getResources().getColor(R.color.message_his_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_his_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_his_text));
			view.setBackgroundColor(context.getResources().getColor(R.color.message_his_background));
		}

		java.text.DateFormat df = DateFormat.getTimeFormat(context);
		final String txt = EscapeUtils.escape(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_BODY)));
		webview.setText(Html.fromHtml(txt));
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		Date t = new Date(cursor.getLong(cursor.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
		timestamp.setText(df.format(t));

	}
}
