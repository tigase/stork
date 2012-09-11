package org.tigase.mobile.roster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.db.GeolocationTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.pubsub.GeolocationModule;
import org.tigase.mobile.vcard.VCardViewActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class RosterFragment extends Fragment {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	static final int TOKEN_CHILD = 1;

	static final int TOKEN_GROUP = 0;

	private static long extractId(ContextMenuInfo menuInfo) {
		if (menuInfo instanceof ExpandableListContextMenuInfo) {
			int type = ExpandableListView.getPackedPositionType(((ExpandableListContextMenuInfo) menuInfo).packedPosition);
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				return ((ExpandableListContextMenuInfo) menuInfo).id;
			} else
				return -1;
		} else if (menuInfo instanceof AdapterContextMenuInfo) {
			return ((AdapterContextMenuInfo) menuInfo).id;
		} else {
			return -1;
		}
	}

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

	private static boolean isEmpty(Object x) {
		if (x == null)
			return true;
		else if (x.toString().length() == 0)
			return true;
		else
			return false;
	}

	private static boolean isSessionEstablished(SessionObject o) {
		return o != null && o.getProperty(Connector.CONNECTOR_STAGE_KEY) == State.connected
				&& o.getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
	}

	public static RosterFragment newInstance(String layout) {
		RosterFragment f = new RosterFragment();

		Bundle args = new Bundle();
		args.putString("layout", layout);
		f.setArguments(args);

		return f;
	}

	private static long[] toLongArray(List<Long> list) {
		long[] ret = new long[list.size()];
		int i = 0;
		for (Long e : list)
			ret[i++] = e.longValue();
		return ret;
	}

	private Object adapter;

	private Listener<ResourceBindEvent> bindListener;

	private Cursor c;

	private ImageView connectionStatus;

	private final Listener<ConnectorEvent> connectorListener;

	private long[] expandedIds;

	private ContextMenuInfo lastMenuInfo;

	private AbsListView listView;

	private ProgressBar progressBar;

	private String rosterLayout;

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
		if (listView instanceof ExpandableListView && adapter != null) {
			int length = ((GroupsRosterAdapter) adapter).getGroupCount();
			ArrayList<Long> expandedIds = new ArrayList<Long>();
			for (int i = 0; i < length; i++) {
				if (((ExpandableListView) listView).isGroupExpanded(i)) {
					expandedIds.add(((GroupsRosterAdapter) adapter).getGroupId(i));
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
			if (!cursor.moveToNext())
				return null;
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
		long lastId = extractId(lastMenuInfo);

		if (item.getItemId() == R.id.contactDetails) {
			Long infoId = extractId(item.getMenuInfo());
			Intent intent = new Intent(getActivity().getApplicationContext(), VCardViewActivity.class);
			intent.putExtra("itemId", infoId);
			this.startActivityForResult(intent, 0);
			return true;
		} else if (item.getItemId() == R.id.contactEdit) {
			Long infoId = extractId(item.getMenuInfo());
			Intent intent = new Intent(getActivity().getApplicationContext(), ContactEditActivity.class);
			intent.putExtra("itemId", infoId);
			this.startActivityForResult(intent, 0);
			return true;
		} else if (item.getItemId() == R.id.contactRemove) {
			Long infoId = extractId(item.getMenuInfo());
			DialogFragment newFragment = ContactRemoveDialog.newInstance(infoId);
			newFragment.show(getFragmentManager(), "dialog");
			return true;
		} else if (item.getItemId() == R.id.contactAuthorization) {
			this.lastMenuInfo = item.getMenuInfo();
			return true;
		} else if (item.getItemId() == R.id.contactAuthResend) {
			sendAuthResend(lastId);
			return true;
		} else if (item.getItemId() == R.id.contactAuthRerequest) {
			sendAuthRerequest(lastId);
			return true;
		} else if (item.getItemId() == R.id.contactAuthRemove) {
			sendAuthRemove(lastId);
			return true;
		} else
			return super.onContextItemSelected(item);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			this.rosterLayout = getArguments().getString("layout");
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		final Long id = extractId(menuInfo);

		if (id != null && id != -1) {
			RosterItem r = getJid(id);
			final boolean sessionEstablished = r != null && isSessionEstablished(r.getSessionObject());

			MenuInflater m = new MenuInflater(getActivity());
			try {
				Presence p = r.getSessionObject().getPresence().getBestPresence(r.getJid());
				if (p != null && p.getType() == null) {
					SubMenu sm = menu.addSubMenu(R.id.contactsOnlineGroup, Menu.NONE, Menu.NONE, "Chat with…");
					prepareResources(sm, id);
				}
			} catch (Exception e) {
			}
			m.inflate(R.menu.roster_context_menu, menu);

			JaxmppCore jaxmpp = this.getMulti().get(r.getSessionObject());
			if (jaxmpp != null && sessionEstablished) {
				GeolocationModule module = jaxmpp.getModule(GeolocationModule.class);
				if (module != null) {
					ContentValues location = module.getLocationForJid(r.getJid());
					if (location != null) {
						Double lat = location.getAsDouble(GeolocationTableMetaData.FIELD_LAT);
						Double lon = location.getAsDouble(GeolocationTableMetaData.FIELD_LON);
						String uriStr = null;
						if (lon == null || lat == null) {
							String str = "";
							String val = location.getAsString(GeolocationTableMetaData.FIELD_STREET);
							Log.v(TAG, "Street = " + String.valueOf(val));
							if (val != null) {
								str += val;
							}
							val = location.getAsString(GeolocationTableMetaData.FIELD_LOCALITY);
							Log.v(TAG, "Locality = " + String.valueOf(val));
							if (val != null) {
								if (!isEmpty(str)) {
									str += " ";
								}
								str += val;
							}
							val = location.getAsString(GeolocationTableMetaData.FIELD_COUNTRY);
							Log.v(TAG, "Country = " + String.valueOf(val));
							if (val != null) {
								if (!isEmpty(str)) {
									str += " ";
								}
								str += val;
							}

							if (!isEmpty(str)) {
								str = str.replace(' ', '+');
								uriStr = "geo:0,0?q=" + str;
							}
						} else {
							Log.v(TAG, "latitude = " + String.valueOf(lat));
							Log.v(TAG, "longitude = " + String.valueOf(lon));
							uriStr = "geo:" + String.valueOf(lat) + "," + String.valueOf(lon) + "?z=14";
						}
						if (uriStr != null) {
							Log.v(TAG, "created geolocation uri = " + uriStr);
							Uri uri = Uri.parse(uriStr);
							MenuItem item = menu.add(R.string.geolocation_show);
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							item.setIntent(intent);
						}
					}
				}
			}

			menu.setGroupVisible(R.id.contactsOnlineGroup, sessionEstablished);
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");

		if (getArguments() != null) {
			this.rosterLayout = getArguments().getString("layout");
		}

		View layout;
		if ("groups".equals(this.rosterLayout)) {
			layout = inflater.inflate(R.layout.roster_list, null);
		} else if ("flat".equals(this.rosterLayout)) {
			layout = inflater.inflate(R.layout.roster_list_flat, null);
		} else if ("grid".equals(this.rosterLayout)) {
			layout = inflater.inflate(R.layout.roster_list_grid, null);
		} else {
			throw new RuntimeException("Unknown roster layout");
		}

		listView = (AbsListView) layout.findViewById(R.id.rosterList);
		listView.setTextFilterEnabled(true);
		registerForContextMenu(listView);

		if (listView instanceof ExpandableListView) {
			if (c != null) {
				getActivity().stopManagingCursor(c);
			}
			this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.GROUP_URI), null, null, null,
					null);
			getActivity().startManagingCursor(c);
			GroupsRosterAdapter.staticContext = inflater.getContext();

			this.adapter = new GroupsRosterAdapter(inflater.getContext(), c);

			((ExpandableListView) listView).setAdapter((ExpandableListAdapter) adapter);
			((ExpandableListView) listView).setOnChildClickListener(new OnChildClickListener() {

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
		} else if (listView instanceof ListView || listView instanceof GridView) {
			if (c != null) {
				getActivity().stopManagingCursor(c);
			}
			this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, null,
					null);

			getActivity().startManagingCursor(c);
			// FlatRosterAdapter.staticContext = inflater.getContext();

			if (listView instanceof ListView) {
				this.adapter = new FlatRosterAdapter(inflater.getContext(), c, R.layout.roster_item);
				((ListView) listView).setAdapter((ListAdapter) adapter);
			} else if (listView instanceof GridView) {
				this.adapter = new FlatRosterAdapter(inflater.getContext(), c, R.layout.roster_grid_item);
				((GridView) listView).setAdapter((ListAdapter) adapter);
			}
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.i(TAG, "Clicked on id=" + id);

					Intent intent = new Intent();
					intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
					intent.putExtra("id", id);

					getActivity().getApplicationContext().sendBroadcast(intent);
				}
			});

		}
		// there can be no connection status icon - we have notifications and
		// accounts view in Android >= 3.0
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

	private boolean prepareResources(final SubMenu sm, final long id) {
		final RosterItem rosterItem = getJid(id);
		final Jaxmpp jaxmpp = getMulti().get(rosterItem.getSessionObject());
		Map<String, Presence> all = jaxmpp.getSessionObject().getPresence().getPresences(rosterItem.getJid());

		final CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
		final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);

		boolean added = false;

		try {
			if (all != null)
				for (final Entry<String, Presence> entry : all.entrySet()) {
					if (entry.getValue().getType() != null)
						continue;
					MenuItem i = sm.add(entry.getKey());

					added = true;
					i.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							Intent intent = new Intent();
							intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
							intent.putExtra("id", id);
							intent.putExtra("resource", entry.getKey());

							getActivity().getApplicationContext().sendBroadcast(intent);

							return true;
						}
					});
				}
		} catch (Exception e) {
			Log.e(TAG, "Problem on resources menu", e);
		}

		// if (!added) {
		// Intent intent = new Intent();
		// intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
		// intent.putExtra("id", id);
		//
		// getActivity().getApplicationContext().sendBroadcast(intent);
		// }
		return added;
	}

	private void restoreExpandedState(long[] expandedIds) {
		if (listView instanceof ExpandableListView) {
			this.expandedIds = expandedIds;
			if (expandedIds != null && adapter != null) {
				for (int i = 0; i < ((GroupsRosterAdapter) adapter).getGroupCount(); i++) {
					long id = ((GroupsRosterAdapter) adapter).getGroupId(i);
					if (inArray(expandedIds, id))
						((ExpandableListView) listView).expandGroup(i);
				}
			}
		}
	}

	private void sendAuthRemove(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
				rosterItem.getSessionObject());
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).unsubscribed(jid);
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't remove auth", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
				rosterItem.getSessionObject(), jid.getBareJid());
		String txt = String.format(getActivity().getString(R.string.auth_removed), name, jid.getBareJid().toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
	}

	private void sendAuthRerequest(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = getMulti().get(rosterItem.getSessionObject());

		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).subscribe(jid);
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't rerequest subscription", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
				rosterItem.getSessionObject(), jid.getBareJid());
		String txt = String.format(getActivity().getString(R.string.auth_rerequested), name, jid.getBareJid().toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();

	}

	private void sendAuthResend(long id) {
		final RosterItem rosterItem = getJid(id);
		final JID jid = JID.jidInstance(rosterItem.getJid());
		final Jaxmpp jaxmpp = getMulti().get(rosterItem.getSessionObject());

		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).subscribed(jid);
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't resend subscription", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = (new RosterDisplayTools(getActivity().getApplicationContext())).getDisplayName(
				rosterItem.getSessionObject(), jid.getBareJid());
		String txt = String.format(getActivity().getString(R.string.auth_resent), name, jid.getBareJid().toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();

	}

	private void updateConnectionStatus() {
		int onlineCount = 0;
		int offlineCount = 0;
		int connectingCount = 0;
		int disabledCount = 0;
		for (JaxmppCore jaxmpp : getMulti().get()) {
			State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
			boolean established = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
			if (isDisabled(jaxmpp.getSessionObject()))
				++disabledCount;
			else if (state == State.connected && established)
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

		// there can be no connection status icon - we have notifications and
		// accounts view in Android >= 3.0
		if (connectionStatus != null) {
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

}
