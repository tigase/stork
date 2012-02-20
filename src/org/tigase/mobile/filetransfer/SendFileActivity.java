package org.tigase.mobile.filetransfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.filetransfer.FileTransferModule.StreamInitiationOfferAsyncCallback;
import org.tigase.mobile.roster.RosterAdapter;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.j2se.Jaxmpp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

public class SendFileActivity extends Activity {

	private static final String TAG = "SendFileActivity";

	public void onCreate(Bundle bundle) {

		super.onCreate(bundle);

		setContentView(R.layout.send_file);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.sendFileContacts);
		Cursor c = getContentResolver().query(
				Uri.parse(RosterProvider.GROUP_URI), null, null, null, null);
		// listView.setAdapter(new SimpleCursorAdapter(this,
		// R.layout.roster_item, c, new String[] {
		// RosterTableMetaData.FIELD_DISPLAY_NAME,
		// RosterTableMetaData.FIELD_STATUS_MESSAGE,
		// RosterTableMetaData.FIELD_AVATAR }, new int[] { R.id.roster_item_jid,
		// R.id.roster_item_description, R.id.imageView1}));
		listView.setAdapter(new RosterAdapter(this, c) {
			@Override
			protected Cursor getChildrenCursor(Cursor groupCursor) {
				String group = groupCursor.getString(1);
				return getContentResolver().query(
						Uri.parse(RosterProvider.CONTENT_URI), null, "status",
						new String[] { group }, null);
			}
		});
		listView.setTextFilterEnabled(true);

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		final String mimetype = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && mimetype != null) {
			final Uri uri = (Uri) intent
					.getParcelableExtra(Intent.EXTRA_STREAM);
			final String filename = uri != null ? uri.getLastPathSegment() : ""; 
			if (uri != null) {				
				Log.v(TAG, "received input uri = " + uri.toString()+" for path = "+uri.getLastPathSegment());
			}
			// if ("text/plain".equals(type)) {
			// handleSendText(intent); // Handle text being sent
			// } else if (type.startsWith("image/")) {
			// handleSendImage(intent); // Handle single image being sent
			// }
			// } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type !=
			// null) {
			// if (type.startsWith("image/")) {
			// handleSendMultipleImages(intent); // Handle multiple images being
			// sent
			// }
			// } else {
			// // Handle other intents, such as being started from the home
			// screen
			// }

			final ContentResolver cr = getContentResolver();
			listView.setOnChildClickListener(new OnChildClickListener() {

				@Override
				public boolean onChildClick(ExpandableListView parent, View v,
						int groupPosition, int childPosition, long id) {
					final Cursor cursor = getContentResolver().query(
							Uri.parse(RosterProvider.CONTENT_URI + "/" + id),
							null, null, null, null);
					try {
						cursor.moveToNext();
						final JID bareJid = JID.jidInstance(cursor.getString(cursor
								.getColumnIndex(RosterTableMetaData.FIELD_JID)));
						final String name = cursor.getString(cursor
								.getColumnIndex(RosterTableMetaData.FIELD_DISPLAY_NAME));
						final BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor
								.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

						new Thread() {
							public void run() {

								try {
									final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext())
											.getMultiJaxmpp().get(account);
									final JID jid = jaxmpp
											.getPresence()
											.getBestPresence(
													bareJid.getBareJid())
											.getFrom();
									final FileTransferModule ftModule = jaxmpp
											.getModulesManager().getModule(
													FileTransferModule.class);
									final InputStream is = cr
											.openInputStream(uri);
									final long size = is.available();
									final FileTransfer ft = new FileTransfer(
											jaxmpp, jid, name, filename, is, size);
									ftModule.sendStreamInitiationOffer(
											jid,
											uri.getLastPathSegment(),
											mimetype,
											size,
											new StreamInitiationOfferAsyncCallback() {
												@Override
												public void onAccept(String sid) {
													Log.v(TAG,
															"stream initiation accepted by "
																	+ jid.toString());
													ft.setSid(sid);
													FileTransferUtility
															.onStreamAccepted(ft);
												}

												@Override
												public void onReject() {
													Log.v(TAG,
															"stream initiation rejected by "
																	+ jid.toString());
													ft.transferError("transfer rejected");
												}

												@Override
												public void onError() {
													Log.v(TAG,
															"stream initiation failed for "
																	+ jid.toString());
													ft.transferError("transfer initiation failed");
												}
											});
								} catch (XMLException e) {
									Log.e(TAG, "WTF?", e);
								} catch (JaxmppException e) {
									Log.e(TAG, "WTF?", e);
								} catch (FileNotFoundException e) {
									Log.e(TAG, "WTF?", e);
								} catch (IOException e) {
									Log.e(TAG, "WTF?", e);
								}
							}
						}.start();
					} finally {
						cursor.close();
						finish();
					}
					return true;
				}
			});

		}
	}
}