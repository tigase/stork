package org.tigase.mobile.vcard;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import org.tigase.mobile.Constants;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class VCardEditorActivity extends Activity {

	private static final int ERROR_TOAST = 3;

	private static final int PICK_ACCOUNT = 2;
	private static final int PICK_IMAGE = 1;

	private static final int PUBLISHED_TOAST = 1;
	private static final String TAG = "VCardEditorActivity";
	private static final int TIMEOUT_TOAST = 2;

	/**
	 * Fill activity for editing vcard from vcard instance
	 * 
	 * @param activity
	 * @param contentResolver
	 * @param resources
	 * @param jid
	 * @param vcard
	 */
	public static void fillFields(final Activity activity, final ContentResolver contentResolver, final Resources resources,
			final JID jid, final VCard vcard) {
		((TextView) activity.findViewById(R.id.fullname)).setText(vcard.getFullName());
		((TextView) activity.findViewById(R.id.nickname)).setText(vcard.getNickName());
		((TextView) activity.findViewById(R.id.birthday)).setText(vcard.getBday());
		((TextView) activity.findViewById(R.id.email)).setText(vcard.getHomeEmail());

		ImageView avatar = (ImageView) activity.findViewById(R.id.avatarButton);
		Bitmap bmp;
		try {
			if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
				String val = vcard.getPhotoVal();
				byte[] buffer = Base64.decode(val);

				bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
				((VCardEditorActivity) activity).bitmap = bmp;
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

	private ImageButton avatar;

	private Bitmap bitmap = null;
	private JID jid;

	private VCard vcard;

	/**
	 * Serialize bitmap instance to byte array encoded in PNG format
	 * 
	 * @param bmp
	 * @return
	 */
	private byte[] bitmapToByteArray(Bitmap bmp) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
		return bos.toByteArray();
	}

	/**
	 * Starts activity to select picture for avatar
	 */
	private void chooseAvatar() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE);
	}

	/**
	 * Create progress dialog based on id of resource string
	 * 
	 * @param resourceString
	 * @return
	 */
	private ProgressDialog createProgress(int resourceString) {
		final ProgressDialog dialog = ProgressDialog.show(VCardEditorActivity.this, "",
				getResources().getString(resourceString), true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Intent result = new Intent();
				setResult(Activity.RESULT_CANCELED, result);
				finish();
			}
		});
		return dialog;
	}

	/**
	 * Starts requesting vcard
	 */
	private void downloadVCard() {
		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(jid.getBareJid());
		final VCardModule module = jaxmpp.getModule(VCardModule.class);

		if (jaxmpp.isConnected()) {
			final TextView fullName = (TextView) findViewById(R.id.fullname);

			final ProgressDialog dialog = createProgress(R.string.vcard_retrieving);

			(new Thread() {
				@Override
				public void run() {
					try {
						module.retrieveVCard(jid, new VCardAsyncCallback() {

							@Override
							public void onError(Stanza responseStanza, final ErrorCondition error) throws JaxmppException {
								dialog.dismiss();
								showToast(ERROR_TOAST);
							}

							@Override
							public void onTimeout() throws JaxmppException {
								dialog.dismiss();
								showToast(TIMEOUT_TOAST);
							}

							@Override
							protected void onVCardReceived(final VCard vcard) throws XMLException {
								fullName.post(new Runnable() {

									@Override
									public void run() {
										dialog.dismiss();
										VCardEditorActivity.this.vcard = vcard;
										fillFields(VCardEditorActivity.this, getContentResolver(), getResources(), jid, vcard);
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
			dialog.show();
		}

	}

	/**
	 * Returns image loaded from file and scaled to 128
	 * 
	 * @param path
	 *            - path to image
	 * @return scaled image
	 */
	private Bitmap getScaledImage(Uri uri) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 128;

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	@TargetApi(11)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
			Uri _uri = data.getData();

			if (_uri != null) {
				// Link to the image
				Bitmap bmp = getScaledImage(_uri);
				avatar.setImageBitmap(bmp);

				byte[] buffer = bitmapToByteArray(bmp);
				bitmap = bmp;
				ContentValues values = new ContentValues();
				values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
				getContentResolver().insert(
						Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())), values);
			}
		} else if (requestCode == PICK_ACCOUNT) {
			if (data == null || data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) == null) {
				this.finish();
				return;
			}
			String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			setAccountJid(JID.jidInstance(accName));
			this.invalidateOptionsMenu();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vcard_editor);

		avatar = (ImageButton) findViewById(R.id.avatarButton);
		avatar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				chooseAvatar();
			}
		});

		JID jid = null;
		Account account = (Account) getIntent().getParcelableExtra("account");
		if (account != null) {
			jid = JID.jidInstance(account.name);
		} else {
			jid = JID.jidInstance(getIntent().getStringExtra("account_jid"));
		}

		if (account != null && Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
			startChooseAccountIceCream(account);
		} else {
			setAccountJid(jid);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.vcard_editor_refresh) {
			Log.v(TAG, "downloading vcard");
			downloadVCard();
		} else if (item.getItemId() == R.id.vcard_editor_publish) {
			Log.v(TAG, "publishing vcard");
			publishVCard();
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		if (jid != null) {
			final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(jid.getBareJid());
			if (jaxmpp.isConnected()) {
				inflater.inflate(R.menu.vcard_editor_menu, menu);
			}
		}
		return true;
	}

	/**
	 * Starts publishing vcard
	 */
	private void publishVCard() {
		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(jid.getBareJid());

		if (jaxmpp.isConnected()) {
			String fullname = ((TextView) findViewById(R.id.fullname)).getText().toString();
			String nick = ((TextView) findViewById(R.id.nickname)).getText().toString();
			String bday = ((TextView) findViewById(R.id.birthday)).getText().toString();
			String email = ((TextView) findViewById(R.id.email)).getText().toString();
			vcard.setFullName(fullname);
			vcard.setNickName(nick);
			vcard.setHomeEmail(email);
			vcard.setBday(bday);

			byte[] buffer = bitmap == null ? null : bitmapToByteArray(bitmap);
			if (buffer != null) {
				vcard.setPhotoVal(Base64.encode(buffer));
				vcard.setPhotoType("image/png");
			} else {
				vcard.setPhotoVal(null);
				vcard.setPhotoType(null);
			}

			final ProgressDialog dialog = createProgress(R.string.vcard_publishing);

			(new Thread() {
				@Override
				public void run() {

					try {
						final IQ iq = IQ.create();
						iq.setType(StanzaType.set);
						iq.addChild(vcard.makeElement());

						jaxmpp.send(iq, new AsyncCallback() {

							@Override
							public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
								dialog.dismiss();
								showToast(ERROR_TOAST);
							}

							@Override
							public void onSuccess(Stanza responseStanza) throws JaxmppException {
								dialog.dismiss();
								showToast(PUBLISHED_TOAST);
							}

							@Override
							public void onTimeout() throws JaxmppException {
								dialog.dismiss();
								showToast(TIMEOUT_TOAST);
							}

						});
					} catch (Exception ex) {
						Log.v(TAG, "problems with publishing vcard", ex);
					}
				}
			}).start();

			dialog.show();
		}
	}

	protected void setAccountJid(JID jid_) {
		this.jid = jid_;

		final Cursor cursor = getContentResolver().query(
				Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())), null, null, null, null);
		try {
			cursor.moveToNext();
			byte[] buffer = cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
			Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
			avatar.setImageBitmap(bmp);
		} catch (Exception ex) {

		} finally {
			cursor.close();
		}

		downloadVCard();

		final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(jid.getBareJid());
		boolean enabled = jaxmpp.isConnected();
		((TextView) findViewById(R.id.fullname)).setEnabled(enabled);
		((TextView) findViewById(R.id.nickname)).setEnabled(enabled);
		((TextView) findViewById(R.id.birthday)).setEnabled(enabled);
		((TextView) findViewById(R.id.email)).setEnabled(enabled);
	}

	/**
	 * Show toast based on type
	 * 
	 * @param type
	 *            - type of message to present
	 */
	protected void showToast(final int type) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Context context = getApplicationContext();
				CharSequence text = null;
				switch (type) {
				case PUBLISHED_TOAST:
					text = getResources().getString(R.string.vcard_published_toast);
					break;
				case TIMEOUT_TOAST:
					text = getResources().getString(R.string.vcard_timeout_toast);
					break;
				case ERROR_TOAST:
					text = getResources().getString(R.string.vcard_error_toast);
					break;
				}
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
		});
	}

	@TargetApi(14)
	private void startChooseAccountIceCream(final Account account) {
		Intent intentChooser = AccountManager.newChooseAccountIntent(account, null, new String[] { Constants.ACCOUNT_TYPE },
				false, null, null, null, null);
		this.startActivityForResult(intentChooser, PICK_ACCOUNT);
	}
}
