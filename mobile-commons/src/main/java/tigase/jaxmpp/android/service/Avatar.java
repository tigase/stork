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
