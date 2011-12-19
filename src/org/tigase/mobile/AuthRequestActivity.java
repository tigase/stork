package org.tigase.mobile;

import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AuthRequestActivity extends FragmentActivity {

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
			x = bmp;
		}
		avatar.setImageBitmap(x);
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.auth_request);

		this.jid = JID.jidInstance(getIntent().getStringExtra("jid"));
		final TextView jidTextView = (TextView) findViewById(R.id.vcard_jid);
		jidTextView.setText(jid.toString());

		final VCardModule module = ((MessengerApplication) getApplicationContext()).getJaxmpp().getModulesManager().getModule(
				VCardModule.class);
		final PresenceModule presenceModule = ((MessengerApplication) getApplicationContext()).getJaxmpp().getModulesManager().getModule(
				PresenceModule.class);

		final Button okButton = (Button) findViewById(R.id.req_yesButton);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					presenceModule.subscribed(jid);
					presenceModule.subscribe(jid);
				} catch (Exception e) {
					showWarning("Can't accept subscription");
				}
				finish();
			}
		});

		final Button noButton = (Button) findViewById(R.id.req_noButton);
		noButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					presenceModule.unsubscribe(jid);
					presenceModule.unsubscribed(jid);
				} catch (Exception e) {
					showWarning("Can't deny subscription");
				}
				finish();
			}
		});
		final Button cancelButton = (Button) findViewById(R.id.req_cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		try {
			module.retrieveVCard(jid, new VCardAsyncCallback() {

				@Override
				public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
				}

				@Override
				public void onTimeout() throws JaxmppException {
				}

				@Override
				protected void onVCardReceived(final VCard vcard) throws XMLException {
					jidTextView.post(new Runnable() {

						@Override
						public void run() {
							fillFields(vcard);
						}
					});
				}
			});
		} catch (JaxmppException e) {
			e.printStackTrace();
		}
	}

	private void showWarning(String message) {
		DialogFragment newFragment = WarningDialog.newInstance(message);
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

}
