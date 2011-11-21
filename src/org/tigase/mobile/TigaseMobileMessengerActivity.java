package org.tigase.mobile;

import java.util.List;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
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

	public abstract class MyFragmentPagerAdapter<T> extends FragmentPagerAdapter {

		private FragmentTransaction mCurTransaction = null;

		private final FragmentManager mFragmentManager;

		public MyFragmentPagerAdapter(FragmentManager fm) {
			super(fm);
			mFragmentManager = fm;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}

			if (DEBUG)
				Log.v(TAG, "Detaching item #" + position + ": f=" + object + " v=" + ((Fragment) object).getView());

			mCurTransaction.detach((Fragment) object);
		}

		@Override
		public void finishUpdate(View container) {
			try {
				if (mCurTransaction != null) {
					mCurTransaction.commit();
					mCurTransaction = null;
					mFragmentManager.executePendingTransactions();
				}
			} catch (IllegalStateException e) {
				Log.e(TAG, "Again?", e);
				mCurTransaction = null;
			}
		}

		@Override
		public int getCount() {
			return getPages().size();
		}

		/**
		 * Return the Fragment associated with a specified position.
		 */
		@Override
		public abstract Fragment getItem(int position);

		@Override
		public int getItemPosition(Object object) {
			return PagerAdapter.POSITION_NONE;
		}

		public T getObject(int i) {
			return getPages().get(i);
		}

		protected abstract List<T> getPages();

		@Override
		public Object instantiateItem(View container, int position) {

			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}

			// Do we already have this fragment?
			String name = makeFragmentName(container.getId(), position);
			Fragment fragment = mFragmentManager.findFragmentByTag(name);

			if (fragment != null) {
				if (DEBUG)
					Log.v(TAG, "Attaching item #" + position + ": f=" + fragment);

				mCurTransaction.attach(fragment);
			} else {
				fragment = getItem(position);
				if (DEBUG)
					Log.v(TAG, "Adding item #" + position + ": f=" + fragment);

				mCurTransaction.add(container.getId(), fragment, makeFragmentName(container.getId(), position));
			}

			return fragment;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return ((Fragment) object).getView() == view;
		}

		protected String makeFragmentName(int viewId, int index) {
			return "android:switcher:" + viewId + ":" + getPages().get(index);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View container) {
		}
	}

	private class RosterClickReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!active)
			// return;
			final long id = intent.getLongExtra("id", -1);

			for (RosterItem i : XmppService.jaxmpp(getApplicationContext()).getRoster().getAll()) {
				if (id == i.getId()) {
					JID jid = JID.jidInstance(i.getJid());
					openChatWith(jid);
					break;
				}
			}

		}
	}

	private final static int ABOUT_DIALOG = 1;

	public static final String CLIENT_FOCUS_MSG = "org.tigase.mobile.CLIENT_FOCUS_MSG";

	private static final boolean DEBUG = false;

	public static final int REQUEST_CHAT = 1;

	// private ListView rosterList;

	public static final String ROSTER_CLICK_MSG = "org.tigase.mobile.ROSTER_CLICK_MSG";

	private static final String TAG = "tigase";

	private PagerAdapter adapter;

	private final Listener<MessageEvent> chatListener;

	private int currentPage;

	private Bundle incomingExtras;

	private final RosterClickReceiver rosterClickReceiver = new RosterClickReceiver();

	private ViewPager viewPager;
	private ViewPager viewRoster;

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
		return XmppService.jaxmpp(this).getModulesManager().getModule(MessageModule.class).getChatManager().getChats();
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

		if (msg > 0) {
			final Chat chat = getChatList().get(msg - (isXLarge() ? 0 : 1));
			if (chat != null)
				intent.putExtra("chatId", chat.getId());
		}

		sendBroadcast(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d(TAG, "onActivityResult()");
		if (requestCode == REQUEST_CHAT && resultCode == Activity.RESULT_OK) {
			this.incomingExtras = data.getExtras();
		}
	}

	@Override
	public void onBackPressed() {
		if (!isXLarge() && currentPage > 0) {
			viewPager.setCurrentItem(0);
		} else
			super.onBackPressed();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG, "onCreate()");

		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			currentPage = savedInstanceState.getInt("currentPage", 0);
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
		this.adapter = new MyFragmentPagerAdapter<Chat>(getSupportFragmentManager()) {

			@Override
			public int getCount() {
				if (isXLarge())
					return getChatList().size();
				int n = 1 + getChatList().size();
				return n;
			}

			@Override
			public Fragment getItem(int i) {
				if (DEBUG)
					Log.i(TAG, "FragmentPagerAdapter.getItem(" + i + ")");
				if (!isXLarge() && i == 0) {
					Fragment f = RosterFragment.newInstance();
					if (DEBUG)
						Log.d(TAG, "Created roster with FragmentManager " + f.getFragmentManager());
					return f;

				} else {
					final Chat chat = getChatList().get(i - (!isXLarge() ? 1 : 0));
					return ChatHistoryFragment.newInstance(chat.getId());
				}
			}

			@Override
			public int getItemPosition(Object object) {
				if (DEBUG)
					Log.i(TAG, "FragmentPagerAdapter.getItemPosition()");
				return -1000;
			}

			@Override
			protected List<Chat> getPages() {
				return getChatList();
			}

			@Override
			protected String makeFragmentName(int viewId, int index) {
				if (index == 0)
					return "android:switcher:" + viewId + ":roster";
				else {
					return "android:switcher:" + viewId + ":" + getPages().get(index - 1).getId();
				}
			}

		};
		viewPager.setAdapter(this.adapter);

		// final ArrayAdapter<String> adapter = new
		// ArrayAdapter<String>(getApplicationContext(), R.layout.item, item);
		// adapter.setNotifyOnChange(true);
		// rosterList.setAdapter(adapter);

		if (!XmppService.jaxmpp(TigaseMobileMessengerActivity.this).isConnected()) {
			// getContentResolver().delete(Uri.parse(RosterProvider.PRESENCE_URI),
			// null, null);
		}

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (DEBUG)
				Log.i(TAG, "Jest extras! " + extras.getString("jid"));
			incomingExtras = extras;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()");
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		final Connector.State st = XmppService.jaxmpp(this).getConnector() == null ? State.disconnected : XmppService.jaxmpp(
				this).getConnector().getState();
		final boolean serviceActive = JaxmppService.isServiceActive();

		MenuItem con = menu.findItem(R.id.connectButton);
		con.setVisible(currentPage == 0);
		con.setEnabled(!serviceActive);

		MenuItem dcon = menu.findItem(R.id.disconnectButton);
		dcon.setVisible(currentPage == 0);
		dcon.setEnabled(serviceActive);

		MenuItem setup = menu.findItem(R.id.propertiesButton);
		setup.setVisible(currentPage == 0);

		MenuItem closeChat = menu.findItem(R.id.closeChatButton);
		closeChat.setVisible(currentPage != 0);

		MenuItem about = menu.findItem(R.id.aboutButton);
		about.setVisible(currentPage == 0);

		return super.onMenuOpened(featureId, menu);
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
		this.incomingExtras = intent.getExtras();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
			final AbstractChatManager cm = XmppService.jaxmpp(this).getModulesManager().getModule(MessageModule.class).getChatManager();
			Chat chat = cm.getChats().get(p - (isXLarge() ? 0 : 1));
			try {
				cm.close(chat);
				if (DEBUG)
					Log.i(TAG, "Chat with " + chat.getJid() + " (" + chat.getId() + ") closed");
			} catch (JaxmppException e) {
				Log.w(TAG, "Chat close problem!", e);
			}
			viewPager.setCurrentItem(0);
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

		XmppService.jaxmpp(this).getModulesManager().getModule(MessageModule.class).removeListener(this.chatListener);
		notifyPageChange(-1);
		// TODO Auto-generated method stub
		super.onPause();
		if (DEBUG)
			Log.d(TAG, "onPause()");

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume()");

		registerReceiver(rosterClickReceiver, new IntentFilter(ROSTER_CLICK_MSG));

		viewPager.getAdapter().notifyDataSetChanged();

		XmppService.jaxmpp(this).getModulesManager().getModule(MessageModule.class).addListener(this.chatListener);

		if (incomingExtras != null) {
			String s_jid = incomingExtras.getString("jid");
			long chatId = incomingExtras.getLong("chatId", -1);
			incomingExtras = null;
			if (DEBUG)
				Log.d(TAG, "Intent with data? chatId=" + chatId);
			if (s_jid != null && chatId != -1) {
				final Integer idx = findChat(chatId);
				if (idx != null) {
					currentPage = idx + (isXLarge() ? 0 : 1);
					if (DEBUG)
						Log.d(TAG, "Set current page " + currentPage);
				}
			}
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

	protected void openChatWith(final JID jid) {
		Runnable r = new Runnable() {

			@Override
			public void run() {

				try {
					Integer idx = findChat(jid);

					if (DEBUG)
						Log.i(TAG, "Opening new chat with " + jid + ". idx=" + idx);

					if (idx == null) {
						XmppService.jaxmpp(TigaseMobileMessengerActivity.this).createChat(jid);
						viewPager.setCurrentItem(getChatList().size() - (isXLarge() ? 1 : 0));
					} else {
						viewPager.setCurrentItem(idx + (isXLarge() ? 0 : 1));
					}

				} catch (JaxmppException e) {
					throw new RuntimeException(e);
				}

			}
		};
		viewPager.postDelayed(r, 750);
	}
}