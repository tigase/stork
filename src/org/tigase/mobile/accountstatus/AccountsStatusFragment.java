package org.tigase.mobile.accountstatus;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AccountsStatusFragment extends Fragment {

	public static AccountsStatusFragment newInstance() {
		AccountsStatusFragment f = new AccountsStatusFragment();

		return f;
	}

	private ArrayAdapter<Jaxmpp> adapter;

	private final Listener<ResourceBindEvent> bindListener = new Listener<ResourceBinderModule.ResourceBindEvent>() {

		@Override
		public void handleEvent(ResourceBindEvent be) throws JaxmppException {
			view.post(new Runnable() {

				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
		}
	};

	private final Listener<ConnectorEvent> connectorListener = new Listener<Connector.ConnectorEvent>() {

		@Override
		public void handleEvent(ConnectorEvent be) throws JaxmppException {
			final MultiJaxmpp multi = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

			// int p = adapter.getPosition((Jaxmpp)
			// multi.get(be.getSessionObject()));

			view.post(new Runnable() {

				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});

		}
	};

	private View view;

	public AccountsStatusFragment() {
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.view = inflater.inflate(R.layout.account_status, null);

		this.adapter = new ArrayAdapter<Jaxmpp>(getActivity().getApplicationContext(), R.layout.account_status_item) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = inflater.inflate(R.layout.account_status_item, null);
				}

				Jaxmpp jaxmpp = getItem(position);

				final TextView accountName = (TextView) v.findViewById(R.id.account_name);
				final TextView accountDescription = (TextView) v.findViewById(R.id.account_item_description);
				final ImageView accountStatus = (ImageView) v.findViewById(R.id.account_status);
				final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.account_status_progress);

				final boolean established = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
				State st = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
				if (st == null)
					st = State.disconnected;
				else if (st == State.connected && !established)
					st = State.connecting;

				String errorMessage = jaxmpp.getSessionObject().getProperty("messenger#error");

				if (st == State.disconnected && !TextUtils.isEmpty(errorMessage)) {
					accountDescription.setText("Error: " + errorMessage);
				} else
					accountDescription.setText("" + st);

				accountName.setText(jaxmpp.getSessionObject().getUserBareJid().toString());

				if (st == State.connected) {
					accountStatus.setImageResource(R.drawable.user_available);
					accountStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else if (st == State.disconnected) {
					accountStatus.setImageResource(R.drawable.user_offline);
					accountStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else {
					accountStatus.setVisibility(View.GONE);
					progressBar.setVisibility(View.VISIBLE);
				}
				return v;
			}

		};

		for (JaxmppCore jaxmpp : ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().get()) {
			adapter.add((Jaxmpp) jaxmpp);
		}

		ListView list = (ListView) view.findViewById(R.id.account_status_list);
		list.setAdapter(adapter);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.addListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.addListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);
	}

	@Override
	public void onStop() {
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.removeListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.removeListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);

		super.onStop();
	}
}
