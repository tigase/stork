package org.tigase.messenger.phone.pro.conenctionStatus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.EventListener;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class ConnectionStatusesFragment
		extends Fragment {

	RecyclerView recyclerView;
	private StatusesRecyclerViewAdapter adapter;
	private MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			adapter.setMultiJaxmpp(getService().getMultiJaxmpp());
			getService().getMultiJaxmpp().addListener(listener);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getService().getMultiJaxmpp().remove(listener);
			super.onServiceDisconnected(name);
			adapter.setMultiJaxmpp(null);
		}
	};
	private OnListFragmentInteractionListener mListener = new OnListFragmentInteractionListener() {
		@Override
		public void onPingServer(String accountJID) {
			Log.d("Status", "Ping " + accountJID);
			if (mConnection == null) {
				showInfo("Service is not started");
				return;
			}
			final BareJID jid = BareJID.bareJIDInstance(accountJID);
			Jaxmpp j = mConnection.getService().getJaxmpp(jid);
			if (j == null) {
				showInfo("Not connected with " + jid.getDomain());
				return;
			}

			final PingModule pm = j.getModule(PingModule.class);
			(new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {

					try {
						pm.ping(JID.jidInstance(jid.getDomain()), new PingModule.PingAsyncCallback() {
							@Override
							public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									throws JaxmppException {
								showInfo("Pong error from " + jid + ": " + error);
							}

							@Override
							protected void onPong(long time) {
								showInfo("Pong from " + jid + ": " + time + " ms");
							}

							@Override
							public void onTimeout() throws JaxmppException {
								showInfo("Pong error from " + jid + " timeouted");

							}
						});
					} catch (JaxmppException e) {
						showInfo("Ping error: " + e.getMessage());
					}
					return null;
				}
			}).execute();
		}
	};
	private Runnable refreshRun = new Runnable() {
		@Override
		public void run() {
			adapter.notifyDataSetChanged();
		}
	};
	private final EventListener listener = new EventListener() {
		@Override
		public void onEvent(Event<? extends EventHandler> event) {
			refresh();
		}
	};

	public static ConnectionStatusesFragment newInstance() {
		ConnectionStatusesFragment fragment = new ConnectionStatusesFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_connectionstatus_list, container, false);

		recyclerView = (RecyclerView) root.findViewById(R.id.servers_list);

		// Set the adapter
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

		this.adapter = new StatusesRecyclerViewAdapter(this.mListener);
		recyclerView.setAdapter(adapter);

		if (mConnection != null && mConnection.getService() != null) {
			adapter.setMultiJaxmpp(mConnection.getService().getMultiJaxmpp());
		}

		return root;
	}

	@Override
	public void onDetach() {
		try {
			mConnection.getService().getMultiJaxmpp().remove(listener);
		} catch (NullPointerException e) {
		}
		adapter.setMultiJaxmpp(null);
		recyclerView.setAdapter(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	private void refresh() {
		getView().post(refreshRun);
	}

	private void showInfo(final String text) {
		Log.i("ConnectionStatus", "Toast: " + text);
		final FragmentActivity c = getActivity();
		if (c != null) {
			c.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			Log.i("ConnectionStatus", "Activity emtpy!");
		}
	}

	public interface OnListFragmentInteractionListener {

		void onPingServer(String accountJID);
	}
}
