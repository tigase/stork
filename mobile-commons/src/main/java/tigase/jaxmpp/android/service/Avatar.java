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
package tigase.jaxmpp.android.service;

import tigase.jaxmpp.core.client.BareJID;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Avatar implements Parcelable {

	public static final Parcelable.Creator<Avatar> CREATOR = new Parcelable.Creator<Avatar>() {

		@Override
		public Avatar createFromParcel(Parcel source) {
			BareJID jid = BareJID.bareJIDInstance(source.readString());
			Bitmap image = source.readParcelable(this.getClass().getClassLoader());
			return new Avatar(jid, image);
		}

		@Override
		public Avatar[] newArray(int size) {
			return new Avatar[size];
		}

	};

	private Bitmap image;
	private BareJID jid;

	public Avatar(BareJID jid, Bitmap image) {
		this.jid = jid;
		this.image = image;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public Bitmap getImage() {
		return image;
	}

	public BareJID getJid() {
		return jid;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(jid.toString());
		dest.writeParcelable(image, flags);
	}

}
