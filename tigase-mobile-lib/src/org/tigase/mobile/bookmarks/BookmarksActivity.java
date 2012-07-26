package org.tigase.mobile.bookmarks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule.BookmarksAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.tigase.mobile.MessengerApplication;

public class BookmarksActivity extends Activity {

	private class Bookmark {
		protected BareJID accountJid;
		protected BareJID jid;
		protected String name;
		protected String nick;
		protected String password;
	}
	
	private class BookmarksAsyncCallbackImpl extends BookmarksAsyncCallback {

		private final BareJID accountJid;
		private final ArrayAdapter<Bookmark> adapter;

		public BookmarksAsyncCallbackImpl(BareJID accountJid, ArrayAdapter<Bookmark> adapter) {
			this.adapter = adapter;
			this.accountJid = accountJid;
		}

		@Override
		public void onBookmarksReceived(final List<Element> bookmarks) {
			listView.post(new Runnable() {
				public void run() {
					for (Element elem : bookmarks) {
						try {
							if ("conference".equals(elem.getName())) {
								Bookmark bookmark = new Bookmark();
								bookmark.name = elem.getAttribute("name");
								bookmark.accountJid = accountJid;
								bookmark.jid = BareJID.bareJIDInstance(elem.getAttribute("jid"));
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
			
//			public void notifyDataSetChanged() {
//				sort(new Comparator<Bookmark>() {
//
//					@Override
//					public int compare(Bookmark lhs, Bookmark rhs) {
//						if (lhs == null || rhs == null)
//							return -1;
//						return lhs.name.compareTo(rhs.name);
//					}
//
//				});
//				
//				super.notifyDataSetChanged();
//			}
		};
		listView.setAdapter(adapter);
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

		MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
		for (final JaxmppCore jaxmpp : multi.get()) {
			if (!jaxmpp.isConnected()) continue;
			
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
	}

}
