/*
 * MyRosterItemRecyclerViewAdapter.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.roster;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.CursorRecyclerViewAdapter;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;

public class MyRosterItemRecyclerViewAdapter extends CursorRecyclerViewAdapter<ViewHolder> {

    private final RosterItemFragment.OnRosterItemIteractionListener mListener;

    private final RosterItemFragment.OnRosterItemDeleteListener mLongClickListener;

    public MyRosterItemRecyclerViewAdapter(Context context, Cursor cursor,
                                           RosterItemFragment.OnRosterItemIteractionListener mListener,
                                           RosterItemFragment.OnRosterItemDeleteListener mRemoveListener) {
        super(cursor);
        this.mListener = mListener;
        this.mLongClickListener = mRemoveListener;
    }

    @Override
    public void onBindViewHolderCursor(final ViewHolder holder, Cursor cursor) {
        final int id = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ID));
        final String jid = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_JID));
        final String account = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_ACCOUNT));
        final String name = cursor.getString(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
        int status = cursor.getInt(cursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_STATUS));

        holder.mContactNameView.setText(name);

        int presenceIconResource;
        switch (status) {
            case CPresence.OFFLINE:
                presenceIconResource = android.R.drawable.presence_invisible;
                break;
            case CPresence.ERROR:
                presenceIconResource = android.R.drawable.presence_offline;
                break;
            case CPresence.DND:
                presenceIconResource = android.R.drawable.presence_busy;
                break;
            case CPresence.XA:
                presenceIconResource = android.R.drawable.presence_away;
                break;
            case CPresence.AWAY:
                presenceIconResource = android.R.drawable.presence_away;
                break;
            case CPresence.ONLINE:
                presenceIconResource = android.R.drawable.presence_online;
                break;
            case CPresence.CHAT: // chat
                presenceIconResource = android.R.drawable.presence_online;
                break;
            default:
                presenceIconResource = android.R.drawable.presence_offline;
        }

        holder.mContactPresence.setImageResource(presenceIconResource);
        holder.mJidView.setText(jid);


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(id, account, jid);
                }
            }
        });
        holder.setContextMenu(R.menu.roster_context, new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_roster_delete) {
                    mLongClickListener.onRosterItemDelete(id, account, jid, name);
                    return true;
                } else
                    return false;
            }
        });
        AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(jid), holder.mContactAvatar);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_rosteritem, parent, false);
        return new ViewHolder(view);
    }

}
