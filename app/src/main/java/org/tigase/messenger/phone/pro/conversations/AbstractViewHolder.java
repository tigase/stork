package org.tigase.messenger.phone.pro.conversations;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;

public abstract class AbstractViewHolder
		extends RecyclerView.ViewHolder {

	protected final ImageView mAvatar;
	protected final TextView mContentView;
	protected final ImageView mDeliveryStatus;
	protected final TextView mNickname;
	protected final TextView mTimestamp;
	protected String ownNickname;

	public AbstractViewHolder(View itemView) {
		super(itemView);
		mContentView = (TextView) itemView.findViewById(R.id.content);
		mTimestamp = (TextView) itemView.findViewById(R.id.chat_timestamp);
		mDeliveryStatus = (ImageView) itemView.findViewById(R.id.chat_delivery_status);
		mAvatar = (ImageView) itemView.findViewById(R.id.contact_avatar);
		mNickname = (TextView) itemView.findViewById(R.id.nickname);
	}

	public abstract void bind(Context context, Cursor cursor);

	public String getOwnNickname() {
		return ownNickname;
	}

	public void setOwnNickname(String ownNickname) {
		this.ownNickname = ownNickname;
	}
}
