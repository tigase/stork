package org.tigase.messenger.phone.pro.conenctionStatus;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;

import java.util.ArrayList;

public class StatusesRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

	private final ConnectionStatusesFragment.OnListFragmentInteractionListener listener;
	private MultiJaxmpp multiJaxmpp;
	private ArrayList<JaxmppCore> jaxmpps;

	public StatusesRecyclerViewAdapter(ConnectionStatusesFragment.OnListFragmentInteractionListener mListener) {
		this.listener = mListener;
	}

	@Override
	public int getItemCount() {
		if (jaxmpps == null)
			return 0;
		return jaxmpps.size();
	}

	public MultiJaxmpp getMultiJaxmpp() {
		return multiJaxmpp;
	}

	public void setMultiJaxmpp(MultiJaxmpp multiJaxmpp) {
		this.multiJaxmpp = multiJaxmpp;
		this.jaxmpps = multiJaxmpp == null ? null : new ArrayList<>(multiJaxmpp.get());
		notifyDataSetChanged();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		final JaxmppCore j = this.jaxmpps.get(position);
		if (j == null) {
			holder.mServerName.setText("?");
			holder.mStage.setText("?");
			holder.mConnected.setText("?");
			holder.mResumption.setText("?");
		} else {
			holder.mServerName.setText(j.getSessionObject().getUserBareJid().toString());
			holder.mStage.setText("Stage: " + j.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY));
			holder.mConnected.setText("Connected: " + j.isConnected());
			holder.mResumption.setText(
					"Session resumption: " + StreamManagementModule.isResumptionEnabled(j.getSessionObject()));

			PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (item.getItemId() == R.id.menu_connectionstatus_ping) {
						listener.onPingServer(j.getSessionObject().getUserBareJid().toString());
						return true;
					} else
						return false;
				}
			};

			holder.setContextMenu(R.menu.connection_status_context, menuListener);
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_connectionstatusitem, parent, false);
		return new ViewHolder(view);
	}

}
