/*
 * Stork
 * Copyright (C) 2021 Tigase, Inc. (office@tigase.com)
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
package org.tigase.messenger.phone.pro.roster.view;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.github.abdularis.civ.StorkAvatarView;
import org.jetbrains.annotations.NotNull;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.roster.PresenceIconMapper;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionAdapter;
import org.tigase.messenger.phone.pro.roster.multiselect.SelectionViewHolder;
import tigase.jaxmpp.core.client.BareJID;

public class RosterAdapter
		extends SelectionAdapter<RosterAdapter.RosterViewHolder> {

	@NotNull
	@Override
	public RosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_rosteritem, parent, false);
		return new RosterViewHolder(view);
	}

	@Override
	protected Long getKey(Cursor cursor) {
		return (long) cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
	}

	public static class RosterViewHolder
			extends SelectionViewHolder {

		private final StorkAvatarView mContactAvatar;
		private final TextView mContactNameView;
		private final TextView mJidView;
		private String account;
		private int adapterPosition;
		private long id;
		private String jid;

		public RosterViewHolder(@NonNull @NotNull View itemView) {
			super(itemView);
			mJidView = itemView.findViewById(R.id.contact_jid);
			mContactNameView = itemView.findViewById(R.id.contact_display_name);
			mContactAvatar = itemView.findViewById(R.id.contact_avatar);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(itemView.getContext(), "kliknięty", Toast.LENGTH_SHORT).show();
				}
			});
		}

		public String getAccount() {
			return account;
		}

		public long getId() {
			return id;
		}

		public String getJid() {
			return jid;
		}

		public void bind(final int adapterPosition, final Cursor cursor, final boolean selected) {
			this.adapterPosition = adapterPosition;
			this.id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
			this.jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));
			this.account = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
			final String name = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
			int status = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_STATUS));

			mContactNameView.setText(name);
			mJidView.setText(jid);
			mContactAvatar.setJID(BareJID.bareJIDInstance(jid), name, PresenceIconMapper.getPresenceResource(status));

			itemView.setActivated(selected);
		}

	}
}
