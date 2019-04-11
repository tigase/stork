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
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import org.tigase.messenger.phone.pro.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FeaturesProvider {

	private ArrayList<FeatureItem> items;

	public List<FeatureItem> get(final Context context) {
		if (items == null) {
			read(context);
			Collections.sort(items, new Comparator<FeatureItem>() {
				@Override
				public int compare(FeatureItem o1, FeatureItem o2) {
					return o1.getXep().compareTo(o2.getXep());
				}
			});
		}
		return Collections.unmodifiableList(this.items);
	}

	private void read(final Context context) {
		Resources res = context.getResources();
		items = new ArrayList<>();

		FeatureItem item = null;

		try (XmlResourceParser xrp = res.getXml(R.xml.features)) {
			xrp.next(); // skip first 'mountains' element
			while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
				xrp.next(); // get first 'mountain' element
				// double check its the right element
				if (xrp.getEventType() == XmlResourceParser.START_TAG && xrp.getName().equals("feature")) {
					String id = xrp.getAttributeValue(null, "id");
					String active = xrp.getAttributeValue(null, "active");
					if ((id == null || id.trim().isEmpty()) || (active != null && !Boolean.parseBoolean(active))) {
						item = null;
					} else {
						item = new FeatureItem(id);
					}
				}

				if (xrp.getEventType() == XmlResourceParser.START_TAG && xrp.getName().equals("xep") && item != null) {
					xrp.next();
					item.setXep(xrp.getText());
				}
				if (xrp.getEventType() == XmlResourceParser.START_TAG && xrp.getName().equals("description") &&
						item != null) {
					xrp.next();
					item.setDescription(xrp.getText());
				}
				if (xrp.getEventType() == XmlResourceParser.START_TAG && xrp.getName().equals("name") && item != null) {
					xrp.next();
					item.setName(xrp.getText());
				}

				if (xrp.getEventType() == XmlResourceParser.END_TAG && xrp.getName().equals("feature") &&
						item != null) {
					items.add(item);
					item = null;

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("FeaturesProvider", "Cannot parse server features", e);
		}
	}

}
