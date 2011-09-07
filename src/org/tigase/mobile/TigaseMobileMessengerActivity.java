package org.tigase.mobile;

import java.util.ArrayList;
import java.util.List;

import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

public class TigaseMobileMessengerActivity extends FragmentActivity {

	private class RosterClickReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			JID jid = JID.jidInstance(intent.getStringExtra("jid"));
			openChatWith(jid);
		}
	}

	public static final String CLIENT_FOCUS_MSG = "org.tigase.mobile.CLIENT_FOCUS_MSG";

	public static final String LOG_TAG = "tigase";

	public static final String ROSTER_CLICK_MSG = "org.tigase.mobile.ROSTER_CLICK_MSG";

	// private ListView rosterList;

	private final Listener<MessageEvent> chatListener;

	private final ArrayList<Chat> chats = new ArrayList<Chat>();

	private int currentPage;

	private Bundle incomingExtras;

	private RosterClickReceiver rosterClickReceiver;

	private ViewPager viewSwitcher;

	public TigaseMobileMessengerActivity() {
		this.chatListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				onMessageEvent(be);
			}
		};
	}

	protected Integer findChat(final JID jid) {
		List<Chat> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getJid().getBareJid().equals(jid.getBareJid()))
				return i;
		}
		return null;
	}

	protected Integer findChat(final long chatId) {
		List<Chat> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getId() == chatId)
				return i;
		}
		return null;
	}

	protected List<Chat> getChatList() {
		return chats;
		// return
		// XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChatManager().getChats();
	}

	private void notifyPageChange(int msg) {
		Intent intent = new Intent();
		intent.setAction(CLIENT_FOCUS_MSG);
		intent.putExtra("page", msg);

		if (msg > 0) {
			final Chat chat = this.chats.get(msg - 1);
			if (chat != null)
				intent.putExtra("chatId", chat.getId());
		}

		sendBroadcast(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(LOG_TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");
	}

	@Override
	public void onBackPressed() {
		if (currentPage > 0) {
			viewSwitcher.setCurrentItem(0);
		} else
			super.onBackPressed();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			currentPage = savedInstanceState.getInt("currentPage", 0);
		}

		setContentView(R.layout.roster);

		this.viewSwitcher = (ViewPager) findViewById(R.id.viewSwitcher);
		this.viewSwitcher.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// Log.i(TigaseMobileMessengerActivity.LOG_TAG, "PageScrolled: "
				// + position + ", " +
				// positionOffset + ", " + positionOffsetPixels);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				Log.i(TigaseMobileMessengerActivity.LOG_TAG, "PageScrollStateChanged: " + state);
			}

			@Override
			public void onPageSelected(int position) {
				Log.i(TigaseMobileMessengerActivity.LOG_TAG, "PageSelected: " + position);
				currentPage = position;
				notifyPageChange(position);
			}
		});

		this.rosterClickReceiver = new RosterClickReceiver();
		IntentFilter filter = new IntentFilter(ROSTER_CLICK_MSG);
		registerReceiver(rosterClickReceiver, filter);

		viewSwitcher.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return 1 + getChatList().size();
			}

			@Override
			public Fragment getItem(int i) {
				if (i == 0)
					return RosterFragment.newInstance();
				else {
					final Chat chat = getChatList().get(i - 1);
					return ChatHistoryFragment.newInstance(chat.getId());
				}
			}
		});

		// final ArrayAdapter<String> adapter = new
		// ArrayAdapter<String>(getApplicationContext(), R.layout.item, item);
		// adapter.setNotifyOnChange(true);
		// rosterList.setAdapter(adapter);

		if (!XmppService.jaxmpp().isConnected()) {
			getContentResolver().delete(Uri.parse(RosterProvider.PRESENCE_URI), null, null);
		}

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			Log.i(LOG_TAG, "Jest extras! " + extras.getString("jid"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		if (rosterClickReceiver != null)
			unregisterReceiver(rosterClickReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		final Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();

		MenuItem con = menu.findItem(R.id.connectButton);
		con.setVisible(currentPage == 0);
		con.setEnabled(st == null || st == State.disconnected);

		MenuItem dcon = menu.findItem(R.id.disconnectButton);
		dcon.setVisible(currentPage == 0);
		dcon.setEnabled(st == State.connected || st == State.connecting);

		MenuItem setup = menu.findItem(R.id.propertiesButton);
		setup.setVisible(currentPage == 0);

		MenuItem closeChat = menu.findItem(R.id.closeChatButton);
		closeChat.setVisible(currentPage != 0);

		return super.onMenuOpened(featureId, menu);
	}

	protected void onMessageEvent(MessageEvent be) {
		if (be.getType() == MessageModule.ChatCreated) {
			this.chats.add(be.getChat());
		} else if (be.getType() == MessageModule.ChatClosed) {
			this.chats.remove(be.getChat());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(LOG_TAG, "onNewIntent()");
		this.incomingExtras = intent.getExtras();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.closeChatButton:
			final int p = this.currentPage;
			final ChatManager cm = XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChatManager();
			Chat chat = cm.getChats().get(p - 1);
			try {
				cm.close(chat);
			} catch (JaxmppException e) {
			}
			viewSwitcher.setCurrentItem(0);
			break;
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

			XmppService.jaxmpp().getProperties().setUserProperty(SoftwareVersionModule.NAME_KEY, "Tigase Mobile Messenger");
			XmppService.jaxmpp().getProperties().setUserProperty(SoftwareVersionModule.VERSION_KEY,
					getResources().getString(R.string.app_version));
			XmppService.jaxmpp().getProperties().setUserProperty(SoftwareVersionModule.OS_KEY,
					"Android " + android.os.Build.VERSION.RELEASE);

			XmppService.jaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.USER_JID, jid);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.PASSWORD, password);

			startService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));

			// try {
			// XmppService.jaxmpp().login(false);
			// } catch (JaxmppException e) {
			// Log.e(TigaseMobileMessengerActivity.LOG_TAG, "Can't connect", e);
			// Toast.makeText(getApplicationContext(), "Connection error!",
			// Toast.LENGTH_LONG).show();
			// }
		default:
			break;
		}
		return true;
	}

	@Override
	protected void onPause() {
		XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).removeListener(this.chatListener);
		notifyPageChange(-1);
		// TODO Auto-generated method stub
		super.onPause();
	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	//
	// Log.i(LOG_TAG, "Sprawdzamy extrasy...");
	// if (getIntent() != null && getIntent().getExtras() != null) {
	// Log.i(LOG_TAG, "Mamy extrasy");
	// if (getIntent().getExtras().containsKey("chatId")) {
	// long chatId = getIntent().getLongExtra("chatId", -1);
	// getIntent().removeExtra("chatId");
	//
	// List<Chat> l = getChatList();
	// for (int i = 0; i < l.size(); i++) {
	// Chat chh = l.get(i);
	// if (chh.getId() == chatId) {
	// final int x = i + 1;
	// viewSwitcher.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// Log.i(LOG_TAG, "Switch chats to " + x);
	//
	// viewSwitcher.setCurrentItem(x);
	// }
	// });
	//
	// }
	// }
	// }
	//
	// }
	// }

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume()");

		chats.clear();
		chats.addAll(XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChatManager().getChats());

		XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).addListener(this.chatListener);

		if (incomingExtras != null) {
			String s_jid = incomingExtras.getString("jid");
			long chatId = incomingExtras.getLong("chatId", -1);
			incomingExtras = null;
			Log.d(LOG_TAG, "Intent with data? chatId=" + chatId);
			if (s_jid != null && chatId != -1) {
				final Integer idx = findChat(chatId);
				if (idx != null) {
					currentPage = idx + 1;
					Log.d(LOG_TAG, "Set current page " + currentPage);
				}
			}
		}
		viewSwitcher.post(new Runnable() {

			@Override
			public void run() {
				Log.d(LOG_TAG, "Focus on page " + currentPage);
				viewSwitcher.setCurrentItem(currentPage);
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("currentPage", currentPage);
		super.onSaveInstanceState(outState);
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

}