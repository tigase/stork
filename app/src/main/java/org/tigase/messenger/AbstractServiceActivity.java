package org.tigase.messenger;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;

import java.util.ArrayList;

public abstract class AbstractServiceActivity
		extends AppCompatActivity {

	private final ArrayList<Runnable> doAfterBind = new ArrayList<>();
	private final MainActivity.XMPPServiceConnection mServiceConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			AbstractServiceActivity.this.onXMPPServiceConnected();
			try {
				for (Runnable runnable : doAfterBind) {
					runnable.run();
				}
			} finally {
				doAfterBind.clear();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			try {
				AbstractServiceActivity.this.onXMPPServiceDisconnected();
			} finally {
				super.onServiceDisconnected(name);
			}
		}
	};

	public JaxmppCore getJaxmpp(String accountJid) {
		return this.getJaxmpp(BareJID.bareJIDInstance(accountJid));
	}

	public JaxmppCore getJaxmpp(BareJID accountJid) {
		XMPPService s = mServiceConnection.getService();
		if (s == null) {
			return null;
		}
		return s.getJaxmpp(accountJid);
	}

	public MainActivity.XMPPServiceConnection getServiceConnection() {
		return mServiceConnection;
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

	protected abstract void onXMPPServiceConnected();

	protected abstract void onXMPPServiceDisconnected();

	protected final void startWhenBinded(Runnable r) {
		if (getServiceConnection().getService() == null) {
			doAfterBind.add(r);
		} else {
			r.run();
		}
	}
}
