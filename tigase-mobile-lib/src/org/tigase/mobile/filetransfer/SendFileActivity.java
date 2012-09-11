package org.tigase.mobile.filetransfer;

import java.util.Map;
import java.util.Map.Entry;

import org.tigase.mobile.ClientIconsTool;
import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.roster.GroupsRosterAdapter;
import org.tigase.mobile.ui.IconContextMenu;
import org.tigase.mobile.ui.IconContextMenu.IconContextItemSelectedListener;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class SendFileActivity extends Activity {

	private static final String TAG = "SendFileActivity";

	private Jaxmpp getJaxmpp(BareJID account) {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
	}

	private RosterItem getRosterItem(long itemId) {
		final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + itemId), null, null,
				null, null);

		try {
			if (!cursor.moveToNext())
				return null;
			BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
			BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

			if (account != null && jid != null) {
				return getJaxmpp(account).getRoster().get(jid);
			}

		} finally {
			cursor.close();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle bundle) {

		super.onCreate(bundle);

		setContentView(R.layout.send_file);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.sendFileContacts);
		Cursor c = getContentResolver().query(Uri.parse(RosterProvider.GROUP_URI), null, null, null, null);
		listView.setAdapter(new GroupsRosterAdapter(this, c) {
			@Override
			protected Cursor getChildrenCursor(Cursor groupCursor) {
				String group = groupCursor.getString(1);
				return getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, "feature",
						new String[] { group, Features.FILE_TRANSFER, Features.BYTESTREAMS }, null);
			}
		});
		listView.setTextFilterEnabled(true);

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		final String mimetype = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && mimetype != null) {
			final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (uri != null) {
				Log.v(TAG, "received input uri = " + uri.toString() + " for path = " + uri.getLastPathSegment());
			}

			listView.setOnChildClickListener(new OnChildClickListener() {

				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
					RosterItem item = getRosterItem(id);
					final String nodeName = item.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
					final Jaxmpp jaxmpp = getJaxmpp(item.getSessionObject().getUserBareJid());
					JID jid = null;
					// try to find resource with messenger
					if (nodeName != null) {
						Map<String, Presence> presences = jaxmpp.getPresence().getPresences(item.getJid());
						for (Presence p : presences.values()) {
							try {
								Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
								if (c == null)
									continue;
								if (nodeName.equals(c.getAttribute("node"))) {
									jid = p.getFrom();
								}
							} catch (JaxmppException ex) {
								Log.v(TAG, "WTF?", ex);
							}
						}
					}
					if (jid == null) {
						jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, item.getJid(), FileTransferUtility.FEATURES);
					}
					AndroidFileTransferUtility.startFileTransfer(SendFileActivity.this, item, jid, uri, mimetype);
					finish();
					return true;
				}
			});

			listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
					ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
					int type = ExpandableListView.getPackedPositionType(info.packedPosition);
					if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
						final RosterItem r = getRosterItem(info.id);
						try {
							Presence p = r.getSessionObject().getPresence().getBestPresence(r.getJid());
							if (p != null && p.getType() == null) {
								prepareResources(menu, r);
							}
						} catch (Exception e) {
						}

						IconContextMenu imenu = new IconContextMenu(SendFileActivity.this, menu, r.getJid().toString(),
								IconContextMenu.ICON_RIGHT);
						imenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {

							@Override
							public void onIconContextItemSelected(MenuItem item, Object info) {
								String resource = item.getTitle().toString();
								JID jid = JID.jidInstance(r.getJid(), resource);
								AndroidFileTransferUtility.startFileTransfer(SendFileActivity.this, r, jid, uri, mimetype);
								finish();
							}

						});
						imenu.show();
					}
				}
			});

		}
	}

	private void prepareResources(ContextMenu menu, RosterItem ri) throws XMLException {
		final Jaxmpp jaxmpp = getJaxmpp(ri.getSessionObject().getUserBareJid());
		Map<String, Presence> all = jaxmpp.getSessionObject().getPresence().getPresences(ri.getJid());

		final CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
		final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);

		for (Entry<String, Presence> entry : all.entrySet()) {
			MenuItem mitem = menu.add(entry.getKey());
			int iconRes = ClientIconsTool.getResourceImage(entry.getValue(), capabilitiesModule, nodeName);
			boolean enabled = FileTransferUtility.resourceContainsFeatures(jaxmpp, entry.getValue().getFrom(),
					FileTransferUtility.FEATURES);
			mitem.setEnabled(enabled);
			mitem.setIcon(iconRes);
		}
	}

}