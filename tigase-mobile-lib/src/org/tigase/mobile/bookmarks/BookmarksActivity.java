package org.tigase.mobile.bookmarks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule.BookmarksAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.muc.JoinMucDialog;

@SuppressLint("NewApi")
public class BookmarksActivity extends FragmentActivity {

	private static final int DIALOG_EDIT_BOOKMARK = 1;

	private class Bookmark {

		public Bookmark() {
			id = UUID.randomUUID().toString();
			autojoin = false;
		}

		protected String id;
		protected BareJID accountJid;
		protected BareJID jid;
		protected String name;
		protected String nick;
		protected String password;
		protected boolean autojoin;
	}

	private ActionMode.Callback mActionModeCallback = null;
	private ActionMode mActionMode = null;

	private class BookmarksAsyncCallbackImpl extends BookmarksAsyncCallback {

		private final BareJID accountJid;
		private final ArrayAdapter<Bookmark> adapter;

		public BookmarksAsyncCallbackImpl(BareJID accountJid, ArrayAdapter<Bookmark> adapter) {
			this.adapter = adapter;
			this.accountJid = accountJid;
		}

		@Override
		public void onBookmarksReceived(final List<Element> bookmarks) {
			if (bookmarks == null) return;
			
			listView.post(new Runnable() {
				public void run() {
					for (Element elem : bookmarks) {
						try {
							if ("conference".equals(elem.getName())) {
								Bookmark bookmark = new Bookmark();
								bookmark.name = elem.getAttribute("name");
								bookmark.accountJid = accountJid;
								bookmark.jid = BareJID.bareJIDInstance(elem.getAttribute("jid"));
								if (elem.getAttribute("autojoin") != null) {
									bookmark.autojoin = Boolean.parseBoolean(elem.getAttribute("autojoin"));
								}
								if (bookmark.name == null) {
									bookmark.name = bookmark.jid.toString();
								}
								for (Element child : elem.getChildren()) {
									if ("nick".equals(child.getName())) {
										bookmark.nick = child.getValue();
									} else if ("password".equals(child.getName())) {
										bookmark.password = child.getValue();
									}
								}
								adapter.add(bookmark);
							}
						} catch (XMLException ex) {
							Log.e(TAG, "exception processing bookmarks", ex);
						}
					}
					adapter.sort(new Comparator<Bookmark>() {

						@Override
						public int compare(Bookmark lhs, Bookmark rhs) {
							if (lhs == null || rhs == null)
								return -1;
							return lhs.name.compareTo(rhs.name);
						}

					});
				}
			});
		}

		@Override
		public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTimeout() throws JaxmppException {
			// TODO Auto-generated method stub

		}
	};

	private static final String TAG = "BookmarksActivity";
	private ArrayAdapter<Bookmark> adapter;

	private ListView listView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.bookmarks_list);

		listView = (ListView) this.findViewById(R.id.bookmarksList);

		adapter = new ArrayAdapter<Bookmark>(this, R.layout.bookmarks_list_item) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = getLayoutInflater().inflate(R.layout.bookmarks_list_item, null);
				}
				TextView titleView = (TextView) v.findViewById(R.id.bookmark_title);
				TextView descriptionView = (TextView) v.findViewById(R.id.bookmark_description);

				Bookmark bookmark = getItem(position);
				titleView.setText(bookmark.name);
				descriptionView.setText("Join " + (bookmark.nick != null ? "as " + bookmark.nick : "") + " to "
						+ bookmark.jid.toString());

				return v;
			}

			// public void notifyDataSetChanged() {
			// sort(new Comparator<Bookmark>() {
			//
			// @Override
			// public int compare(Bookmark lhs, Bookmark rhs) {
			// if (lhs == null || rhs == null)
			// return -1;
			// return lhs.name.compareTo(rhs.name);
			// }
			//
			// });
			//
			// super.notifyDataSetChanged();
			// }
		};
		listView.setAdapter(adapter);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initializeContextActions();
		}

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				final Bookmark bookmark = adapter.getItem(position);

				new Thread() {
					@Override
					public void run() {
						JaxmppCore jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(
								bookmark.accountJid);
						try {
							Room room = jaxmpp.getModulesManager().getModule(MucModule.class).join(bookmark.jid.getLocalpart(),
									bookmark.jid.getDomain().toString(), bookmark.nick, bookmark.password);

							Intent intent = new Intent();
							intent.putExtra("roomId", room.getId());
							setResult(RESULT_OK, intent);
							finish();
						} catch (JaxmppException e) {
							Log.e(TAG, "exception while trying to join room");
						}
					}
				}.start();
			}

		});

		refresh();
	}

	private void refresh() {
		listView.post(new Runnable() {
			@Override
			public void run() {
				adapter.clear();
			}
		});

		MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
		for (final JaxmppCore jaxmpp : multi.get()) {
			if (!jaxmpp.isConnected())
				continue;

			new Thread() {
				@Override
				public void run() {
					BookmarksModule module = jaxmpp.getModulesManager().getModule(BookmarksModule.class);
					if (module == null) {
						module = new BookmarksModule(jaxmpp.getSessionObject(), new PacketWriter() {

							@Override
							public void write(Element stanza) throws JaxmppException {
								jaxmpp.send((Stanza) stanza);
							}

							@Override
							public void write(Element stanza, AsyncCallback asyncCallback) throws JaxmppException {
								jaxmpp.send((Stanza) stanza, asyncCallback);
							}

							@Override
							public void write(Element stanza, Long timeout, AsyncCallback asyncCallback) throws JaxmppException {
								jaxmpp.send((Stanza) stanza, timeout, asyncCallback);
							}

						});
						jaxmpp.getModulesManager().register(module);
					}

					try {
						BookmarksAsyncCallback callback = new BookmarksAsyncCallbackImpl(
								jaxmpp.getSessionObject().getUserBareJid(), adapter);
						module.retrieveBookmarks(callback);
					} catch (JaxmppException e) {
						Log.e(TAG, "exception retrieving bookmarks", e);
					}
				}
			}.start();
		}

		this.registerForContextMenu(listView);
	}

	@TargetApi(11)
	private void initializeContextActions() {

		// mActionModeCallback = new ActionMode.Callback() {
		//
		// // Called when the action mode is created; startActionMode() was
		// called
		// @Override
		// public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		// // Inflate a menu resource providing context menu items
		// MenuInflater inflater = mode.getMenuInflater();
		// inflater.inflate(R.menu.bookmarks_context_menu, menu);
		//
		// mActionMode = mode;
		//
		// return true;
		// }
		//
		// // Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// // may be called multiple times if the mode is invalidated.
		// @Override
		// public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// return false; // Return false if nothing is done
		// }
		//
		// // Called when the user selects a contextual menu item
		// @Override
		// public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// if (item.getItemId() == R.id.edit) {
		// mode.finish(); // Action picked, so close the CAB
		// return true;
		// } else if (item.getItemId() == R.id.remove) {
		// return true;
		// } else {
		// return false;
		// }
		//
		// }
		//
		// // Called when the user exits the action mode
		// @Override
		// public void onDestroyActionMode(ActionMode mode) {
		// mActionMode = null;
		// }
		// };

		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				// long[] ids = listView.getCheckedItemIds();
				SparseBooleanArray selection = listView.getCheckedItemPositions();
				if (item.getItemId() == R.id.edit) {
					for (int pos = 0; pos < adapter.getCount(); pos++) {
						// if (ids[0] == adapter.getItemId(pos)) {
						if (selection.get(pos)) {
							editItem(adapter.getItem(pos));
						}
					}
					mode.finish(); // Action picked, so close the CAB
					return true;
				} else if (item.getItemId() == R.id.remove) {
					List<Bookmark> items = new ArrayList<Bookmark>();
					// for (long id : ids) {
					for (int pos = 0; pos < adapter.getCount(); pos++) {
						if (selection.get(pos)) {
							// if (id == adapter.getItemId(pos)) {
							items.add(adapter.getItem(pos));
						}
					}
					// }
					removeItems(items);
					mode.finish();
					return true;
				} else {
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.bookmarks_context_menu, menu);
				return true;
			}
			
			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				Menu menu = mode.getMenu();
				for (int i = 0; i < menu.size(); i++) {
					MenuItem mi = menu.getItem(i);
					if (mi.getItemId() == R.id.edit) {
						mi.setVisible(listView.getCheckedItemCount() < 2);
					}
				}
			}

		});
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmarks_context_menu, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (item.getItemId() == R.id.edit) {
			Bookmark bookmark = adapter.getItem(info.position);
			editItem(bookmark);
			return true;
		} else if (item.getItemId() == R.id.remove) {
			Bookmark bookmark = adapter.getItem(info.position);
			List<Bookmark> bookmarks = new ArrayList<Bookmark>();
			bookmarks.add(bookmark);
			removeItems(bookmarks);
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add) {
			Bundle data = new Bundle();
			data.putBoolean("editMode", true);
			JoinMucDialog dlg = JoinMucDialog.newInstance(data);
			dlg.show(getSupportFragmentManager(), "dialog");
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmarks_menu, menu);
		return true;
	}
	
	public void editItem(Bookmark bookmark) {
		Bundle data = new Bundle();

		data.putBoolean("editMode", true);

		data.putString("id", bookmark.id);
		data.putString("account", bookmark.accountJid.toString());
		data.putString("room", bookmark.jid.getLocalpart());
		data.putString("server", bookmark.jid.getDomain());
		data.putString("name", bookmark.name);
		data.putString("nick", bookmark.nick);
		data.putString("password", bookmark.password);
		data.putBoolean("autojoin", bookmark.autojoin);

		JoinMucDialog dlg = JoinMucDialog.newInstance(data);
		dlg.show(getSupportFragmentManager(), "dialog");
	}

	public void saveItem(Bundle data) {
		String id = data.getString("id");
		BareJID account = BareJID.bareJIDInstance(data.getString("account"));

		List<Bookmark> bookmarks = new ArrayList<Bookmark>();
		Bookmark bookmark = null;
		for (int i = 0; i < adapter.getCount(); i++) {
			Bookmark item = adapter.getItem(i);
			if (account.equals(item.accountJid)) {
				bookmarks.add(item);

				if (id != null && id.equals(item.id)) {
					bookmark = item;
				}
			}
		}

		if (bookmark == null) {
			bookmark = new Bookmark();
			bookmark.accountJid = account;
			bookmarks.add(bookmark);
		}

		bookmark.jid = BareJID.bareJIDInstance(data.getString("room"), data.getString("server"));
		bookmark.name = data.getString("name");
		bookmark.nick = data.getString("nick");
		bookmark.password = data.getString("password");
		bookmark.autojoin = data.getBoolean("autojoin");

		publishBookmarks(account, bookmarks, new AsyncCallback() {

			@Override
			public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksActivity.this, "Could not remove bookmark", 5).show();
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksActivity.this, "Bookmark saved", 5).show();
			}

			@Override
			public void onTimeout() throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksActivity.this, "Request timed out", 5).show();
			}

		});
	}

	public void removeItems(List<Bookmark> bookmarks) {
		Map<BareJID, List<Bookmark>> map = new HashMap<BareJID, List<Bookmark>>();
		for (int i = 0; i < adapter.getCount(); i++) {
			Bookmark bookmark = adapter.getItem(i);
			List<Bookmark> list = map.get(bookmark.accountJid);
			if (list == null) {
				list = new ArrayList<Bookmark>();
				map.put(bookmark.accountJid, list);
			}

			list.add(bookmark);
		}

		Set<BareJID> changedAccounts = new HashSet<BareJID>();

		for (Bookmark bookmark : bookmarks) {
			List<Bookmark> list = map.get(bookmark.accountJid);
			list.remove(bookmark);
			changedAccounts.add(bookmark.accountJid);
		}

		// need to send changed bookmarks
		for (BareJID accountJID : changedAccounts) {
			publishBookmarks(accountJID, map.get(accountJID), new AsyncCallback() {

				@Override
				public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksActivity.this, "Could not remove bookmark", 5).show();
				}

				@Override
				public void onSuccess(Stanza responseStanza) throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksActivity.this, "Bookmarks removed", 5).show();
				}

				@Override
				public void onTimeout() throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksActivity.this, "Request timed out", 5).show();
				}

			});
		}
	}

	public void publishBookmarks(final BareJID accountJID, final List<Bookmark> bookmarks, final AsyncCallback callback) {
		new Thread() {
			public void run() {

				List<Element> items = new ArrayList<Element>();

				for (Bookmark bookmark : bookmarks) {
					try {
						Element item = new DefaultElement("conference");
						if (bookmark.name != null && bookmark.name.length() > 0) {
							item.setAttribute("name", bookmark.name);
						}
						item.setAttribute("jid", bookmark.jid.toString());
						if (bookmark.autojoin) {
							item.setAttribute("autojoin", "true");
						}
						if (bookmark.nick != null && bookmark.nick.length() > 0) {
							item.addChild(new DefaultElement("nick", bookmark.nick, null));
						}
						if (bookmark.password != null && bookmark.password.length() > 0) {
							item.addChild(new DefaultElement("password", bookmark.password, null));
						}
						items.add(item);
					} catch (XMLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
				BookmarksModule module = multi.get(accountJID).getModulesManager().getModule(BookmarksModule.class);
				try {
					module.publishBookmarks(items, callback);
				} catch (JaxmppException ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}
}