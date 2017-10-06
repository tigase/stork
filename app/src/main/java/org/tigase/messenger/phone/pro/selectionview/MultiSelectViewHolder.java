package org.tigase.messenger.phone.pro.selectionview;

import android.view.View;
import com.bignerdranch.android.multiselector.SwappingHolder;

public abstract class MultiSelectViewHolder
		extends SwappingHolder
		implements View.OnLongClickListener, View.OnClickListener {

	private final MultiSelectFragment fragment;

	public MultiSelectViewHolder(View itemView, MultiSelectFragment multiSelectFragment) {
		super(itemView, multiSelectFragment.getMultiSelector());
		this.fragment = multiSelectFragment;
		itemView.setLongClickable(true);
		itemView.setOnLongClickListener(this);
		itemView.setClickable(true);
		itemView.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (fragment.getMultiSelector().isSelectable()) {
			fragment.getMultiSelector().tapSelection(this);
			fragment.updateAction();
		} else {
			onItemClick(v);
		}
	}

	protected abstract void onItemClick(View v);

//	protected abstract boolean onItemLongClick(View v);

	@Override
	public boolean onLongClick(View v) {
		if (!fragment.getMultiSelector().isSelectable()) {
			fragment.startActionMode();
			fragment.getMultiSelector().setSelectable(true);
			fragment.getMultiSelector().setSelected(this, true);
			fragment.updateAction();
			return true;
		} else if (fragment.getMultiSelector().isSelectable()) {
			fragment.getMultiSelector().tapSelection(this);
			fragment.updateAction();
			return true;
		} else {
			return false;
//			return onItemLongClick(v);
		}
	}
}
