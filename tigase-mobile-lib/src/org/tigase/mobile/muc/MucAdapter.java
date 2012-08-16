package org.tigase.mobile.muc;

import java.sql.Date;

import org.tigase.mobile.R;
import org.tigase.mobile.db.ChatTableMetaData;

import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MucAdapter extends SimpleCursorAdapter {

	static class ViewHolder {
		ImageView avatar;
		TextView nickname;
		TextView timestamp;
		TextView webview;
	}

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID /*
																		 * ,
																		 * VCardsCacheTableMetaData
																		 * .
																		 * FIELD_DATA
																		 */};

	private final static int[] names = new int[] { R.id.chat_item_body };

	static int getOccupantColor(final String nick) {
		int color = Math.abs(nick.hashCode()) % 17;

		switch (color) {
		case 0:
			return R.color.mucmessage_his_nickname_0;
		case 1:
			return R.color.mucmessage_his_nickname_1;
		case 2:
			return R.color.mucmessage_his_nickname_2;
		case 3:
			return R.color.mucmessage_his_nickname_3;
		case 4:
			return R.color.mucmessage_his_nickname_4;
		case 5:
			return R.color.mucmessage_his_nickname_5;
		case 6:
			return R.color.mucmessage_his_nickname_6;
		case 7:
			return R.color.mucmessage_his_nickname_7;
		case 8:
			return R.color.mucmessage_his_nickname_8;
		case 9:
			return R.color.mucmessage_his_nickname_9;
		case 10:
			return R.color.mucmessage_his_nickname_10;
		case 11:
			return R.color.mucmessage_his_nickname_11;
		case 12:
			return R.color.mucmessage_his_nickname_12;
		case 13:
			return R.color.mucmessage_his_nickname_13;
		case 14:
			return R.color.mucmessage_his_nickname_14;
		case 15:
			return R.color.mucmessage_his_nickname_15;
		case 16:
			return R.color.mucmessage_his_nickname_16;
		default:
			return R.color.mucmessage_his_nickname_0;
		}
	}

	private final Room room;

	public MucAdapter(Context context, int layout, Room room) {
		super(context, layout, null, cols, names, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		this.room = room;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
			holder.webview = (TextView) view.findViewById(R.id.chat_item_body);
			holder.timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
			holder.avatar = (ImageView) view.findViewById(R.id.user_avatar);
		}

		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		// byte[] avatarData =
		// cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
		holder.avatar.setVisibility(View.GONE);

		// final BareJID account =
		// BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));
		final String nick = cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));

		// JaxmppCore jaxmpp = ((MessengerApplication)
		// context.getApplicationContext()).getMultiJaxmpp().get(account);
		holder.nickname.setText(nick);

		final String txt = EscapeUtils.escape(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_BODY)));

		if (nick.equals(room.getNickname())) {
			holder.nickname.setTextColor(context.getResources().getColor(R.color.mucmessage_mine_nickname));
			holder.webview.setTextColor(context.getResources().getColor(R.color.mucmessage_mine_text));
			holder.timestamp.setTextColor(context.getResources().getColor(R.color.mucmessage_mine_text));
			view.setBackgroundColor(context.getResources().getColor(R.color.mucmessage_mine_background));
		} else {
			int colorRes = getOccupantColor(nick);

			if (txt.contains(room.getNickname())) {
				view.setBackgroundColor(context.getResources().getColor(R.color.mucmessage_his_background_marked));
			} else {
				view.setBackgroundColor(context.getResources().getColor(R.color.mucmessage_his_background));
			}

			holder.nickname.setTextColor(context.getResources().getColor(colorRes));
			holder.webview.setTextColor(context.getResources().getColor(R.color.mucmessage_his_text));
			holder.timestamp.setTextColor(context.getResources().getColor(R.color.mucmessage_his_text));
		}

		java.text.DateFormat df = DateFormat.getTimeFormat(context);

		holder.webview.setText(Html.fromHtml(txt.replace(room.getNickname(), "<b>" + room.getNickname() + "</b>")));
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		Date t = new Date(cursor.getLong(cursor.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
		holder.timestamp.setText(df.format(t));

	}
}
