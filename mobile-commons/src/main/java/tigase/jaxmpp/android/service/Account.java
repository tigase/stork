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
