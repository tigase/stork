package org.tigase.mobile.filetransfer;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class IncomingFileActivity extends Activity {

	private static final String TAG = "IncomingFileActivity";

	private JID account;
	private String filename;
	private Long filesize;
	private String id;
	private String mimetype;
	private JID sender;
	private String senderName;
	private String sid;
	private String store = "Download";
	private String tag;

	public void accept(View view) {
		Log.v(TAG, "incoming file accepted");

		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("account", account.toString());
		intent.putExtra("sender", sender.toString());
		intent.putExtra("id", id);
		intent.putExtra("sid", sid);
		intent.putExtra("filename", filename);
		if (filesize != null) {
			intent.putExtra("filesize", filesize);
		}
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "accept");
		intent.putExtra("mimetype", mimetype);
		intent.putExtra("store", store);

		startService(intent);

		finish();
	}

	private Jaxmpp getJaxmpp(BareJID account) {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case R.id.incoming_file_accept: {
	// accept(null);
	// break;
	// }
	// case R.id.incoming_file_reject: {
	// reject(null);
	// break;
	// }
	// default:
	// break;
	// }
	//
	// return true;
	// }

	// @Override
	// public boolean onPrepareOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// menu.clear();
	// final Jaxmpp jaxmpp = ((MessengerApplication)
	// getApplicationContext()).getMultiJaxmpp().get(account.getBareJid());
	// if (jaxmpp.isConnected()) {
	// inflater.inflate(R.menu.incoming_file_menu, menu);
	// }
	// return true;
	// }

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.incoming_file);

		Intent intent = getIntent();
		tag = intent.getStringExtra("tag");
		account = JID.jidInstance(intent.getStringExtra("account"));
		sender = JID.jidInstance(intent.getStringExtra("sender"));
		id = intent.getStringExtra("id");
		sid = intent.getStringExtra("sid");
		filename = intent.getStringExtra("filename");
		filesize = intent.getLongExtra("filesize", 0);
		String filesizeStr = null;
		if (filesize == 0) {
			filesize = null;
		} else {
			if (filesize > 1024 * 1024) {
				filesizeStr = String.valueOf(filesize / (1024 * 1024)) + "MB";
			} else if (filesize > 1024) {
				filesizeStr = String.valueOf(filesize / 1024) + "KB";
			} else {
				filesizeStr = String.valueOf(filesize) + "B";
			}
		}
		mimetype = intent.getStringExtra("mimetype");
		if (mimetype == null) {
			mimetype = AndroidFileTransferUtility.guessMimeType(filename);
		}
		Log.v(TAG, "got mimetype = " + mimetype);

		Jaxmpp jaxmpp = getJaxmpp(account.getBareJid());
		RosterItem ri = jaxmpp.getRoster().get(sender.getBareJid());

		senderName = ri != null && ri.getName() != null ? ri.getName() : sender.toString();

		((TextView) findViewById(R.id.incoming_file_from_name)).setText(senderName);
		((TextView) findViewById(R.id.incoming_file_from_jid)).setText(ri == null || ri.getName() == null ? ""
				: sender.toString());
		((TextView) findViewById(R.id.incoming_file_filename)).setText(filename);
		((TextView) findViewById(R.id.incoming_file_filesize)).setText(filesize == null ? "unknown" : filesizeStr);

		ImageView itemAvatar = ((ImageView) findViewById(R.id.incoming_file_from_avatar));
		byte[] avatar = null;
		final Cursor cursor = getContentResolver().query(
				Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(sender.getBareJid().toString())), null, null, null, null);
		try {
			cursor.moveToNext();
			avatar = cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
		} catch (Exception ex) {
			avatar = null;
		} finally {
			cursor.close();
		}
		if (avatar != null) {
			Bitmap bmp = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
			itemAvatar.setImageBitmap(bmp);
		} else {
			itemAvatar.setImageResource(R.drawable.user_avatar);
		}

		ArrayAdapter storeAdapter = ArrayAdapter.createFromResource(this, R.array.incoming_file_stores,
				android.R.layout.simple_spinner_item);
		storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner storeSpinner = (Spinner) findViewById(R.id.incoming_file_store);
		storeSpinner.setAdapter(storeAdapter);
		storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				store = (String) parent.getItemAtPosition(pos);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				parent.setSelection(0);
			}
		});

		int pos = 0;
		if (mimetype != null) {
			if (mimetype.startsWith("video/")) {
				pos = storeAdapter.getPosition("Movies");
			} else if (mimetype.startsWith("audio/")) {
				pos = storeAdapter.getPosition("Music");
			} else if (mimetype.startsWith("image/")) {
				pos = storeAdapter.getPosition("Pictures");
			}
		}
		storeSpinner.setSelection(pos);
	}

	public void reject(View view) {
		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("account", account.toString());
		intent.putExtra("sender", sender.toString());
		intent.putExtra("id", id);
		intent.putExtra("sid", sid);
		intent.putExtra("filename", filename);
		if (filesize != null) {
			intent.putExtra("filesize", filesize);
		}
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "reject");

		startService(intent);

		finish();
	}
}
