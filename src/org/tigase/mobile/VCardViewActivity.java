package org.tigase.mobile;

import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class VCardViewActivity extends Activity {

	private void fillFields(VCard vcard) {
		((TextView) findViewById(R.id.vcard_fn)).setText(vcard.getFullName());
		((TextView) findViewById(R.id.vcard_bday)).setText(vcard.getBday());
		((TextView) findViewById(R.id.vcard_home_mail)).setText(vcard.getHomeEmail());
		((TextView) findViewById(R.id.vcard_home_tel_voice)).setText(vcard.getHomeTelVoice());
		((TextView) findViewById(R.id.vcard_nickname)).setText(vcard.getNickName());
		((TextView) findViewById(R.id.vcard_url)).setText(vcard.getUrl());
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
		JID jid = null;
		try {
			cursor.moveToNext();
			jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
		} finally {
			cursor.close();
		}

		VCardModule module = XmppService.jaxmpp(this).getModulesManager().getModule(VCardModule.class);
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
