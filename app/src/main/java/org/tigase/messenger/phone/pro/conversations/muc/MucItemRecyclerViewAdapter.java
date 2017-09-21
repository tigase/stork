package org.tigase.messenger.phone.pro.conversations.muc;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractViewHolder;
import org.tigase.messenger.phone.pro.conversations.ViewHolderImg;
import org.tigase.messenger.phone.pro.conversations.ViewHolderMsg;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class MucItemRecyclerViewAdapter
		extends CursorRecyclerViewAdapter<AbstractViewHolder> {

	private final Context context;
	private final MucItemFragment.ChatItemIterationListener mListener;
	private String ownNickname;

	public MucItemRecyclerViewAdapter(Context context, Cursor cursor,
									  MucItemFragment.ChatItemIterationListener listener) {
		super(cursor);
		mListener = listener;
		this.context = context;
	}

	private void bindViewHolderImg(ViewHolderImg holder, Cursor cursor) {
		holder.bind(context, cursor);
	}

	public void bindViewHolderMsg(ViewHolderMsg holder, Cursor cursor) {
		final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_ID));
		final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_JID));
		final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));

		holder.bind(context, cursor);

		holder.setContextMenu(R.menu.chatitem_context, new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_chat_copytext) {
					mListener.onCopyChatMessage(id, jid, body);
					return true;
				} else {
					return false;
				}
			}
		});
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

		return (state & 0x7fff) << 16 | (type & 0x7fff);
	}

	public String getOwnNickname() {
		return ownNickname;
	}

	public void setOwnNickname(String ownNickname) {
		this.ownNickname = ownNickname;
	}

	@Override
	public void onBindViewHolderCursor(AbstractViewHolder holder, Cursor cursor) {
		if (holder instanceof ViewHolderImg) {
			bindViewHolderImg((ViewHolderImg) holder, cursor);
		} else if (holder instanceof ViewHolderMsg) {
			bindViewHolderMsg((ViewHolderMsg) holder, cursor);
		} else {
			throw new RuntimeException("Unknown ViewHolder type");
		}
	}

	@Override
	public AbstractViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		AbstractViewHolder viewHolder;
		final int messageType = viewType & 0x7fff;
		final int messageState = ((viewType >> 16) & 0x7fff);

		switch (messageState) {
			case DatabaseContract.ChatHistory.STATE_INCOMING:
			case DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD:
				switch (messageType) {
					case DatabaseContract.ChatHistory.ITEM_TYPE_ERROR:
						viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
															   .inflate(R.layout.fragment_chatitem_error, parent,
																		false));
						break;
					default:
						viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
															   .inflate(R.layout.fragment_groupchatitem_received,
																		parent, false));
				}
				break;
			case DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT:
			case DatabaseContract.ChatHistory.STATE_OUT_DELIVERED:
			case DatabaseContract.ChatHistory.STATE_OUT_SENT:
				switch (messageType) {
					case DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE:
						viewHolder = new ViewHolderImg(context, LayoutInflater.from(parent.getContext())
								.inflate(R.layout.fragment_groupchatitem_sent_image, parent, false));
						break;
					default:
						viewHolder = new ViewHolderMsg(LayoutInflater.from(parent.getContext())
															   .inflate(R.layout.fragment_groupchatitem_sent, parent,
																		false));
				}
				break;
			default:
				throw new RuntimeException("Unknown view type (t=" + messageType + ", s=" + messageState + ")");
		}

		viewHolder.setOwnNickname(ownNickname);
		return viewHolder;
	}

}
