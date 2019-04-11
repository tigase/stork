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

package org.tigase.messenger.phone.pro.conversations.muc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;

import java.util.ArrayList;

public class JoinMucActivity
		extends AppCompatActivity {

	private final ArrayList<BareJID> accountsList = new ArrayList<>();

	private Spinner mAccountSelector;
	private EditText mNickname;
	private EditText mRoomJid;
	private XMPPService mService;
	private ArrayAdapter<BareJID> sa;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			XMPPService.LocalBinder binder = (XMPPService.LocalBinder) service;
			mService = binder.getService();

			sa.clear();
			for (JaxmppCore j : mService.getMultiJaxmpp().get()) {
				sa.add(j.getSessionObject().getUserBareJid());
			}

			fillNickname(0);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_muc);

		mAccountSelector = (Spinner) findViewById(R.id.contact_account);
		mRoomJid = (EditText) findViewById(R.id.join_room_jid);
		mNickname = (EditText) findViewById(R.id.join_room_nickname);

		Button contactAddButton = (Button) findViewById(R.id.contact_add_button);
		contactAddButton.setOnClickListener(view -> onClickJoin());

		this.sa = new ArrayAdapter<>(getBaseContext(), R.layout.account_list_item, R.id.account_name, accountsList);
		mAccountSelector.setAdapter(sa);
		mAccountSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				fillNickname(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent service = new Intent(getApplicationContext(), XMPPService.class);
		bindService(service, mServiceConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mServiceConnection);
	}

	private void fillNickname(int i) {
		if (i < this.accountsList.size()) {
			BareJID aJid = accountsList.get(i);
			SessionObject so = mService.getJaxmpp(aJid).getSessionObject();
			String nn = so.getProperty(SessionObject.NICKNAME);
			mNickname.setText(nn == null ? aJid.getLocalpart() : nn);
		}
	}

	private void onClickJoin() {
		if (mAccountSelector.getSelectedItem() == null) {
			return;
		}
		final BareJID account = BareJID.bareJIDInstance(mAccountSelector.getSelectedItem().toString());
		final BareJID jid = BareJID.bareJIDInstance(mRoomJid.getText().toString());
		final String nickname = mNickname.getText().toString();

		if (jid.getLocalpart() == null || jid.getDomain() == null || jid.getLocalpart().trim().isEmpty() ||
				jid.getDomain().trim().isEmpty()) {
			new AlertDialog.Builder(JoinMucActivity.this).setMessage(
					"Invalid room address, please check and try again.")
					.setPositiveButton(android.R.string.ok, null)
					.show();
			return;
		}

		(new JoinToRoomTask(account, jid, nickname)).execute();

		(new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				final MucModule mucModule = mService.getJaxmpp(account).getModule(MucModule.class);
				try {
					mucModule.join(jid.getLocalpart(), jid.getDomain(), nickname);
				} catch (JaxmppException e) {
					Log.e("JoinMuc", "Can't join to MUC", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				JoinMucActivity.this.finish();
			}
		}).execute();

	}

	private class JoinToRoomTask
			extends AsyncTask<Void, Void, Boolean> {

		private final Jaxmpp jaxmpp;

		public JoinToRoomTask(BareJID account, BareJID jid, String nickname) {
			this.jaxmpp = mService.getJaxmpp(account);
			// this.jaxmpp.getEventBus().addHandler(MucModule.YouJoinedHandler);
			// this.jaxmpp.getEventBus().addHandler(MucModule.JoinRequestedHandler);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			return null;
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
		}

		@Override
		protected void onPreExecute() {

			super.onPreExecute();
		}
	}

}
