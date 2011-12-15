package org.tigase.mobile;

import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class VCardViewActivity extends Activity {

	private JID jid;

	private void fillFields(VCard vcard) {
		((TextView) findViewById(R.id.vcard_fn)).setText(vcard.getFullName());
		((TextView) findViewById(R.id.vcard_bday)).setText(vcard.getBday());
		((TextView) findViewById(R.id.vcard_home_mail)).setText(vcard.getHomeEmail());
		((TextView) findViewById(R.id.vcard_home_tel_voice)).setText(vcard.getHomeTelVoice());
		((TextView) findViewById(R.id.vcard_nickname)).setText(vcard.getNickName());
		((TextView) findViewById(R.id.vcard_url)).setText(vcard.getUrl());

		ImageView avatar = (ImageView) findViewById(R.id.vcard_avatar);

		Bitmap bmp;
		try {
			if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
				String val = vcard.getPhotoVal();
				byte[] buffer = Base64.decode(val);

				bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);

				ContentValues values = new ContentValues();
				values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
				getContentResolver().insert(Uri.parse(RosterProvider.VCARD_URI + "/" + jid.getBareJid().toString()), values);
			} else {
				bmp = null;
			}
		} catch (Exception e) {
			Log.e("tigase", "WTF?", e);
			bmp = null;
		}

		Bitmap x = BitmapFactory.decodeResource(getResources(), R.drawable.user_avatar);
		if (bmp != null) {
			Rect xr = new Rect(37, 35, 431, 394);
			Canvas c = new Canvas(x);
			Rect br = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			c.drawBitmap(bmp, br, xr, null);
			c.save();
		}
		avatar.setImageBitmap(x);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vcard);

		final ProgressDialog dialog = ProgressDialog.show(VCardViewActivity.this, "", "Loading. Please wait...", true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Intent result = new Intent();
				setResult(Activity.RESULT_CANCELED, result);
				finish();
			}
		});

		long id = getIntent().getLongExtra("itemId", -1);

		final TextView fullName = (TextView) findViewById(R.id.vcard_fn);

		final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + id), null, null, null,
				null);
		this.jid = null;
		try {
			cursor.moveToNext();
			this.jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
		} finally {
			cursor.close();
		}
		((TextView) findViewById(R.id.vcard_fn)).setText(jid.toString());

		VCardModule module = ((MessengerApplication) getApplicationContext()).getJaxmpp().getModulesManager().getModule(
				VCardModule.class);
		try {
			module.retrieveVCard(jid, new VCardAsyncCallback() {

				@Override
				public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}

				@Override
				public void onTimeout() throws JaxmppException {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}

				@Override
				protected void onVCardReceived(final VCard vcard) throws XMLException {
					fullName.post(new Runnable() {

						@Override
						public void run() {
							dialog.dismiss();
							fillFields(vcard);
						}
					});
				}
			});
		} catch (JaxmppException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		dialog.show();
	}

}
