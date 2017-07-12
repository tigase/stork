package org.tigase.messenger.phone.pro;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.service.XMPPService;

/**
 * Created by bmalkow on 18.05.2017.
 */
public class MyFirebaseInstanceIDService
		extends FirebaseInstanceIdService {

	private final static String TAG = "MyFirebaseIDService";

	private AccountManager mAccountManager;

	@Override
	public void onCreate() {
		super.onCreate();
		mAccountManager = AccountManager.get(this);
	}

	@Override
	public void onTokenRefresh() {
		// Get updated InstanceID token.
		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
		Log.d(TAG, "Refreshed token: " + refreshedToken);

		for (Account account : mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
			mAccountManager.setUserData(account, AccountsConstants.PUSH_SERVICE_NODE_KEY, null);
			String tmp = mAccountManager.getUserData(account, AccountsConstants.PUSH_NOTIFICATION);
			boolean enabled = tmp == null ? false : Boolean.parseBoolean(tmp);

			Intent action = new Intent(XMPPService.PUSH_NOTIFICATION_CHANGED);
			action.putExtra("account", account);
			action.putExtra("state", (Boolean) enabled);
			sendBroadcast(action);
		}
	}

}
