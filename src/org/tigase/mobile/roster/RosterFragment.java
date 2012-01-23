package org.tigase.mobile.roster;

import java.util.ArrayList;
import java.util.List;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.vcard.VCardViewActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import android.widget.ProgressBar;
import android.widget.Toast;

public class RosterFragment extends Fragment {

	private static final boolean DEBUG = true;

	private static final String TAG = "tigase";

	static final int TOKEN_CHILD = 1;

	static final int TOKEN_GROUP = 0;

	private static boolean inArray(long[] array, long element) {
		for (long l : array) {
			if (l == element) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDisabled(SessionObject jaxmpp) {
		Boolean x = jaxmpp.getProperty("CC:DISABLED");
		return x == null ? false : x;
	}

	public static RosterFragment newInstance() {
		RosterFragment f = new RosterFragment();
		return f;
	}

	private static long[] toLongArray(List<Long> list) {
		long[] ret = new long[list.size()];
		int i = 0;
		for (Long e : list)
			ret[i++] = e.longValue();
		return ret;
	}

	private RosterAdapter adapter;

	private Listener<ResourceBindEvent> bindListener;

	private Cursor c;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	private long[] expandedIds;

	private ExpandableListContextMenuInfo lastMenuInfo;

	private ExpandableListView listView;

	private ProgressBar progressBar;

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
		this.bindListener = new Listener<ResourceBindEvent>() {

			@Override
			public void handleEvent(ResourceBindEvent be) throws JaxmppException {
				updateConnectionStatus();
			}
		};
	}

	private long[] getExpandedIds() {
		if (adapter != null) {
			int length = adapter.getGroupCount();
			ArrayList<Long> expandedIds = new ArrayList<Long>();
			for (int i = 0; i < length; i++) {
				if (listView.isGroupExpanded(i)) {
					expandedIds.add(adapter.getGroupId(i));
				}
			}
			return toLongArray(expandedIds);
		} else {
			return null;
		}
	}

	private RosterItem getJid(long itemId) {
		final Cursor cursor = getActivity().getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + itemId),
				null, null, null, null);

		try {
			cursor.moveToNext();
			JID jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
			BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

			if (account != null && jid != null) {
				return getMulti().get(account).getRoster().get(jid.getBareJid());
			}

		} finally {
			cursor.close();
		}
		return null;
	}

	private MultiJaxmpp getMulti() {
		return ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.contactDetails: {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
			Intent intent = new Intent(getActivity().getApplicationContext(), VCardViewActivity.class);
			intent.putExtra("itemId", info.id);
			this.startActivityForResult(intent, 0);
			return true;
		}
		case R.id.contactEdit: {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
			Intent intent = new Intent(getActivity().getApplicationContext(), ContactEditActivity.class);
			intent.putExtra("itemId", info.id);
			this.startActivityForResult(intent, 0);

			return true;
		}
		case R.id.contactRemove: {
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
			DialogFragment newFragment = ContactRemoveDialog.newInstance(info.id);
			newFragment.show(getFragmentManager(), "dialog");
			return true;
		}
		case R.id.contactAuthorization: {
			this.lastMenuInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();
			return true;
		}
		case R.id.contactAuthResend:
			sendAuthResend(lastMenuInfo.id);
			return true;
		case R.id.contactAuthRerequest:
			sendAuthRerequest(lastMenuInfo.id);
			return true;
		case R.id.contactAuthRemove:
			sendAuthRemove(lastMenuInfo.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

		final boolean sessionEstablished = true;

		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			MenuInflater m = new MenuInflater(getActivity());
			m.inflate(R.menu.roster_context_menu, menu);

			menu.setGroupVisible(R.id.contactsOnlineGroup, sessionEstablished);
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");
		View layout = inflater.inflate(R.layout.roster_list, null);
		this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.GROUP_URI), null, null, null, null);
		getActivity().startManagingCursor(c);
		RosterAdapter.staticContext = inflater.getContext();
		this.adapter = new RosterAdapter(inflater.getContext(), c);

		listView = (ExpandableListView) layout.findViewById(R.id.rosterList);
		listView.setSaveEnabled(true);
		listView.setTextFilterEnabled(true);

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
		this.progressBar = (ProgressBar) layout.findViewById(R.id.progressBar1);

		if (DEBUG)
			Log.d(TAG + "_rf", "layout created");

		long[] expandedIds = savedInstanceState == null ? null : savedInstanceState.getLongArray("ExpandedIds");
		if (expandedIds != null) {
			restoreExpandedState(expandedIds);
		}

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

		this.expandedIds = getExpandedIds();
		if (DEBUG)
			Log.d(TAG, "Save roster view state." + (this.expandedIds != null));
		outState.putLongArray("ExpandedIds", this.expandedIds);
	}

	@Override
	public void onStart() {
		super.onStart();
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.addListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.addListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);
		updateConnectionStatus();

		if (DEBUG)
			Log.d(TAG + "_rf", "onStart() " + getView());

		if (this.expandedIds != null) {
			restoreExpandedState(expandedIds);
		}
	}

	@Override
	public void onStop() {
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.removeListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.removeListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);
		super.onStop();

		expandedIds = getExpandedIds();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (DEBUG)
			Log.d(TAG + "_rf", "onViewCreated()");
	}

	private void restoreExpandedState(long[] expandedIds) {
		this.expandedIds = expandedIds;
		if (expandedIds != null) {
			if (adapter != null) {
				for (int i = 0; i < adapter.getGroupCount(); i++) {
					long id = adapter.getGroupId(i);
					if (inArray(expandedIds, id))
						listView.expandGroup(i);
				}
			}
		}
	}

	private void sendAuthRemove(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
				rosterItem.getSessionObject());

		try {
			jaxmpp.getModulesManager().getModule(PresenceModule.class).unsubscribed(jid);
			final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
					rosterItem.getSessionObject(), jid.getBareJid());
			String txt = String.format(getActivity().getString(R.string.auth_removed), name, jid.getBareJid().toString());
			Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
		} catch (JaxmppException e) {
			Log.w(TAG, "Can't remove auth", e);
		}
	}

	private void sendAuthRerequest(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = getMulti().get(rosterItem.getSessionObject());

		try {
			jaxmpp.getModulesManager().getModule(PresenceModule.class).subscribe(jid);
			final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
					rosterItem.getSessionObject(), jid.getBareJid());
			String txt = String.format(getActivity().getString(R.string.auth_rerequested), name, jid.getBareJid().toString());
			Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
		} catch (JaxmppException e) {
			Log.w(TAG, "Can't rerequest subscription", e);
		}
	}

	private void sendAuthResend(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = getMulti().get(rosterItem.getSessionObject());

		try {
			jaxmpp.getModulesManager().getModule(PresenceModule.class).subscribed(jid);
			final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
					rosterItem.getSessionObject(), jid.getBareJid());
			String txt = String.format(getActivity().getString(R.string.auth_resent), name, jid.getBareJid().toString());
			Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
		} catch (JaxmppException e) {
			Log.w(TAG, "Can't resend subscription", e);
		}
	}

	private void updateConnectionStatus() {
		int onlineCount = 0;
		int offlineCount = 0;
		int connectingCount = 0;
		int disabledCount = 0;
		for (JaxmppCore jaxmpp : getMulti().get()) {
			State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
			if (isDisabled(jaxmpp.getSessionObject()))
				++disabledCount;
			else if (state == State.connected)
				++onlineCount;
			else if (state == null || state == State.disconnected)
				++offlineCount;
			else
				++connectingCount;
		}
		final State st;
		if (connectingCount > 0)
			st = State.connecting;
		else if (onlineCount == 0)
			st = State.disconnected;
		else
			st = State.connected;

		connectionStatus.post(new Runnable() {

			@Override
			public void run() {
				if (st == State.connected) {
					connectionStatus.setImageResource(R.drawable.user_available);
					connectionStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else if (st == State.disconnected) {
					connectionStatus.setImageResource(R.drawable.user_offline);
					connectionStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else {
					connectionStatus.setVisibility(View.GONE);
					progressBar.setVisibility(View.VISIBLE);
				}
			}
		});

	}

}
