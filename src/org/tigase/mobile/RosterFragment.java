package org.tigase.mobile;

import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

public class RosterFragment extends MyListFragment {

	public static RosterFragment newInstance() {
		RosterFragment f = new RosterFragment();
		return f;
	}

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	private Cursor c;

	public RosterFragment() {
		super(R.id.rosterList);
		this.connectorListener = new Listener<ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				updateConnectionStatus();
			}
		};
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.c = getActivity().getApplicationContext().getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null,
				null, null, null);
		final RosterAdapter adapter = new RosterAdapter(getActivity().getApplicationContext(), R.layout.roster_item, c);
		final ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				CursorWrapper cw = (CursorWrapper) lv.getItemAtPosition(position);

				final String jid = cw.getString(1);

				Intent intent = new Intent();
				intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
				intent.putExtra("jid", jid);

				getActivity().getApplicationContext().sendBroadcast(intent);
			}
		});
		setListAdapter(adapter);

	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View layout = inflater.inflate(R.layout.roster_list, null);

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
		if (c != null) {
			Log.d(TigaseMobileMessengerActivity.LOG_TAG, "Closing cursor");
			c.close();
		}

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
