package org.tigase.messenger.phone.pro.account;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by bmalkow on 28.06.2017.
 */
public class PrioritiesEntity
		implements Parcelable {

	public static final Parcelable.Creator<PrioritiesEntity> CREATOR = new Parcelable.Creator<PrioritiesEntity>() {
		public PrioritiesEntity createFromParcel(Parcel in) {
			return new PrioritiesEntity(in);
		}

		public PrioritiesEntity[] newArray(int size) {
			return new PrioritiesEntity[size];
		}
	};
	private final static String SEPARATOR = ",";
	private int away = 3;
	private int chat = 5;
	private int dnd = 1;
	private int online = 4;
	private int xa = 2;

	public static PrioritiesEntity instance(String data) {
		PrioritiesEntity r = new PrioritiesEntity();
		if (data != null && !data.trim().isEmpty()) {
			String[] x = data.split(SEPARATOR);
			r.chat = Integer.parseInt(x[0]);
			r.online = Integer.parseInt(x[1]);
			r.away = Integer.parseInt(x[2]);
			r.xa = Integer.parseInt(x[3]);
			r.dnd = Integer.parseInt(x[4]);
		}
		return r;
	}

	public PrioritiesEntity() {
	}

	private PrioritiesEntity(Parcel in) {
		chat = in.readInt();
		online = in.readInt();
		away = in.readInt();
		xa = in.readInt();
		dnd = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public int getAway() {
		return away;
	}

	public void setAway(int away) {
		this.away = away;
	}

	public int getChat() {
		return chat;
	}

	public void setChat(int chat) {
		this.chat = chat;
	}

	public int getDnd() {
		return dnd;
	}

	public void setDnd(int dnd) {
		this.dnd = dnd;
	}

	public int getOnline() {
		return online;
	}

	public void setOnline(int online) {
		this.online = online;
	}

	public int getXa() {
		return xa;
	}

	public void setXa(int xa) {
		this.xa = xa;
	}

	@Override
	public String toString() {
		return chat + SEPARATOR + online + SEPARATOR + away + SEPARATOR + xa + SEPARATOR + dnd;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(chat);
		dest.writeInt(online);
		dest.writeInt(away);
		dest.writeInt(xa);
		dest.writeInt(dnd);
	}

}
