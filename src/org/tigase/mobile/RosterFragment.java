package org.tigase.mobile;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

public class RosterFragment extends Fragment {

	public static RosterFragment newInstance(RosterAdapter adapter, OnItemClickListener listener) {
		RosterFragment f = new RosterFragment();
		f.adapter = adapter;
		f.clickListener = listener;
		return f;
	}

	private RosterAdapter adapter;

	private OnItemClickListener clickListener;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	public RosterFragment() {
		this.connectorListener = new Listener<ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				updateConnectionStatus();
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.roster_list, null);

		ListView lv = (ListView) layout.findViewById(R.id.rosterList);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(clickListener);

		this.connectionStatus = (ImageView) layout.findViewById(R.id.connection_status);

		return layout;
	}

	@Override
	public void onStart() {
		XmppService.jaxmpp().addListener(Connector.StateChanged, this.connectorListener);
		super.onStart();
		updateConnectionStatus();
	}

	@Override
	public void onStop() {
		XmppService.jaxmpp().removeListener(Connector.StateChanged, this.connectorListener);
		super.onStop();
	}

	private void updateConnectionStatus() {
		final Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();
		Log.i(TigaseMobileMessengerActivity.LOG_TAG, "State changed to " + st);

		connectionStatus.post(new Runnable() {

			@Override
			public void run() {
				if (st == State.connected) {
					connectionStatus.setImageResource(R.drawable.user_available);
				} else if (st == State.disconnected) {
					connectionStatus.setImageResource(R.drawable.user_offline);
				} else {
					connectionStatus.setImageResource(R.drawable.user_extended_away);
				}
			}
		});

	}

}
