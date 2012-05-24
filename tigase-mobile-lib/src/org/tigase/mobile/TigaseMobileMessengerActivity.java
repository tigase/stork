package org.tigase.mobile;

import java.util.List;

import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.accountstatus.AccountsStatusFragment;
import org.tigase.mobile.authenticator.AuthenticatorActivity;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.filetransfer.AndroidFileTransferUtility;
import org.tigase.mobile.filetransfer.FileTransferUtility;
import org.tigase.mobile.preferences.MessengerPreferenceActivity;
import org.tigase.mobile.roster.AccountSelectorDialogFragment;
import org.tigase.mobile.roster.ContactEditActivity;
import org.tigase.mobile.roster.RosterFragment;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class TigaseMobileMessengerActivity extends FragmentActivity {

	private class RosterClickReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!active)
			// return;
			final long id = intent.getLongExtra("id", -1);
			final String resource = intent.getStringExtra("resource");

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
					openChatWith(i, resource);
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

	public static final String ROSTER_CLICK_MSG = "org.tigase.mobile.ROSTER_CLICK_MSG";

	// private ListView rosterList;

	public static final int SELECT_FOR_SHARE = 2;

	private final static String STATE_CURRENT_PAGE_KEY = "currentPage";

	private static final String TAG = "tigase";

	private MyFragmentPageAdapter adapter;

	private final Listener<BaseEvent> chatListener;

	private int currentPage = -1;

	private SharedPreferences mPreferences;
	private OnSharedPreferenceChangeListener prefChangeListener;

	private final RosterClickReceiver rosterClickReceiver = new RosterClickReceiver();

	private ViewPager viewPager;

	private ViewPager viewRoster;;

	public TigaseMobileMessengerActivity() {
		this.chatListener = new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) throws JaxmppException {
				if (be instanceof MessageEvent)
					onMessageEvent((MessageEvent) be);
			}
		};
		this.prefChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (Preferences.ROSTER_LAYOUT_KEY.equals(key)) {
					((MyFragmentPageAdapter) viewPager.getAdapter()).setRefreshRoster(true);
					viewPager.getAdapter().notifyDataSetChanged();
				}
			}
		};
	}

	protected RosterFragment createRosterFragment(String string) {
		return RosterFragment.newInstance(string);
	}

	protected Integer findChat(final long chatId) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getId() == chatId)
				return i;
		}
		return null;
	}

	protected Integer findChat(final RosterItem rosterItem) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getSessionObject() == rosterItem.getSessionObject()
					&& c.getChat().getJid().getBareJid().equals(rosterItem.getJid()))
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

	protected ChatWrapper getChatByPageIndex(int page) {
		int x = page - (isXLarge() ? 1 : 2);
		if (x < 0)
			return null;
		List<ChatWrapper> chats = getChatList();
		if (x >= chats.size())
			return null;
		return chats.get(x);
	}

	protected List<ChatWrapper> getChatList() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().getChats();
	}

	protected boolean isXLarge() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4;
		// return getResources().getConfiguration().screenLayout >= 0x04 &&
		// Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	private void notifyPageChange(int msg) {
		Intent intent = new Intent();
		intent.setAction(CLIENT_FOCUS_MSG);
		intent.putExtra("page", msg);

		if (msg == -1) {
			((MessengerApplication) getApplication()).getTracker().trackPageView("/off");
		} else if (msg == 0) {
			((MessengerApplication) getApplication()).getTracker().trackPageView("/accountsListPage");
		} else if (msg == 1) {
			((MessengerApplication) getApplication()).getTracker().trackPageView("/rosterPage");
		} else if (msg > 1) {
			((MessengerApplication) getApplication()).getTracker().trackPageView("/chatPage");
			final ChatWrapper chat = getChatByPageIndex(msg);
			if (chat != null && chat.isChat())
				intent.putExtra("chatId", chat.getChat().getId());
			else if (chat != null && chat.isRoom())
				intent.putExtra("roomId", chat.getRoom().getId());
		}

		sendBroadcast(intent);

		updateActionBar();

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
		} else if (requestCode == SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			Uri selected = data.getData();
			String mimetype = data.getType();
			final int p = this.currentPage;
			ChatWrapper chatW = getChatByPageIndex(p);
			Chat chat = chatW.getChat();
			if (chat == null)
				return;
			RosterItem ri = chat.getSessionObject().getRoster().get(chat.getJid().getBareJid());
			JID jid = chat.getJid();
			if (jid.getResource() == null) {
				final Jaxmpp jaxmpp = ((MessengerApplication) TigaseMobileMessengerActivity.this.getApplicationContext()).getMultiJaxmpp().get(
						ri.getSessionObject());
				jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
			}
			if (jid != null) {
				AndroidFileTransferUtility.startFileTransfer(this, ri, chat.getJid(), selected, mimetype);
			}
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
		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.mPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);

		boolean autostart = mPreferences.getBoolean(Preferences.AUTOSTART_KEY, true);
		autostart &= mPreferences.getBoolean(Preferences.SERVICE_ACTIVATED, true);
		if (autostart && !JaxmppService.isServiceActive()) {
			Intent intent = new Intent(this, JaxmppService.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(intent);
		}

		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		String previouslyStartedVersion = mPreferences.getString(Preferences.LAST_STARTED_VERSION, null);
		mPreferences.edit().putString(Preferences.LAST_STARTED_VERSION, getResources().getString(R.string.app_version)).commit();

		if (previouslyStartedVersion == null && (accounts == null || accounts.length == 0)) {
			Intent intent = new Intent(this, AuthenticatorActivity.class);
			intent.putExtra("new", true);
			startActivity(intent);
			// finish();
		}

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.getBoolean("error", false)) {
			bundle.putBoolean("error", false);
			String account = bundle.getString("account");
			String message = bundle.getString("message");

			ErrorDialog newFragment = ErrorDialog.newInstance(account, message);
			newFragment.show(getSupportFragmentManager(), "dialog");
		}

		if (savedInstanceState != null && currentPage == -1) {
			currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE_KEY, -1);
			if (getChatByPageIndex(currentPage) == null)
				currentPage = -1;
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
					return createRosterFragment(mPreferences.getString(Preferences.ROSTER_LAYOUT_KEY, "flat"));
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
					Fragment f = createRosterFragment(mPreferences.getString(Preferences.ROSTER_LAYOUT_KEY, "flat"));
					if (DEBUG)
						Log.d(TAG, "Created roster with FragmentManager " + f.getFragmentManager());
					return f;

				} else {
					int idx = i - (!isXLarge() ? 2 : 1);
					final ChatWrapper wrapper = getChatList().get(idx);
					if (wrapper.isChat()) {
						return ChatHistoryFragment.newInstance(
								wrapper.getChat().getSessionObject().getUserBareJid().toString(), wrapper.getChat().getId(),
								idx);
					} else {
						Room room = wrapper.getRoom();
						// TODO
						return null;
					}
				}
			}

			@Override
			public int getItemPosition(Object object) {
				if (object instanceof AccountsStatusFragment) {
					return 0;
				} else if (refreshRoster && object instanceof RosterFragment) {
					return POSITION_NONE;
				} else if (object instanceof RosterFragment) {
					return 1;
				} else if (object instanceof ChatHistoryFragment) {
					Log.v(TAG, "got chat history fragment");
					Chat chat = ((ChatHistoryFragment) object).getChat();
					if (chat != null) {
						Integer position = findChat(chat.getId());
						if (position != null) {
							if (isXLarge())
								return 1 + position;
							return 2 + position;
						}
					}
					return POSITION_NONE;
				} else {
					return POSITION_NONE;
				}
			}

			@Override
			protected String makeFragmentName(int viewId, int index) {
				viewId = 1;
				if (index == 0)
					return "android:switcher:" + viewId + ":accounts";
				else if (index == 1)
					return "android:switcher:" + viewId + ":roster";
				else {
					ChatWrapper wrapper = getChatByPageIndex(index);
					String name = "android:switcher:" + viewId + ":" + wrapper;
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

			JID jid = JID.jidInstance(uri.getPath().substring(1));
			for (JaxmppCore jaxmpp : ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get()) {
				RosterItem ri = jaxmpp.getRoster().get(jid.getBareJid());
				if (ri != null) {
					openChatWith(ri, jid.getResource());
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

			TextView tos = (TextView) dialog.findViewById(R.id.aboutTermsOfService);
			tos.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.termsOfServiceURL) + "'>"
					+ getResources().getString(R.string.termsOfService) + "</a>"));
			tos.setMovementMethod(LinkMovementMethod.getInstance());

			TextView pp = (TextView) dialog.findViewById(R.id.aboutPrivacyPolicy);
			pp.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.privacyPolicyURL) + "'>"
					+ getResources().getString(R.string.privacyPolicy) + "</a>"));
			pp.setMovementMethod(LinkMovementMethod.getInstance());

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
	protected void onDestroy() {
		this.mPreferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
		super.onDestroy();
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

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()");
	}

	protected void onMessageEvent(final MessageEvent be) {
		Runnable action = new Runnable() {

			@Override
			public void run() {
				if (be.getType() == MessageModule.ChatCreated) {
					viewPager.getAdapter().notifyDataSetChanged();
				} else if (be.getType() == MessageModule.ChatClosed) {
					viewPager.getAdapter().notifyDataSetChanged();
				}
				RosterItem it = be.getChat().getSessionObject().getRoster().get(be.getChat().getJid().getBareJid());
				if (it != null) {
					Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), it.getId());
					getApplicationContext().getContentResolver().notifyChange(insertedItem, null);
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

		updateActionBar();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			viewPager.setCurrentItem(1);
		}
		if (item.getItemId() == R.id.showHideOffline) {
			boolean x = mPreferences.getBoolean(Preferences.SHOW_OFFLINE, Boolean.TRUE);
			Editor editor = mPreferences.edit();
			editor.putBoolean(Preferences.SHOW_OFFLINE, !x);
			editor.commit();
			Uri insertedItem = Uri.parse(RosterProvider.CONTENT_URI);
			getApplicationContext().getContentResolver().notifyChange(insertedItem, null);
			// insertedItem =
			// Uri.parse("content://org.tigase.mobile.db.providers.RosterProvider");
			// getApplicationContext().getContentResolver().notifyChange(insertedItem,
			// null);
		} else if (item.getItemId() == R.id.contactAdd) {
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
				intent.putExtra("account", accounts[0].name);
				startActivityForResult(intent, 0);
			}
		} else if (item.getItemId() == R.id.aboutButton) {
			showDialog(ABOUT_DIALOG);
		} else if (item.getItemId() == R.id.showChatsButton) {
			Intent chatListActivity = new Intent(this, ChatListActivity.class);
			this.startActivityForResult(chatListActivity, REQUEST_CHAT);
		} else if (item.getItemId() == R.id.closeChatButton) {
			final int p = this.currentPage;
			ChatWrapper wrapper = getChatByPageIndex(p);
			if (wrapper.isChat()) {
				Chat chat = wrapper.getChat();
				final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(
						chat.getSessionObject());
				final AbstractChatManager cm = jaxmpp.getModulesManager().getModule(MessageModule.class).getChatManager();
				try {
					cm.close(chat);
					if (DEBUG)
						Log.i(TAG, "Chat with " + chat.getJid() + " (" + chat.getId() + ") closed");
				} catch (JaxmppException e) {
					Log.w(TAG, "Chat close problem!", e);
				}
			} else {
				Room room = wrapper.getRoom();
				final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(
						room.getSessionObject());
				final MucModule cm = jaxmpp.getModulesManager().getModule(MucModule.class);
				try {
					cm.leave(room);
				} catch (JaxmppException e) {
					Log.w(TAG, "Chat close problem!", e);
				}
			}

			viewPager.setCurrentItem(1);
		} else if (item.getItemId() == R.id.shareImageButton) {
			final int p = this.currentPage;
			Chat chat = getChatByPageIndex(p).getChat();
			Log.v(TAG, "share selected for = " + chat.getJid().toString());
			Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			pickerIntent.setType("image/*");
			pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(pickerIntent, SELECT_FOR_SHARE);
		} else if (item.getItemId() == R.id.shareVideoButton) {
			final int p = this.currentPage;
			Chat chat = getChatByPageIndex(p).getChat();
			Log.v(TAG, "share selected for = " + chat.getJid().toString());
			Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			pickerIntent.setType("video/*");
			pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(pickerIntent, SELECT_FOR_SHARE);
		} else if (item.getItemId() == R.id.propertiesButton) {
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
		} else if (item.getItemId() == R.id.disconnectButton) {
			mPreferences.edit().putBoolean(Preferences.SERVICE_ACTIVATED, false).commit();
			stopService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));
		} else if (item.getItemId() == R.id.connectButton) {
			mPreferences.edit().putBoolean(Preferences.SERVICE_ACTIVATED, true).commit();

			Intent intent = new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class);
			intent.putExtra("focused", true);
			startService(intent);
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
		Log.v(TAG, "current page " + currentPage);
		Log.v(TAG, "xlarge = " + isXLarge());
		final boolean serviceActive = JaxmppService.isServiceActive();
		if (currentPage == 0 || currentPage == 1 || isXLarge()) {
			inflater.inflate(R.menu.main_menu, menu);

			MenuItem con = menu.findItem(R.id.connectButton);
			con.setVisible(!serviceActive);

			MenuItem dcon = menu.findItem(R.id.disconnectButton);
			dcon.setVisible(serviceActive);

			MenuItem showOffline = menu.findItem(R.id.showHideOffline);
			showOffline.setCheckable(true);
			showOffline.setChecked(mPreferences.getBoolean(Preferences.SHOW_OFFLINE, Boolean.TRUE));

			MenuItem add = menu.findItem(R.id.contactAdd);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				add.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			}
			add.setVisible(serviceActive);

		}
		if (currentPage > 1 || (currentPage > 0 && serviceActive && isXLarge())) {
			inflater.inflate(R.menu.chat_main_menu, menu);

			// Share button support
			MenuItem share = menu.findItem(R.id.shareButton);
			Chat chat = getChatByPageIndex(this.currentPage).getChat();
			if (chat == null)
				return false;
			final Jaxmpp jaxmpp = ((MessengerApplication) TigaseMobileMessengerActivity.this.getApplicationContext()).getMultiJaxmpp().get(
					chat.getSessionObject());
			try {
				JID jid = chat.getJid();
				boolean visible = false;
				if (jid.getResource() == null) {
					jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
				}
				if (jid != null) {
					visible = FileTransferUtility.resourceContainsFeatures(jaxmpp, chat.getJid(), FileTransferUtility.FEATURES);
				}
				share.setVisible(visible);
			} catch (XMLException e) {
			}

			if (isXLarge())
				menu.findItem(R.id.showChatsButton).setVisible(false);
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume()");

		int tmp = findChatPage(getIntent().getExtras());
		if (tmp != -1) {
			this.currentPage = tmp;
		}

		registerReceiver(rosterClickReceiver, new IntentFilter(ROSTER_CLICK_MSG));

		viewPager.getAdapter().notifyDataSetChanged();

		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();

		multi.addListener(this.chatListener);

		if (currentPage < 0) {
			currentPage = isXLarge() ? 0 : 1;
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
		outState.putInt(STATE_CURRENT_PAGE_KEY, currentPage);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG)
			Log.d(TAG, "onStop()");
	}

	protected void openChatWith(final RosterItem rosterItem, final String resource) {
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
						jaxmpp.createChat(JID.jidInstance(rosterItem.getJid(), resource));
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

	private void updateActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(currentPage != 1 && !isXLarge());

			// Chat c = getChatByPageIndex(currentPage);
			// if (c != null) {
			// actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE,
			// ActionBar.DISPLAY_SHOW_TITLE);
			// String subtitle = "Chat with " +
			// c.getSessionObject().getRoster().get(c.getJid().getBareJid()).getName();
			// actionBar.setSubtitle(subtitle);
			// }
			// else {
			// actionBar.setSubtitle(null);
			// }
		}
	}
}