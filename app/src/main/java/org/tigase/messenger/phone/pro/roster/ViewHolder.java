package org.tigase.messenger.phone.pro.roster;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tigase.messenger.phone.pro.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.contact_jid)
    TextView mJidView;

    @Bind(R.id.contact_display_name)
    TextView mContactNameView;

    @Bind(R.id.contact_presence)
    ImageView mContactPresence;

    @Bind(R.id.contact_avatar)
    ImageView mContactAvatar;


    public ViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public String toString() {
        return super.toString() + " '" + mContactNameView.getText() + "'";
    }
}
