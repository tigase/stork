/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.conversations.imagepreview;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.AbstractServiceActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.MessageSender;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

import java.io.IOException;

public class PreviewImageActivity
		extends AbstractServiceActivity {

	public static final String ACCOUNT_KEY = "ACCOUNT_KEY";
	public static final String JID_KEY = "JID_KEY";
	public static final String BITMAP = "BITMAP";
	public static final String MIME = "MIME";
	private final static String TAG = "DataReceiver";
	private String account;
	private ImageView contactAvatar;
	private TextView contactName;
	private Uri data;
	private ImageView imagePreview;
	private BareJID jid;
	private EditText messageText;
	private ImageView sendButton;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				//NavUtils.navigateUpFromSameTask(this);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	String getContactName() {
		try {
			final String[] cols = new String[]{DatabaseContract.OpenChats.FIELD_ID,
											   DatabaseContract.OpenChats.FIELD_ACCOUNT,
											   DatabaseContract.OpenChats.FIELD_JID, ChatProvider.FIELD_NAME};

			try (Cursor c = getContentResolver().query(Uri.parse(ChatProvider.OPEN_CHATS_URI + "/" + jid), cols, null,
													   null, null)) {
				if (c.moveToNext()) {
					return c.getString(c.getColumnIndex(ChatProvider.FIELD_NAME));
				} else {
					return jid.toString();
				}
			}
		} catch (Throwable e) {
			Log.wtf("Preview", e);
			return jid.toString();
		}
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preview_image);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		this.jid = BareJID.bareJIDInstance(getIntent().getStringExtra(JID_KEY));
		this.account = getIntent().getStringExtra(ACCOUNT_KEY);
		this.data = getIntent().getData();

		this.contactAvatar = (ImageView) findViewById(R.id.contact_avatar);
		this.contactName = (TextView) findViewById(R.id.contact_display_name);
		this.imagePreview = (ImageView) findViewById(R.id.imageView);
		this.messageText = (EditText) findViewById(R.id.messageText);
		this.sendButton = (ImageView) findViewById(R.id.send_button);

		this.sendButton.setOnClickListener(v -> send());

		AvatarHelper.setAvatarToImageView(this.jid, contactAvatar);

		Log.i(TAG, ">" + data);
		this.imagePreview.setImageURI(data);

		this.contactName.setText(getContactName());

		try {
			String mime = MessageSender.getMimeType(this, data);
			Bitmap b = MessageSender.getBitmapFromUri(this, data);

			Log.i(TAG, mime);
		} catch (IOException e) {
			Log.e(TAG, "Cannot load image", e);
		}
	}

	@Override
	protected void onXMPPServiceConnected() {

	}

	@Override
	protected void onXMPPServiceDisconnected() {

	}

	private void send() {
		Intent returnIntent = new Intent();
		returnIntent.setData(this.data);
		String txt = messageText.getText().toString();
		if (txt != null && !txt.trim().isEmpty()) {
			returnIntent.putExtra(ChatActivity.TEXT, txt);
		}

		setResult(Activity.RESULT_OK, returnIntent);
		finish();
	}
}
