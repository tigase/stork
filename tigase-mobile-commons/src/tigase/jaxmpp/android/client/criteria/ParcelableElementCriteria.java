package tigase.jaxmpp.android.client.criteria;

import android.os.Parcel;
import android.os.Parcelable;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;

public class ParcelableElementCriteria extends ElementCriteria implements Parcelable {

	public static final Parcelable.Creator<ParcelableElementCriteria> CREATOR = new Parcelable.Creator<ParcelableElementCriteria>() {

		@Override
		public ParcelableElementCriteria createFromParcel(Parcel source) {
			// name
			String name = source.readString();
			// attrs
			int size = source.readInt();
			String[] keys = new String[size];
			source.readStringArray(keys);
			String[] values = new String[size];
			source.readStringArray(values);
			
			ParcelableElementCriteria criteria = new ParcelableElementCriteria(name, keys, values);
			// nextCriteria
			Criteria nextCriteria = (Criteria) source.readValue(null);
			if (nextCriteria != null) {
				criteria.add(nextCriteria);
			}
			
			return criteria;
		}

		@Override
		public ParcelableElementCriteria[] newArray(int size) {
			return new ParcelableElementCriteria[size];
		}
		
	};
	
	public static final ParcelableElementCriteria emptyP() {
		return new ParcelableElementCriteria(null, null, null);
	}

	public static final ParcelableElementCriteria nameP(String name) {
		return new ParcelableElementCriteria(name, null, null);
	}

	public static final ParcelableElementCriteria nameP(String name, String xmlns) {
		return new ParcelableElementCriteria(name, new String[] { "xmlns" }, new String[] { xmlns });
	}

	public static final ParcelableElementCriteria nameP(String name, String[] attNames, String[] attValues) {
		return new ParcelableElementCriteria(name, attNames, attValues);
	}

	public static final ParcelableElementCriteria xmlnsP(String xmlns) {
		return new ParcelableElementCriteria(null, new String[] { "xmlns" }, new String[] { xmlns });
	}
	
	
	public ParcelableElementCriteria(String name, String[] attname, String[] attValue) {
		super(name, attname, attValue);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(this.name);
		int size = this.attrs.size();
		String[] keys = this.attrs.keySet().toArray(new String[size]);
		String[] values = new String[size];
		for (int i=0; i<keys.length; i++) {
			values[i] = this.attrs.get(keys[i]);
		}
		dest.writeInt(size);
		dest.writeStringArray(keys);
		dest.writeStringArray(values);
		dest.writeParcelable((Parcelable) this.nextCriteria, flags);
	}

}
