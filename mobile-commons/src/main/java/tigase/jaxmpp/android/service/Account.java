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
import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable {

	public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {

		@Override
		public Account createFromParcel(Parcel source) {
			String jid = source.readString();
			boolean[] bools = new boolean[1];
			source.readBooleanArray(bools);
			return new Account(BareJID.bareJIDInstance(jid), bools[0]);
		}

		@Override
		public Account[] newArray(int size) {
			return new Account[size];
		}

	};

	private boolean connected;
	private BareJID jid;

	public Account(BareJID jid, boolean connected) {
		this.jid = jid;
		this.connected = connected;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public BareJID getJid() {
		return jid;
	}

	public boolean isConnected() {
		return connected;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(jid.toString());
		dest.writeBooleanArray(new boolean[] { connected });
	}

}
