package tigase.jaxmpp.android.client.criteria;

import android.os.Parcel;
import android.os.Parcelable;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.Or;

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
