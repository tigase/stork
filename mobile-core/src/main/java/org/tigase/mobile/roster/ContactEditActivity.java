package org.tigase.mobile.roster;

import java.util.ArrayList;
import java.util.Collections;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.WarningDialog;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class ContactEditActivity extends FragmentActivity {

	private static final String TAG = "tigase";
	private Spinner groupEdit;
	private ArrayList<String> groups;
	private EditText jabberIdEdit;
	private Jaxmpp jaxmpp;
	private EditText nameEdit;
	private CheckBox requestAuth;

	private RosterItem rosterItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_edit);
		((MessengerApplication) getApplication()).getTracker().trackPageView("/contactEditPage");

		final long id = getIntent().getLongExtra("itemId", -1);
		final BareJID account;

		if (id != -1) {
			JID jid = null;
			final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + id), null, null,
					null, null);
			try {
				cursor.moveToNext();
				jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
				account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));
			} finally {
				cursor.close();
			}
			this.jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
			rosterItem = jaxmpp.getRoster().get(jid.getBareJid());
		} else {
			account = BareJID.bareJIDInstance(getIntent().getStringExtra("account"));
			this.jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
		}

		this.jabberIdEdit = (EditText) findViewById(R.id.ce_jabberid);
		if (rosterItem != null)
			jabberIdEdit.setText(rosterItem.getJid().toString());
		jabberIdEdit.setEnabled(rosterItem == null);

		this.nameEdit = (EditText) findViewById(R.id.ce_name);
		if (rosterItem != null)
			nameEdit.setText(rosterItem.getName());

		this.groups = new ArrayList<String>(jaxmpp.getRoster().getGroups());
		Collections.sort(groups);
		groups.add(0, "- none -");

		this.groupEdit = (Spinner) findViewById(R.id.ce_group);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				groups.toArray(new String[] {}));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupEdit.setAdapter(adapter);

		int position = 0;
		if (rosterItem != null && rosterItem.getGroups().size() > 0) {
			String x = rosterItem.getGroups().get(0);
			position = groups.indexOf(x);
		}
		groupEdit.setSelection(position);

		this.requestAuth = (CheckBox) findViewById(R.id.authRequest);
		if (rosterItem == null) {
			requestAuth.setChecked(true);
			requestAuth.setVisibility(View.VISIBLE);
		} else {
			requestAuth.setChecked(false);
			requestAuth.setVisibility(View.INVISIBLE);
		}

		final Button saveButton = (Button) findViewById(R.id.ce_saveButton);
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateItem();
			}
		});

		final Button cancelButton = (Button) findViewById(R.id.ce_cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	protected void updateItem() {
		final BareJID jid = BareJID.bareJIDInstance(jabberIdEdit.getText().toString());
		final String name = nameEdit.getText().toString();
		final ArrayList<String> g = new ArrayList<String>();
		int p = groupEdit.getSelectedItemPosition();
		if (p > 0) {
			g.add(groups.get(p));
		}

		if (jid == null || jid.toString().length() == 0) {
			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_jid_cant_be_empty);
			return;
		}

		if (jid.getLocalpart() == null || jid.getDomain() == null) {
			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_wrong_jid);
			return;
		}

		if (name == null || name.length() == 0) {
			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_name_cant_be_empty);
			return;
		}

		final ProgressDialog dialog = ProgressDialog.show(ContactEditActivity.this, "",
				getResources().getString(R.string.contact_edit_info_updating), true);
		Runnable r = new Runnable() {

			@Override
			public void run() {

				try {
					jaxmpp.getRoster().add(jid, name, g, new AsyncCallback() {

						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
							dialog.cancel();
							if (error == null)
								WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_unkown);
							else
								WarningDialog.showWarning(ContactEditActivity.this, error.name());
						}

						@Override
						public void onSuccess(Stanza responseStanza) throws JaxmppException {
							if (requestAuth.getVisibility() == View.VISIBLE && requestAuth.isChecked()) {
								jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));
							}
							dialog.cancel();
							finish();
						}

						@Override
						public void onTimeout() throws JaxmppException {
							dialog.cancel();
							WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_timeout);
						}
					});
				} catch (JaxmppException e) {
					dialog.cancel();
					Log.e(TAG, "Can't add buddy", e);
					WarningDialog.showWarning(ContactEditActivity.this, e.getMessage());
				}
			}
		};

		(new Thread(r)).start();

	}
}
