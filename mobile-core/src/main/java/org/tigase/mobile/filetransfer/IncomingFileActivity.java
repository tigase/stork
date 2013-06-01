package org.tigase.mobile.filetransfer;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.service.JaxmppService;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransfer;
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

	private FileTransfer ft;
	private String store = "Download";
	private String tag;

	public void accept(View view) {
		Log.v(TAG, "incoming file accepted");

		FileTransferUtility.registerFileTransfer(ft);
		
		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("peer", ft.getPeer().toString());
		intent.putExtra("sid", ft.getSid());
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "accept");
		intent.putExtra("store", store);

		startService(intent);

		finish();
	}

	private Jaxmpp getJaxmpp(SessionObject sessionObject) {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(sessionObject);
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

		JID peerJid = JID.jidInstance(intent.getStringExtra("peer"));
		String sid = intent.getStringExtra("sid");
		ft = FileTransferUtility.unregisterFileTransfer(peerJid, sid);
		FileTransferUtility.registerFileTransfer(ft);
		
		Long filesize = ft.getFileSize();
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
		
		String mimetype = ft.getFileMimeType();
		String filename = ft.getFilename();
		if (mimetype == null) {
			mimetype = FileTransferUtility.guessMimeType(filename);
			ft.setFileMimeType(mimetype);
		}
		Log.v(TAG, "got mimetype = " + mimetype);

		String senderName = RosterDisplayTools.getDisplayName(ft.getSessionObject(), peerJid.getBareJid());

		((TextView) findViewById(R.id.incoming_file_from_name)).setText(senderName);
		((TextView) findViewById(R.id.incoming_file_from_jid)).setText(senderName.equals(peerJid.getBareJid().toString()) ? ""
				: peerJid.getBareJid().toString());
		((TextView) findViewById(R.id.incoming_file_filename)).setText(filename);
		((TextView) findViewById(R.id.incoming_file_filesize)).setText(filesize == null ? "unknown" : filesizeStr);

		ImageView itemAvatar = ((ImageView) findViewById(R.id.incoming_file_from_avatar));
		AvatarHelper.setAvatarToImageView(peerJid.getBareJid(), itemAvatar, 200);

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
		FileTransferUtility.registerFileTransfer(ft);

		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("peer", ft.getPeer().toString());
		intent.putExtra("sid", ft.getSid());
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "reject");

		startService(intent);

		finish();
	}
}
