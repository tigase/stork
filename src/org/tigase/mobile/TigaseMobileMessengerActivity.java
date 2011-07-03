package org.tigase.mobile;

import java.util.ArrayList;
import java.util.List;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule.RosterEvent;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TigaseMobileMessengerActivity extends Activity {

	private List<String> item = new ArrayList<String>();

	private ListView rosterList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.rosterList = (ListView) findViewById(R.id.rosterList);

		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.item, item);
		adapter.setNotifyOnChange(true);
		rosterList.setAdapter(adapter);

		XmppService.jaxmpp().getModulesManager().getModule(RosterModule.class).addListener(RosterModule.ItemAdded,
				new Listener<RosterModule.RosterEvent>() {

					@Override
					public synchronized void handleEvent(final RosterEvent be) throws JaxmppException {
						System.out.println(" +++++++ " + be.getItem().getJid().toString());
						// item.add(be.getItem().getJid().toString());

						rosterList.post(new Runnable() {

							@Override
							public void run() {
								adapter.add(be.getItem().getJid().toString());
							}
						});

					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.propertiesButton:
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			break;
		case R.id.connectButton:
			Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_LONG).show();

			SharedPreferences prefs = getSharedPreferences("org.tigase.mobile_preferences", 0);
			JID jid = JID.jidInstance(prefs.getString("user_jid", null));
			String password = prefs.getString("user_password", null);
			String hostname = prefs.getString("hostname", null);

			XmppService.jaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.USER_JID, jid);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.PASSWORD, password);

			try {
				XmppService.jaxmpp().login(false);
			} catch (JaxmppException e) {
				Log.e("messenger", "Can't connect", e);
				Toast.makeText(getApplicationContext(), "Connection error!", Toast.LENGTH_LONG).show();
			}
		default:
			break;
		}
		return true;
	}
}