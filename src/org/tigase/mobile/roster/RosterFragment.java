package org.tigase.mobile.roster;

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
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.MultiJaxmpp;
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

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	static final int TOKEN_CHILD = 1;

	static final int TOKEN_GROUP = 0;

	public static RosterFragment newInstance() {
		RosterFragment f = new RosterFragment();
		return f;
	}

	private Listener<ResourceBindEvent> bindListener;

	private Cursor c;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

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
		RosterAdapter.staticContext = inflater.getContext();
		final RosterAdapter adapter = new RosterAdapter(inflater.getContext(), c);

		listView = (ExpandableListView) layout.findViewById(R.id.rosterList);
		listView.setTextFilterEnabled(true);

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
		this.progressBar = (ProgressBar) layout.findViewById(R.id.progressBar1);

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
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.addListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.addListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);
		updateConnectionStatus();

		if (DEBUG)
			Log.d(TAG + "_rf", "onStart() " + getView());
	}

	@Override
	public void onStop() {
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.removeListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.removeListener(ResourceBinderModule.ResourceBindSuccess, this.bindListener);
		super.onStop();

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (DEBUG)
			Log.d(TAG + "_rf", "onViewCreated()");
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
	}

}
