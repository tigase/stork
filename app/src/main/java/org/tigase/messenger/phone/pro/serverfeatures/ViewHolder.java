package org.tigase.messenger.phone.pro.serverfeatures;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;

class ViewHolder
		extends RecyclerView.ViewHolder {

	ViewHolder(View itemView) {
		super(itemView);
	}

	void display(final FeatureItem item) {
		TextView name = itemView.findViewById(R.id.feature_name);
		TextView description = itemView.findViewById(R.id.feature_description);

		name.setText(item.getXep() + ": " + item.getName());

		if (item.getDescription() == null || item.getDescription().isEmpty()) {
			description.setText("");
			description.setVisibility(View.VISIBLE);
		} else {
			description.setText(item.getDescription());
			description.setVisibility(View.VISIBLE);
		}

	}
}
