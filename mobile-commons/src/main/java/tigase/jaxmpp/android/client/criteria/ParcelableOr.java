/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.jaxmpp.android.client.criteria;

import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.Or;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableOr extends Or implements Parcelable {

	public static final Parcelable.Creator<ParcelableOr> CREATOR = new Parcelable.Creator<ParcelableOr>() {

		@Override
		public ParcelableOr createFromParcel(Parcel source) {
			Criteria[] criterias = (Criteria[]) source.readArray(null);
			return new ParcelableOr(criterias);
		}

		@Override
		public ParcelableOr[] newArray(int size) {
			return new ParcelableOr[size];
		}
	};

	public ParcelableOr(Criteria[] criteria) {
		super(criteria);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeArray(this.crits);
	}

}
