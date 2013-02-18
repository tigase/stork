package org.tigase.mobile.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AuthenticationService extends Service {

	private static final String TAG = "AuthenticationService";

	private Authenticator mAuthenticator;

	@Override
	public IBinder onBind(Intent intent) {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
		}
		return mAuthenticator.getIBinder();
	}

	@Override
	public void onCreate() {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "TigaseMobile Authentication Service started.");
		}
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public void onDestroy() {
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "TigaseMobile Authentication Service stopped.");
		}
	}
}