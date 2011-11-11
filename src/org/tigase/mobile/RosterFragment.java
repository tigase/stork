package org.tigase.mobile;

import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;

public class RosterFragment extends Fragment {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	static final int TOKEN_CHILD = 1;

	static final int TOKEN_GROUP = 0;

	public static RosterFragment newInstance() {
		RosterFragment f = new RosterFragment();
		return f;
	}

	private Cursor c;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	private ExpandableListView listView;

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
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.contactDetails: {
			Intent intent = new Intent(getActivity().getApplicationContext(), VCardViewActivity.class);
			intent.putExtra("itemId", info.id);
			this.startActivityForResult(intent, 0);

			return true;
		}
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			MenuInflater m = new MenuInflater(getActivity());
			m.inflate(R.menu.roster_context_menu, menu);
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");
		View layout = inflater.inflate(R.layout.roster_list, null);
		this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.GROUP_URI), null, null, null, null);
		RosterAdapter.staticContext = inflater.getContext();
		final RosterAdapter adapter = new RosterAdapter(inflater.getContext(), c);

		listView = (ExpandableListView) layout.findViewById(R.id.rosterList);

		// listView.setSaveEnabled(true);
		listView.setAdapter(adapter);
		registerForContextMenu(listView);

		listView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

				Log.i(TAG, "Clicked on id=" + id);

				Intent intent = new Intent();
				intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
				intent.putExtra("id", id);

				getActivity().getApplicationContext().sendBroadcast(intent);
				return true;
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		super.onStart();
		XmppService.jaxmpp(getActivity()).addListener(Connector.StateChanged, this.connectorListener);
		updateConnectionStatus();

		if (DEBUG)
			Log.d(TAG + "_rf", "onStart() " + getView());
	}

	@Override
	public void onStop() {
		XmppService.jaxmpp(getActivity()).removeListener(Connector.StateChanged, this.connectorListener);
		super.onStop();

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (DEBUG)
			Log.d(TAG + "_rf", "onViewCreated()");
	}

	private void updateConnectionStatus() {
		final Connector.State st = XmppService.jaxmpp(getActivity()).getConnector() == null ? State.disconnected
				: XmppService.jaxmpp(getActivity()).getConnector().getState();
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
