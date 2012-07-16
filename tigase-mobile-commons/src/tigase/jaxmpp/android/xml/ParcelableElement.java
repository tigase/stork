package tigase.jaxmpp.android.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

public class ParcelableElement extends DefaultElement implements Parcelable {

	private static final String TAG = "ParcelableElement";
	
	public static final Parcelable.Creator<ParcelableElement> CREATOR = new Parcelable.Creator<ParcelableElement>() {
		@Override
		public ParcelableElement createFromParcel(Parcel source) {
			String name = source.readString();
			String value = source.readString();
			String xmlns = source.readString();
			
			ParcelableElement parcelable = new ParcelableElement(name, value, xmlns);
			
			Map<String,String> attrs = (Map<String,String>) source.readHashMap(null);
			try {
				parcelable.setAttributes(attrs);
			} catch (XMLException e) {
				Log.e(TAG, e.getMessage(), e);
			}
			
			List<ParcelableElement> children = new ArrayList<ParcelableElement>();
			source.readList(children, this.getClass().getClassLoader());
			
			for (Element child : children) {
				try {
					parcelable.addChild(child);
				} catch (XMLException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
			
			return parcelable;
		}

		@Override
		public ParcelableElement[] newArray(int size) {
			return new ParcelableElement[size];
		}
	};
	
	public static final ParcelableElement fromElement(Element element) throws XMLException {
		if (element instanceof ParcelableElement) {
			return (ParcelableElement) element;
		}
		
		ParcelableElement parcelable = new ParcelableElement(element.getName(), element.getValue(), element.getXMLNS());
		parcelable.setAttributes(element.getAttributes());
		
		for (Element child : element.getChildren()) {
			Element pchild = fromElement(child);
			parcelable.addChild(pchild);
		}
		return parcelable;
	}
	
	public ParcelableElement(String name) {
		super(name);
	}
	
	public ParcelableElement(String name, String value, String xmlns) {
		super(name, value, xmlns);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		try {
			dest.writeString(getName());
			dest.writeString(getValue());
			dest.writeString(getXMLNS());

			dest.writeMap(getAttributes());
			dest.writeList(getChildren());
		}
		catch (XMLException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

}
