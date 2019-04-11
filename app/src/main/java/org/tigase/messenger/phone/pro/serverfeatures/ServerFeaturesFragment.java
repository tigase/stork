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

package org.tigase.messenger.phone.pro.serverfeatures;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountProperties;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.SERVER_FEATURES_KEY;

public class ServerFeaturesFragment
		extends Fragment {

	private static final String TAG = "ServerFeaturesFragment";
	private String accountJID;
	private FeaturesAdapter adapter;
	private RecyclerView recyclerView;
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			fill();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			super.onServiceDisconnected(name);
		}
	};

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_serverfeatures, container, false);

		recyclerView = (RecyclerView) root.findViewById(R.id.server_features);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		this.adapter = new FeaturesAdapter(getActivity());
		this.recyclerView.setAdapter(adapter);

		return root;
	}

	@Override
	public void onDetach() {
		recyclerView.setAdapter(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	public void setAccount(String accountName) {
		this.accountJID = accountName;
	}

	private void addFeatures(JaxmppCore jaxmpp, Collection<String> serverFeatures) {
		adapter.addFeatures(serverFeatures);
		try {
			if (jaxmpp != null) {
				adapter.addFeatures(getStreamFeaturesXMLNS(jaxmpp.getSessionObject()));
				if (StreamManagementModule.isStreamManagementAvailable(jaxmpp.getSessionObject())) {
					adapter.addFeature(StreamManagementModule.XMLNS);
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "", e);
		}

		recyclerView.post(() -> adapter.notifyDataSetChanged());
	}

	private void fill() {
		if (accountJID == null) {
			this.accountJID = AccountProperties.getAccountName(getActivity().getIntent());
		}

		if (accountJID == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Cannot show server features.")
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					})
					.show();
			return;
		}

		final JaxmppCore jaxmpp = mConnection.getService().getJaxmpp(accountJID);

		if (jaxmpp == null || !jaxmpp.isConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Client must be connected to server")
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					})
					.show();

			return;
		}

		HashSet<String> serverFeatures = jaxmpp.getSessionObject().getProperty(SERVER_FEATURES_KEY);

		if (serverFeatures == null || serverFeatures.isEmpty()) {
			try {
				jaxmpp.getModule(DiscoveryModule.class)
						.discoverServerFeatures(new DiscoveryModule.DiscoInfoAsyncCallback(null) {
							@Override
							public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									throws JaxmppException {
								Log.w(TAG, "Cannot get server features: " + error);
							}

							@Override
							public void onTimeout() throws JaxmppException {
								Log.w(TAG, "Cannot get server features: timeout");

							}

							@Override
							protected void onInfoReceived(String node, Collection<DiscoveryModule.Identity> identities,
														  Collection<String> features) throws XMLException {
								Log.i(TAG, "Received features");
								getActivity().runOnUiThread(() -> {
									addFeatures(jaxmpp, features);
								});
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Cannot get server features", e);
			}
		} else {
			addFeatures(jaxmpp, serverFeatures);
		}
	}

	private Set<String> getStreamFeaturesXMLNS(final SessionObject sessionObject) {
		try {
			final HashSet<String> result = new HashSet<>();
			final Element features = StreamFeaturesModule.getStreamFeatures(sessionObject);

			for (Element child : features.getChildren()) {
				String x = child.getXMLNS();
				if (x != null) {
					result.add(x);
				}
			}

			return result;
		} catch (Throwable e) {
			return Collections.emptySet();
		}
	}
}
