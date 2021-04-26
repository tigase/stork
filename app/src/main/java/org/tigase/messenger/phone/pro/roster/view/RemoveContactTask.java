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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;

public class RemoveContactTask
		extends AsyncTask<BareJID, Void, Void> {

	private final BareJID account;
	private final Context context;
	private final MainActivity.XMPPServiceConnection mConnection;

	public RemoveContactTask(Context context, MainActivity.XMPPServiceConnection xmppServiceConnection,
							 BareJID account) {
		this.account = account;
		this.mConnection = xmppServiceConnection;
		this.context = context;
	}

	@Override
	protected Void doInBackground(BareJID... jidsToRemove) {
		final XMPPService mService = mConnection.getService();
		if (mService == null) {
			Log.i("RosterItemFragment", "Service is disconnected!!!");
			return null;
		}
		try {
			Jaxmpp jaxmpp = mService.getJaxmpp(account);
			if (jaxmpp.isConnected()) {
				RosterStore store = RosterModule.getRosterStore(jaxmpp.getSessionObject());
				for (BareJID jid : jidsToRemove) {
					store.remove(jid);
				}
			}
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Can't remove contact from roster", e);
			Toast.makeText(context, "ERROR " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		return null;
	}

}
