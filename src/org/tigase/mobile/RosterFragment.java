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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

public class RosterFragment extends Fragment {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public static RosterFragment newInstance() {
		RosterFragment f = new RosterFragment();
		return f;
	}

	private Cursor c;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	public RosterFragment() {
		super();
		if (DEBUG)
			Log.d(TAG + "_rf", "RosterFragment()");

		this.connectorListener = new Listener<ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				updateConnectionStatus();
			}
		};
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");
		View layout = inflater.inflate(R.layout.roster_list, null);
		this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, null, null);
		final RosterAdapter adapter = new RosterAdapter(inflater.getContext(), R.layout.roster_item, c);

		final ListView lv = (ListView) layout.findViewById(R.id.rosterList);
		lv.setAdapter(adapter);
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

		this.connectionStatus = (ImageView) layout.findViewById(R.id.connection_status);

		if (DEBUG)
			Log.d(TAG + "_rf", "layout created");

		return layout;
	}

	@Override
	public void onDestroyView() {
		if (c != null) {
			if (DEBUG)
				Log.d(TAG, "Closing cursor");
			c.close();
		}
		super.onDestroyView();
		if (DEBUG)
			Log.d(TAG + "_rf", "onDestroyView()");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG + "_rf", "onResume()");
	}

	@Override
	public void onStart() {
		super.onStart();
		XmppService.jaxmpp().addListener(Connector.StateChanged, this.connectorListener);
		updateConnectionStatus();

		if (DEBUG)
			Log.d(TAG + "_rf", "onStart() " + getView());
	}

	@Override
	public void onStop() {
		XmppService.jaxmpp().removeListener(Connector.StateChanged, this.connectorListener);
		super.onStop();

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (DEBUG)
			Log.d(TAG + "_rf", "onViewCreated()");
	}

	private void updateConnectionStatus() {
		final Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();
		if (DEBUG)
			Log.i(TAG, "State changed to " + st);

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
