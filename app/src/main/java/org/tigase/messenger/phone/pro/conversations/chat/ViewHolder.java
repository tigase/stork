package org.tigase.messenger.phone.pro.conversations.chat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import org.tigase.messenger.phone.pro.R;

public class ViewHolder extends RecyclerView.ViewHolder {

	@Bind(R.id.content)
	TextView mContentView;
	@Bind(R.id.chat_timestamp)
	TextView mTimestamp;
	ImageView mDeliveryStatus;
	ImageView mAvatar;

	public ViewHolder(View itemView) {
		super(itemView);
		ButterKnife.bind(this, itemView);
		mDeliveryStatus = (ImageView) itemView.findViewById(R.id.chat_delivery_status);
		mAvatar = (ImageView) itemView.findViewById(R.id.contact_avatar);
	}

	public void setContextMenu(final int menuId, final PopupMenu.OnMenuItemClickListener menuClick) {
		View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
				popup.inflate(menuId);
				popup.setOnMenuItemClickListener(menuClick);
				popup.show();
				return true;
			}
		};
		itemView.setOnLongClickListener(longClickListener);
		mContentView.setOnLongClickListener(longClickListener);
	}

	@Override
	public String toString() {
		return super.toString() + " '" + mContentView.getText() + "'";
	}

}
