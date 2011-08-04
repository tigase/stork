package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.db.MessengerDatabaseHelper;
import org.tigase.mobile.db.providers.AbstractRosterProvider;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class TigaseMobileMessengerActivity extends FragmentActivity {

	// private ListView rosterList;

	private int currentPage;

	// @Override
	// public boolean onTouchEvent(MotionEvent touchevent) {
	// switch (touchevent.getAction()) {
	// case MotionEvent.ACTION_DOWN: {
	// oldTouchValue = touchevent.getX();
	// break;
	// }
	//
	// case MotionEvent.ACTION_UP: {
	//
	// float currentX = touchevent.getX();
	// if (oldTouchValue < currentX) {
	// // viewSwitcher.setInAnimation(inFromLeftAnimation());
	// // viewSwitcher.setOutAnimation(outToRightAnimation());
	// // viewSwitcher.showNext();
	// }
	// if (oldTouchValue > currentX) {
	// // viewSwitcher.setInAnimation(inFromRightAnimation());
	// // viewSwitcher.setOutAnimation(outToLeftAnimation());
	// // viewSwitcher.showPrevious();
	// }
	// break;
	// }
	// }
	// return false;
	// }

	private View l;

	private NotificationManager notificationManager;

	private float oldTouchValue;

	private ViewPager viewSwitcher;

	protected Integer findChat(final JID jid) {
		List<Chat> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getJid().getBareJid().equals(jid.getBareJid()))
				return i;
		}
		return null;
	}

	protected List<Chat> getChatList() {
		return XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChatManager().getChats();
	}

	protected Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeft.setDuration(500);
		inFromLeft.setInterpolator(new AccelerateDecelerateInterpolator());
		return inFromLeft;
	}

	protected Animation inFromRightAnimation() {

		Animation inFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(500);
		inFromRight.setInterpolator(new AccelerateDecelerateInterpolator());
		return inFromRight;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.roster);

		this.viewSwitcher = (ViewPager) findViewById(R.id.viewSwitcher);
		this.viewSwitcher.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// Log.i("X", "PageScrolled: " + position + ", " +
				// positionOffset + ", " + positionOffsetPixels);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				Log.i("X", "PageScrollStateChanged: " + state);
			}

			@Override
			public void onPageSelected(int position) {
				Log.i("X", "PageSelected: " + position);
				currentPage = position;
			}
		});

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// this.rosterList = (ListView) l.findViewById(R.id.rosterList);
		//
		final OnItemClickListener listener = new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				ListView rosterList = (ListView) parent.findViewById(R.id.rosterList);

				CursorWrapper cw = (CursorWrapper) rosterList.getItemAtPosition(position);

				JID jid = JID.jidInstance(cw.getString(1));

				openChatWith(jid);

				// Intent i = new Intent(TigaseMobileMessengerActivity.this,
				// ChatActivity.class);
				// startActivity(i);
			}
		};

		Cursor c = getContentResolver().query(Uri.parse(AbstractRosterProvider.CONTENT_URI), null, null, null, null);
		startManagingCursor(c);
		final RosterAdapter adapter = new RosterAdapter(this, R.layout.roster_item, c);

		viewSwitcher.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return 1 + getChatList().size();
			}

			@Override
			public Fragment getItem(int i) {
				if (i == 0)
					return RosterFragment.newInstance(adapter, listener);
				else {
					final Chat chat = getChatList().get(i - 1);
					final Cursor c = getContentResolver().query(
							Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid()), null, null, null, null);

					startManagingCursor(c);

					ChatAdapter ad = new ChatAdapter(TigaseMobileMessengerActivity.this, R.layout.chat_item, c);

					MessengerDatabaseHelper db = new MessengerDatabaseHelper(getApplicationContext());
					db.open();
					return ChatFragment.newInstance(chat, ad, db);
				}
			}
		});

		// final ArrayAdapter<String> adapter = new
		// ArrayAdapter<String>(getApplicationContext(), R.layout.item, item);
		// adapter.setNotifyOnChange(true);
		// rosterList.setAdapter(adapter);

		if (!XmppService.jaxmpp().isConnected()) {
			MessengerDatabaseHelper h = new MessengerDatabaseHelper(getApplicationContext());
			h.open();
			h.makeAllOffline();
			h.close();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		MenuItem con = menu.findItem(R.id.connectButton);
		MenuItem dcon = menu.findItem(R.id.disconnectButton);

		Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();

		con.setEnabled(st == State.disconnected);
		dcon.setEnabled(st == State.connected || st == State.connecting);

		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.propertiesButton:
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			break;
		case R.id.disconnectButton:
			stopService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));
			break;
		case R.id.connectButton:
			// Toast.makeText(getApplicationContext(), "Connecting...",
			// Toast.LENGTH_LONG).show();

			SharedPreferences prefs = getSharedPreferences("org.tigase.mobile_preferences", 0);
			JID jid = JID.jidInstance(prefs.getString("user_jid", null));
			String password = prefs.getString("user_password", null);
			String hostname = prefs.getString("hostname", null);

			XmppService.jaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.USER_JID, jid);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.PASSWORD, password);

			startService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));

			// try {
			// XmppService.jaxmpp().login(false);
			// } catch (JaxmppException e) {
			// Log.e("messenger", "Can't connect", e);
			// Toast.makeText(getApplicationContext(), "Connection error!",
			// Toast.LENGTH_LONG).show();
			// }
		default:
			break;
		}
		return true;
	}

	protected void openChatWith(final JID jid) {
		try {
			Integer idx = findChat(jid);

			if (idx == null) {
				XmppService.jaxmpp().createChat(jid);
				viewSwitcher.setCurrentItem(getChatList().size());
			} else {
				viewSwitcher.setCurrentItem(idx + 1);
			}

		} catch (JaxmppException e) {
			throw new RuntimeException(e);
		}
	}

	protected Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoLeft.setDuration(500);
		outtoLeft.setInterpolator(new AccelerateDecelerateInterpolator());
		return outtoLeft;
	}

	protected Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(500);
		outtoRight.setInterpolator(new AccelerateDecelerateInterpolator());
		return outtoRight;
	}

}