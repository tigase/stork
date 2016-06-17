package org.tigase.messenger.phone.pro.conversations.muc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class MucItemRecyclerViewAdapter extends CursorRecyclerViewAdapter<ViewHolder> {

	public static final int ITEM_ERROR = 1;
	public static final int ITEM_SYS_MSG = 2;
	public static final int ITEM_MESSAGE_IN = 20;
	public static final int ITEM_MESSAGE_OUT = 30;

	private final MucItemFragment.ChatItemIterationListener mListener;
	private final Context context;
	private String ownNickname;

	public MucItemRecyclerViewAdapter(Context context, Cursor cursor, MucItemFragment.ChatItemIterationListener listener) {
		super(cursor);
		mListener = listener;
		this.context = context;
	}

	private int getColor(String nickname) {
		final int i = ((Math.abs(nickname.hashCode()) + 3) * 13) % 19;
		switch (i) {
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
			case 17:
				return R.color.mucmessage_his_nickname_17;
			case 18:
				return R.color.mucmessage_his_nickname_18;
			case 19:
				return R.color.mucmessage_his_nickname_19;
			default:
				return R.color.mucmessage_his_nickname_0;

		}

	}

	@Override
	public int getItemViewType(int i) {
		if (!isDataValid()) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		if (!getCursor().moveToPosition(i)) {
			throw new IllegalStateException("couldn't move cursor to position " + i);
		}

		final int state = getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));
		final int type = getCursor().getInt(getCursor().getColumnIndex(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE));
		final String nickname = getCursor().getString(getCursor().getColumnIndex(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME));

		switch (state) {
			case DatabaseContract.ChatHistory.STATE_INCOMING:
			case DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD:
				if (type == DatabaseContract.ChatHistory.ITEM_TYPE_ERROR) {
					return ITEM_ERROR;
				} else if (type == DatabaseContract.ChatHistory.ITEM_TYPE_SYSTEM_MESSAGE) {
					return ITEM_SYS_MSG;
				} else {
					return ITEM_MESSAGE_IN;
				}
			case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
			case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
			case DatabaseContract.ChatHistory.STATE_OUT_SENT:
				return ITEM_MESSAGE_OUT;
			default:
				return -1;
		}

	}

	public String getOwnNickname() {
		return ownNickname;
	}

	public void setOwnNickname(String ownNickname) {
		this.ownNickname = ownNickname;
	}

	@Override
	public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID));
		final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
		final String nickname = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME));
		final long timestampt = cursor.getLong(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_TIMESTAMP));
		final int state = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_STATE));

		holder.mContentView.setText(body);
		holder.mTimestamp.setText(DateUtils.getRelativeDateTimeString(context, timestampt, DateUtils.MINUTE_IN_MILLIS,
				DateUtils.WEEK_IN_MILLIS, 0));
		if (holder.mNickname != null) {
			holder.mNickname.setVisibility(View.VISIBLE);
			int col = ContextCompat.getColor(context, getColor(nickname));
			holder.mNickname.setTextColor(col);
			holder.mNickname.setText(nickname);
		}

		boolean mentioned = (state == DatabaseContract.ChatHistory.STATE_INCOMING || state == DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD)
				&& body != null
				&& ownNickname != null
				&& body.toLowerCase().contains(ownNickname.toLowerCase());
		if (mentioned) {
			holder.mContentView.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.mContentView.setTypeface(Typeface.DEFAULT);
		}

		if (holder.mDeliveryStatus != null) {
			switch (state) {
				case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_not_sent_24dp);
					break;
				case DatabaseContract.ChatHistory.STATE_OUT_SENT:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_sent_24dp);
					break;
				case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
					holder.mDeliveryStatus.setImageResource(R.drawable.ic_message_delivered_24dp);
					break;
			}
		}

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (null != mListener) {
					// Notify the active callbacks interface (the activity, if
					// the
					// fragment is attached to one) that an item has been
					// selected.
					// mListener.onListFragmentInteraction(holder.mItem);
				}
			}
		});
		holder.setContextMenu(R.menu.chatitem_context, new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_chat_copytext) {
					mListener.onCopyChatMessage(id, jid, body);
					return true;
				} else
					return false;
			}
		});
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view;
		switch (viewType) {
			case ITEM_MESSAGE_IN:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_groupchatitem_received, parent, false);
				break;
			case ITEM_MESSAGE_OUT:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_groupchatitem_sent, parent, false);
				break;
			case ITEM_ERROR:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chatitem_error, parent, false);
				break;
			case ITEM_SYS_MSG:
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_chatitem_sysmsg, parent, false);
				break;
			default:
				throw new RuntimeException("Unknown view type " + viewType);
		}

		return new ViewHolder(view);
	}

}
