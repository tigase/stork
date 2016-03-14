package org.tigase.messenger.phone.pro.openchats;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.tigase.messenger.phone.pro.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ViewHolder extends RecyclerView.ViewHolder {


    @Bind(R.id.id)
    TextView mIdView;

    @Bind(R.id.content)
    TextView mContentView;

    public ViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }


    @Override
    public String toString() {
        return super.toString() + " '" + mContentView.getText() + "'";
    }
}
