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
package org.tigase.messenger.phone.pro.db;

import android.os.Parcel;
import android.os.Parcelable;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

/**
 * Client side presence
 */
public class CPresence
		implements Parcelable {

	public static final int OFFLINE = 0;
	public static final int ERROR = 1;
	public static final int DND = 5;
	public static final int XA = 10;
	public static final int AWAY = 15;
	public static final int ONLINE = 20;
	public static final int CHAT = 25;
	public static final Creator<CPresence> CREATOR = new Creator<CPresence>() {
		public CPresence createFromParcel(Parcel in) {
			return new CPresence(in);
		}

		public CPresence[] newArray(int size) {
			return new CPresence[size];
		}
	};
	private final String description;
	private final Integer priority;
	private final String resource;
	private final int status;

	public static int getStatusFromPresence(Presence p) throws XMLException {
		if (p.getType() == StanzaType.error) {
			return ERROR;
		} else {
			switch (p.getShow()) {
				case online:
					return ONLINE;
				case xa:
					return XA;
				case away:
					return AWAY;
				case chat:
					return CHAT;
				case dnd:
					return DND;
				case offline:
					return OFFLINE;
				default:
					return OFFLINE;
			}
		}
	}

	private CPresence(Parcel in) {
		resource = in.readString();
		int priority = in.readInt();
		if (priority < -1000) {
			this.priority = 0;
		} else {
			this.priority = priority;
		}
		status = in.readInt();
		description = in.readString();
	}

	public CPresence(String resource, Integer priority, int status, String description) {
		this.resource = resource;
		this.priority = priority;
		this.status = status;
		this.description = description;
	}

	public CPresence(Presence p) throws XMLException {
		this.resource = p.getFrom().getResource();
		this.priority = p.getPriority();
		this.status = getStatusFromPresence(p);
		this.description = p.getStatus();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public String getDescription() {
		return description;
	}

	public Integer getPriority() {
		return priority;
	}

	public String getResource() {
		return resource;
	}

	public int getStatus() {
		return status;
	}

	public boolean isAvailable() {
		return status > 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(resource);
		dest.writeInt(priority == null ? -1000 : priority);
		dest.writeInt(status);
		dest.writeString(description);
	}

}
