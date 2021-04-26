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

package org.tigase.messenger.phone.pro.settings.account;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountProperties;
import org.tigase.messenger.phone.pro.serverfeatures.FeatureItem;
import org.tigase.messenger.phone.pro.serverfeatures.FeaturesProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.settings.AbstractServicePreferencesActivity;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.SERVER_FEATURES_KEY;

public class ServerFeaturesFragment
		extends PreferenceFragmentCompat
		implements AbstractServicePreferencesActivity.XmppServiceAware {

	private static final String TAG = "ServerFeaturesFragment";
	private final FeaturesProvider featuresProvider = new FeaturesProvider();
	private String accountJID;
	private Jaxmpp jaxmpp;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.pref_accounts_list, rootKey);
	}

	public void setAccount(String accountName) {
		this.accountJID = accountName;
	}

	@Override
	public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.accountJID = ((AccountProperties) getActivity()).getAccount().name;
	}

	@Override
	public void setXmppService(XMPPService service) {
		if (accountJID == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Cannot show server features.")
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					})
					.show();
			return;
		}

		Jaxmpp jaxmpp = service.getJaxmpp(accountJID);
		if (jaxmpp == null || !jaxmpp.isConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Client must be connected to server")
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					})
					.show();

			return;
		}
		this.jaxmpp = jaxmpp;
		fill();
	}

	private boolean isOnServerFeaturesList(final FeatureItem item, Collection<String> serverFeatures) {
		if (item.getXmlns().contains("*")) {
			final String t = item.getXmlns().substring(0, item.getXmlns().length() - 1);

			for (String serverFeature : serverFeatures) {
				if (serverFeature.startsWith(t)) {
					return true;
				}
			}

			return false;
		} else {
			return serverFeatures.contains(item.getXmlns());
		}
	}

	private void addFeatures(JaxmppCore jaxmpp, Collection<String> serverFeatures) {
		PreferenceScreen screen = getPreferenceScreen();
		screen.removeAll();

		for (FeatureItem item : featuresProvider.get(getContext())) {
			if (isOnServerFeaturesList(item, serverFeatures)) {
				Preference p = new Preference(getContext());
				p.setIconSpaceReserved(false);
				p.setTitle(item.getXep() + ": " + item.getName());
				p.setSummary(item.getDescription());
				screen.addPreference(p);
			}
		}
	}

	private void fill() {
		if (jaxmpp == null) {
			return;
		}

		if (getPreferenceScreen() == null) {
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
