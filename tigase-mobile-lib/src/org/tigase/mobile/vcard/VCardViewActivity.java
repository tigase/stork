package org.tigase.mobile.vcard;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

public class VCardViewActivity extends Activity {

	private final static int ERROR_DIALOG = 2;

	private final static int TIMEOUT_DIALOG = 1;

	public static void fillFields(final Activity activity, final ContentResolver contentResolver, final Resources resources,
			final JID jid, final VCard vcard, final RosterItem rosterItem) {
		((TextView) activity.findViewById(R.id.vcard_fn)).setText(vcard.getFullName());
		((TextView) activity.findViewById(R.id.vcard_title)).setText(vcard.getTitle());
		((TextView) activity.findViewById(R.id.vcard_org)).setText(vcard.getOrgName());
		((TextView) activity.findViewById(R.id.vcard_bday)).setText(vcard.getBday());
		((TextView) activity.findViewById(R.id.vcard_home_mail)).setText(vcard.getHomeEmail());
		((TextView) activity.findViewById(R.id.vcard_home_tel_voice)).setText(vcard.getHomeTelVoice());
		((TextView) activity.findViewById(R.id.vcard_nickname)).setText(vcard.getNickName());
		((TextView) activity.findViewById(R.id.vcard_url)).setText(vcard.getUrl());

		((TableRow) activity.findViewById(R.id.vcard_subscription_status_row)).setVisibility(rosterItem == null ? View.GONE
				: View.VISIBLE);
		if (rosterItem != null) {
			((TextView) activity.findViewById(R.id.vcard_subscription_status)).setText(rosterItem.getSubscription().name());

		}

		ImageView avatar = (ImageView) activity.findViewById(R.id.vcard_avatar);
		Bitmap bmp;
		try {
			if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
				String val = vcard.getPhotoVal();
				byte[] buffer = Base64.decode(val);

				bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);

				ContentValues values = new ContentValues();
				values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
				contentResolver.insert(Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())),
						values);
			} else {
				bmp = null;
			}
		} catch (Exception e) {
			Log.e("tigase", "WTF?", e);
			bmp = null;
		}

		Bitmap x = BitmapFactory.decodeResource(resources, R.drawable.user_avatar);
		if (bmp != null) {
			x = bmp;
		}
		avatar.setImageBitmap(x);
	}

	private BareJID account;

	private JID jid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vcard);

		((MessengerApplication) getApplication()).getTracker().trackPageView("/vcardViewPage");

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
		this.account = null;
		try {
			cursor.moveToNext();
			this.jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
			account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

		} finally {
			cursor.close();
		}
		((TextView) findViewById(R.id.vcard_fn)).setText(jid.toString());
		((TextView) findViewById(R.id.vcard_jid)).setText(jid.toString());

		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
		final RosterItem rosterItem = jaxmpp.getRoster().get(jid.getBareJid());

		((TableRow) findViewById(R.id.vcard_subscription_status_row)).setVisibility(rosterItem == null ? View.GONE
				: View.VISIBLE);
		if (rosterItem != null) {
			((TextView) findViewById(R.id.vcard_subscription_status)).setText(rosterItem.getSubscription().name());
		}

		final VCardModule module = jaxmpp.getModule(VCardModule.class);

		(new Thread() {
			@Override
			public void run() {
				try {
					module.retrieveVCard(jid, new VCardAsyncCallback() {

						@Override
						public void onError(Stanza responseStanza, final ErrorCondition error) throws JaxmppException {
							dialog.dismiss();

							fullName.post(new Runnable() {

								@Override
								public void run() {
									Bundle args = new Bundle();
									if (error != null)
										args.putString("condition", error.name());
									showDialog(ERROR_DIALOG, args);
								}
							});

						}

						@Override
						public void onTimeout() throws JaxmppException {
							dialog.dismiss();
							fullName.post(new Runnable() {

								@Override
								public void run() {
									showDialog(TIMEOUT_DIALOG);
								}
							});
						}

						@Override
						protected void onVCardReceived(final VCard vcard) throws XMLException {
							fullName.post(new Runnable() {

								@Override
								public void run() {
									dialog.dismiss();
									fillFields(VCardViewActivity.this, getContentResolver(), getResources(), jid, vcard,
											rosterItem);
								}
							});
						}
					});
				} catch (JaxmppException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			showActionBar();
		}

		dialog.show();
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case TIMEOUT_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error");
			builder.setMessage("Request timeout");
			builder.setCancelable(true);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			return builder.create();
		}
		case ERROR_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error");
			String message = "Error?";
			if (args != null) {
				String t = args.getString("condition");
				if (t != null)
					message = t;
			}
			builder.setMessage(message);
			builder.setCancelable(true);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			return builder.create();
		}
		default:
			return null;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@TargetApi(11)
	private void showActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}
}
