package org.tigase.messenger.phone.pro.roster.contact;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class EditContactActivity extends AppCompatActivity {

    private final ArrayList accountsList = new ArrayList<>();
    @Bind(R.id.contact_account)
    Spinner mAccountSelector;
    @Bind(R.id.contact_xmppid)
    EditText mContactXMPPID;
    @Bind(R.id.contact_display_name)
    EditText mContactName;
    @Bind(R.id.contact_progress)
    ProgressBar progressBar;
    private ArrayAdapter<Object> sa;
    private XMPPService mService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            XMPPService.LocalBinder binder = (XMPPService.LocalBinder) service;
            mService = binder.getService();

            sa.clear();
            for (JaxmppCore j : mService.getMultiJaxmpp().get()) {
                sa.add(j.getSessionObject().getUserBareJid());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @OnClick(R.id.contact_add_button)
    void onAddButtonClick() {
        mContactXMPPID.setError(null);
        BareJID jid;
        try {
            if (mContactXMPPID.getText().toString().isEmpty()) {
                mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
                return;
            }
            jid = BareJID.bareJIDInstance(mContactXMPPID.getText().toString());
        } catch (Exception e) {
            mContactXMPPID.setError(getString(R.string.contact_xmppid_invalid));
            return;
        }
        if (mService != null) {
            AddContactTask task = new AddContactTask(BareJID.bareJIDInstance(mAccountSelector.getSelectedItem().toString()), jid, mContactName.getText().toString());
            task.execute();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
        ButterKnife.bind(this);

        this.sa = new ArrayAdapter<>(getBaseContext(), R.layout.account_list_item, R.id.account_name, accountsList);
        mAccountSelector.setAdapter(sa);
    }

    @OnEditorAction({R.id.contact_xmppid, R.id.contact_display_name})
    boolean onEditorAction(KeyEvent key) {
        onAddButtonClick();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent service = new Intent(getApplicationContext(), XMPPService.class);
        bindService(service, mServiceConnection, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }

    private class AddContactTask extends AsyncTask<Void, Integer, Boolean> {

        private final String name;
        private final BareJID jid;
        private final BareJID account;
        private boolean result = false;
        private XMPPException.ErrorCondition error = null;

        public AddContactTask(BareJID account, BareJID jid, String name) {
            this.jid = jid;
            this.name = name;
            this.account = account;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                final Jaxmpp jaxmpp = mService.getJaxmpp(account);
                RosterStore rosterStore = RosterModule.getRosterStore(jaxmpp.getSessionObject());
                rosterStore.add(jid, name, new AsyncCallback() {
                    @Override
                    public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
                        error = error;
                        synchronized (AddContactTask.this) {
                            AddContactTask.this.notify();
                        }
                    }

                    @Override
                    public void onSuccess(Stanza responseStanza) throws JaxmppException {
                        result = true;
                        synchronized (AddContactTask.this) {
                            AddContactTask.this.notify();
                        }
                    }

                    @Override
                    public void onTimeout() throws JaxmppException {
                        synchronized (AddContactTask.this) {
                            AddContactTask.this.notify();
                        }
                    }
                });

                jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));

                synchronized (AddContactTask.this) {
                    AddContactTask.this.wait();
                }


                return result;
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Can't add contact to roster", e);
                Toast.makeText(getBaseContext(), "ERROR " + e.getMessage(), Toast.LENGTH_SHORT);
                return Boolean.FALSE;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressBar.setVisibility(View.GONE);
            if (result) {
                EditContactActivity.this.finish();
            } else if (error != null) {
                Toast.makeText(getBaseContext(), "Error " + error, Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(getBaseContext(), "Timeout ", Toast.LENGTH_SHORT);
            }
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }
    }
}
