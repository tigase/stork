/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.serverfeatures;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.tigase.messenger.phone.pro.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

class FeaturesAdapter
		extends RecyclerView.Adapter<ViewHolder> {

	private final Context context;
	private final ArrayList<FeatureItem> featureItems = new ArrayList<>();
	private final FeaturesProvider featuresProvider = new FeaturesProvider();
	private final HashSet<String> serverFeatures = new HashSet<>();

	FeaturesAdapter(Context contex) {
		this.context = contex;
	}

	public void addFeature(String xmlns) {
		this.serverFeatures.add(xmlns);
		updateItems();
	}

	public void addFeatures(Collection<String> serverFeatures) {
		this.serverFeatures.addAll(serverFeatures);
		updateItems();
	}

	@Override
	public int getItemCount() {
		return featureItems.size();
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		FeatureItem item = featureItems.get(position);
		holder.display(item);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.fragment_serverfeature_item, parent, false);
		return new ViewHolder(view);
	}

	public void setFeatures(Collection<String> serverFeatures) {
		this.serverFeatures.clear();
		this.serverFeatures.addAll(serverFeatures);
		updateItems();
	}

	private boolean isOnServerFeaturesList(final FeatureItem item) {
		if (item.getXmlns().contains("*")) {
			final String t = item.getXmlns().substring(0, item.getXmlns().length() - 1);

			for (String serverFeature : serverFeatures) {
				if (serverFeature.startsWith(t)) {
					return true;
				}
			}

			return false;
		} else {
			return this.serverFeatures.contains(item.getXmlns());
		}
	}

	private void updateItems() {
		featureItems.clear();
		if (serverFeatures != null) {
			for (FeatureItem item : featuresProvider.get(context)) {
				if (isOnServerFeaturesList(item)) {
					featureItems.add(item);
				}
			}
		}
		notifyDataSetChanged();
	}
}
