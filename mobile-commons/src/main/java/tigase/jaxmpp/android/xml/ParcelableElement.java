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
package tigase.jaxmpp.android.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ParcelableElement extends DefaultElement implements Parcelable {

	public static final Parcelable.Creator<ParcelableElement> CREATOR = new Parcelable.Creator<ParcelableElement>() {
		@Override
		public ParcelableElement createFromParcel(Parcel source) {
			String name = source.readString();
			String value = source.readString();
			String xmlns = source.readString();

			ParcelableElement parcelable = new ParcelableElement(name, value, xmlns);

			Map<String, String> attrs = source.readHashMap(null);
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

	private static final String TAG = "ParcelableElement";

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
		} catch (XMLException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

}
