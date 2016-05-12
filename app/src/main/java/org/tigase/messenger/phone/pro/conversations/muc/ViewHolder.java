package org.tigase.messenger.phone.pro.conversations.muc;

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
	TextView mNickname;
	ImageView mDeliveryStatus;

	public ViewHolder(View itemView) {
		super(itemView);
		ButterKnife.bind(this, itemView);
		mDeliveryStatus = (ImageView) itemView.findViewById(R.id.chat_delivery_status);
		mNickname = (TextView) itemView.findViewById(R.id.nickname);
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
}
