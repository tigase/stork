package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.accountstatus.AccountsStatusFragment;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.preferences.MessengerPreferenceActivity;
import org.tigase.mobile.roster.AccountSelectorDialogFragment;
import org.tigase.mobile.roster.ContactEditActivity;
import org.tigase.mobile.roster.RosterFragment;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TigaseMobileMessengerActivity extends FragmentActivity {

	private class RosterClickReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!active)
			// return;
			final long id = intent.getLongExtra("id", -1);

			final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + id), null, null,
					null, null);
			JID jid = null;
			BareJID account = null;
			try {
				cursor.moveToNext();
				jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
				account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

			} finally {
				cursor.close();
			}

			final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);

			for (RosterItem i : jaxmpp.getRoster().getAll()) {
				if (id == i.getId()) {
					openChatWith(i);
					break;
				}
			}

		}
	}

	public final static int ABOUT_DIALOG = 1;

	public static final String CLIENT_FOCUS_MSG = "org.tigase.mobile.CLIENT_FOCUS_MSG";

	public final static int CONTACT_REMOVE_DIALOG = 2;

	private static final boolean DEBUG = true;

	public static final int REQUEST_CHAT = 1;

	// private ListView rosterList;

	public static final String ROSTER_CLICK_MSG = "org.tigase.mobile.ROSTER_CLICK_MSG";

	private static final String TAG = "tigase";

	private MyFragmentPageAdapter adapter;

	private final Listener<BaseEvent> chatListener;

	private int currentPage = -1;

	private final RosterClickReceiver rosterClickReceiver = new RosterClickReceiver();

	private ViewPager viewPager;
	private ViewPager viewRoster;

	public TigaseMobileMessengerActivity() {
		this.chatListener = new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) throws JaxmppException {
				if (be instanceof MessageEvent)
					onMessageEvent((MessageEvent) be);
			}
		};
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

	protected Integer findChat(final RosterItem rosterItem) {
		List<Chat> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getSessionObject() == rosterItem.getSessionObject() && c.getJid().getBareJid().equals(rosterItem.getJid()))
				return i;
		}
		return null;
	}

	private int findChatPage(Bundle incomingExtras) {
		if (incomingExtras != null) {
			String s_jid = incomingExtras.getString("jid");
			long chatId = incomingExtras.getLong("chatId", -1);
			incomingExtras = null;
			if (DEBUG)
				Log.d(TAG, "Intent with data? chatId=" + chatId);
			if (s_jid != null && chatId != -1) {
				final Integer idx = findChat(chatId);
				if (idx != null) {
					int currentPage = idx + (isXLarge() ? 1 : 2);
					if (DEBUG)
						Log.d(TAG, "Set current page " + currentPage);
					return currentPage;
				}
			}
		}
		return -1;
	}

	protected List<Chat> getChatList() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().getChats();
	}

	protected boolean isXLarge() {
		return false;
		// return getResources().getConfiguration().screenLayout >= 0x04 &&
		// Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	private void notifyPageChange(int msg) {
		Intent intent = new Intent();
		intent.setAction(CLIENT_FOCUS_MSG);
		intent.putExtra("page", msg);

		if (msg > 1) {
			final Chat chat = getChatList().get(msg - (isXLarge() ? 1 : 2));
			if (chat != null)
				intent.putExtra("chatId", chat.getId());
		}

		sendBroadcast(intent);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d(TAG, "onActivityResult()");
		if (requestCode == REQUEST_CHAT && resultCode == Activity.RESULT_OK) {
			this.currentPage = findChatPage(data.getExtras());
		}
	}

	@Override
	public void onBackPressed() {
		if (currentPage == 0 || !isXLarge() && currentPage > 1) {
			viewPager.setCurrentItem(1);
		} else
			super.onBackPressed();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.getBoolean("error", false)) {
			bundle.putBoolean("error", false);
			String account = bundle.getString("account");
			String message = bundle.getString("message");

			ErrorDialog newFragment = ErrorDialog.newInstance(account, message);
			newFragment.show(getSupportFragmentManager(), "dialog");
		}

		if (savedInstanceState != null && currentPage == -1) {
			currentPage = savedInstanceState.getInt("currentPage", -1);
		}
		if (isXLarge()) {
			setContentView(R.layout.all);
		} else {
			setContentView(R.layout.roster);
		}

		this.viewRoster = (ViewPager) findViewById(R.id.viewRoster);
		this.viewPager = (ViewPager) findViewById(R.id.viewSwitcher);
		this.viewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// if(DEBUG)Log.i(TigaseMobileMessengerActivity.TAG,
				// "PageScrolled: "
				// + position + ", " +
				// positionOffset + ", " + positionOffsetPixels);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				if (DEBUG)
					Log.i(TigaseMobileMessengerActivity.TAG, "PageScrollStateChanged: " + state);
			}

			@Override
			public void onPageSelected(int position) {
				if (DEBUG)
					Log.i(TigaseMobileMessengerActivity.TAG, "PageSelected: " + position);
				currentPage = position;
				notifyPageChange(position);
			}
		});

		if (viewRoster != null) {
			viewRoster.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
				@Override
				public int getCount() {
					// TODO Auto-generated method stub
					return 1;
				}

				@Override
				public Fragment getItem(int i) {
					return RosterFragment.newInstance();
				}
			});
		}

		this.adapter = new MyFragmentPageAdapter(getSupportFragmentManager()) {

			@Override
			public int getCount() {
				if (isXLarge())
					return 1 + getChatList().size();
				int n = 2 + getChatList().size();
				return n;
			}

			@Override
			public Fragment getItem(int i) {
				if (DEBUG)
					Log.i(TAG, "FragmentPagerAdapter.getItem(" + i + ")");
				if (i == 0) {
					return AccountsStatusFragment.newInstance();
				} else if (!isXLarge() && i == 1) {
					Fragment f = RosterFragment.newInstance();
					if (DEBUG)
						Log.d(TAG, "Created roster with FragmentManager " + f.getFragmentManager());
					return f;

				} else {
					final Chat chat = getChatList().get(i - (!isXLarge() ? 2 : 1));
					return ChatHistoryFragment.newInstance(chat.getSessionObject().getUserBareJid().toString(), chat.getId());
				}
			}

			@Override
			public int getItemPosition(Object object) {
				if (DEBUG)
					Log.i(TAG, "FragmentPagerAdapter.getItemPosition()");
				return -1000;
			}

			@Override
			protected String makeFragmentName(int viewId, int index) {
				if (index == 0)
					return "android:switcher:" + viewId + ":accounts";
				else if (index == 1)
					return "android:switcher:" + viewId + ":roster";
				else {
					Chat chat = getChatList().get(index - 2);
					String name = "android:switcher:" + viewId + ":" + chat;
					if (DEBUG)
						Log.d(TAG, "Chat page name: " + name);
					return name;
				}
			}
		};

		viewPager.setAdapter(this.adapter);

		if (getIntent().getData() instanceof Uri) {
			Uri uri = getIntent().getData();
			if (DEBUG)
				Log.d(TAG, "onCreate(" + uri + ")");

			BareJID jid = BareJID.bareJIDInstance(uri.getPath().substring(1));
			for (JaxmppCore jaxmpp : ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get()) {
				RosterItem ri = jaxmpp.getRoster().get(jid);
				if (ri != null) {
					openChatWith(ri);
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONTACT_REMOVE_DIALOG:
			return null;
		case ABOUT_DIALOG: {

			final Dialog dialog = new Dialog(this);
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);

			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle(getString(R.string.aboutButton));

			Button okButton = (Button) dialog.findViewById(R.id.okButton);
			okButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		default:
			return null;
		}
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()");
	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	//
	// if(DEBUG)Log.i(TAG, "Sprawdzamy extrasy...");
	// if (getIntent() != null && getIntent().getExtras() != null) {
	// if(DEBUG)Log.i(TAG, "Mamy extrasy");
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
	// if(DEBUG)Log.i(TAG, "Switch chats to " + x);
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

	protected void onMessageEvent(final MessageEvent be) {
		Runnable action = new Runnable() {

			@Override
			public void run() {
				if (be.getType() == MessageModule.ChatCreated) {
					viewPager.getAdapter().notifyDataSetChanged();
				} else if (be.getType() == MessageModule.ChatClosed) {
					viewPager.getAdapter().notifyDataSetChanged();
				}
			}
		};

		viewPager.post(action);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (DEBUG)
			Log.d(TAG, "onNewIntent()");
		this.currentPage = findChatPage(intent.getExtras());

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.contactAdd: {
			AccountManager accountManager = AccountManager.get(this);
			final Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

			if (accounts != null && accounts.length > 1) {
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				Fragment prev = getSupportFragmentManager().findFragmentByTag("account:select:dialog");
				if (prev != null) {
					ft.remove(prev);
				}
				ft.addToBackStack(null);
				AccountSelectorDialogFragment newFragment = AccountSelectorDialogFragment.newInstance();
				newFragment.show(ft, "account:select:dialog");
			} else if (accounts != null && accounts.length == 1) {
				Intent intent = new Intent(this, ContactEditActivity.class);
				intent.putExtra("account", accounts[0]);
				startActivityForResult(intent, 0);
			}
			break;
		}
		case R.id.aboutButton: {
			showDialog(ABOUT_DIALOG);
			break;
		}
		case R.id.showChatsButton: {
			Intent chatListActivity = new Intent(this, ChatListActivity.class);
			this.startActivityForResult(chatListActivity, REQUEST_CHAT);
			break;
		}
		case R.id.closeChatButton: {
			final int p = this.currentPage;
			Chat chat = getChatList().get(p - (isXLarge() ? 1 : 2));
			final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(chat.getSessionObject());
			final AbstractChatManager cm = jaxmpp.getModulesManager().getModule(MessageModule.class).getChatManager();
			try {
				cm.close(chat);
				if (DEBUG)
					Log.i(TAG, "Chat with " + chat.getJid() + " (" + chat.getId() + ") closed");
			} catch (JaxmppException e) {
				Log.w(TAG, "Chat close problem!", e);
			}
			viewPager.setCurrentItem(1);
			break;
		}
		case R.id.propertiesButton: {
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			break;
		}
		case R.id.disconnectButton: {
			stopService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));
			break;
		}
		case R.id.connectButton: {
			Intent intent = new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class);
			intent.putExtra("focused", true);
			startService(intent);
		}
		default:
			break;
		}
		return true;
	}

	@Override
	protected void onPause() {
		unregisterReceiver(rosterClickReceiver);
		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
		multi.removeListener(this.chatListener);
		notifyPageChange(-1);
		// TODO Auto-generated method stub
		super.onPause();
		if (DEBUG)
			Log.d(TAG, "onPause()");

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		if (currentPage == 0 || currentPage == 1) {
			inflater.inflate(R.menu.main_menu, menu);

			final boolean serviceActive = JaxmppService.isServiceActive();

			MenuItem con = menu.findItem(R.id.connectButton);
			con.setVisible(!serviceActive);

			MenuItem dcon = menu.findItem(R.id.disconnectButton);
			dcon.setVisible(serviceActive);

			MenuItem add = menu.findItem(R.id.contactAdd);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				add.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			}
			add.setVisible(serviceActive);

		} else {
			inflater.inflate(R.menu.chat_main_menu, menu);
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume()");

		registerReceiver(rosterClickReceiver, new IntentFilter(ROSTER_CLICK_MSG));

		viewPager.getAdapter().notifyDataSetChanged();

		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();

		multi.addListener(this.chatListener);

		if (currentPage < 0) {
			currentPage = 1;
		}
		viewPager.post(new Runnable() {

			@Override
			public void run() {
				if (DEBUG)
					Log.d(TAG, "Focus on page " + currentPage);
				viewPager.setCurrentItem(currentPage);
				notifyPageChange(currentPage);
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("currentPage", currentPage);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG)
			Log.d(TAG, "onStop()");
	}

	protected void openChatWith(final RosterItem rosterItem) {
		Runnable r = new Runnable() {

			@Override
			public void run() {

				try {
					Integer idx = findChat(rosterItem);

					if (DEBUG)
						Log.i(TAG, "Opening new chat with " + rosterItem + ". idx=" + idx);

					if (idx == null) {
						final Jaxmpp jaxmpp = ((MessengerApplication) TigaseMobileMessengerActivity.this.getApplicationContext()).getMultiJaxmpp().get(
								rosterItem.getSessionObject());
						jaxmpp.createChat(JID.jidInstance(rosterItem.getJid()));
						int i = getChatList().size() + (isXLarge() ? 1 : 2);
						viewPager.setCurrentItem(i);
					} else {
						viewPager.setCurrentItem(idx + (isXLarge() ? 1 : 2));
					}

				} catch (JaxmppException e) {
					throw new RuntimeException(e);
				}

			}
		};
		viewPager.postDelayed(r, 750);
	}
}