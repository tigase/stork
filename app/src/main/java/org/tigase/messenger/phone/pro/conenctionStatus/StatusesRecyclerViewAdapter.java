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

package org.tigase.messenger.phone.pro.conenctionStatus;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.SettingsActivity;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslMechanism;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;

import java.util.ArrayList;

public class StatusesRecyclerViewAdapter
		extends RecyclerView.Adapter<ViewHolder> {

	private final Context context;
	private final ConnectionStatusesFragment.OnListFragmentInteractionListener listener;
	private final AccountManager mAccountManager;
	private ArrayList<JaxmppCore> jaxmpps;
	private MultiJaxmpp multiJaxmpp;

	public StatusesRecyclerViewAdapter(Context context,
									   ConnectionStatusesFragment.OnListFragmentInteractionListener mListener) {
		this.listener = mListener;
		this.context = context;

		this.mAccountManager = AccountManager.get(context);
	}

	@Override
	public int getItemCount() {
		if (jaxmpps == null) {
			return 0;
		}
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
		final Account account = AccountHelper.getAccount(mAccountManager,
														 j.getSessionObject().getUserBareJid().toString());
		if (j == null) {
			holder.mServerName.setText("?");
			holder.mStage.setText("?");
			holder.mConnected.setText("?");
			holder.mResumption.setText("?");
		} else {
			holder.mServerName.setText(j.getSessionObject().getUserBareJid().toString());

			Connector.State state = j.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
			if (state == Connector.State.disconnected) {
				holder.mStage.setText(SettingsActivity.getDisconnectedCauseMessage(context,
																				   getDisconectionProblemDescription(
																						   account)));
			} else {
				holder.mStage.setText("" + state);
			}
		}

		holder.mConnected.setText("" + j.isConnected());
		holder.mResumption.setText("" + StreamManagementModule.isResumptionEnabled(j.getSessionObject()));

		Boolean authorized = j.getSessionObject().getProperty(AuthModule.AUTHORIZED);
		SaslMechanism saslMech = j.getSessionObject().getProperty(SaslModule.SASL_MECHANISM);

		String x = (authorized != null && authorized) ? "authorized" : "not authorized";
		if (saslMech != null) {
			x += " (" + saslMech.name() + ")";
		}
		holder.mAuthSatus.setText(x);

		Boolean streamEncrypted = j.getSessionObject().getProperty(SocketConnector.ENCRYPTED_KEY);
		holder.mTlsStatus.setText("" + (streamEncrypted != null && streamEncrypted));
		Boolean streamCompressed = j.getSessionObject().getProperty(SocketConnector.COMPRESSED_KEY);
		holder.mZlibStatus.setText("" + (streamCompressed != null && streamCompressed));

		JID bindedJid = ResourceBinderModule.getBindedJID(j.getSessionObject());
		holder.mSessionBind.setText(bindedJid == null ? "-" : bindedJid.toString());

		PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.menu_connectionstatus_republishomemo) {
					listener.onRepublishOMEMO(j.getSessionObject().getUserBareJid().toString());
					return true;
				} else if (item.getItemId() == R.id.menu_connectionstatus_serverfeatures) {
					listener.onServerFeatures(j.getSessionObject().getUserBareJid().toString());
					return true;
				} else if (item.getItemId() == R.id.menu_connectionstatus_ping) {
					listener.onPingServer(j.getSessionObject().getUserBareJid().toString());
					return true;
				} else if (item.getItemId() == R.id.menu_connectionstatus_ack) {
					listener.onAckServer(j.getSessionObject().getUserBareJid().toString());
					return true;
				} else {
					return false;
				}
			}
		};

		holder.setContextMenu(R.menu.connection_status_context, menuListener);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.fragment_connectionstatusitem, parent, false);
		return new ViewHolder(view);
	}

	XMPPService.DisconnectionCauses getDisconectionProblemDescription(Account accout) {
		String tmp = mAccountManager.getUserData(accout, AccountsConstants.DISCONNECTION_CAUSE_KEY);
		if (tmp == null) {
			return null;
		} else {
			return XMPPService.DisconnectionCauses.valueOf(tmp);
		}
	}

}
