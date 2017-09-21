package org.tigase.messenger.phone.pro.utils;

import android.accounts.Account;
import android.accounts.AccountManager;

public class AccountHelper {

	public static Account getAccount(AccountManager mAccountManager, String name) {
		for (Account account : mAccountManager.getAccounts()) {
			if (account.name.equals(name)) {
				return account;
			}
		}
		return null;
	}

	private AccountHelper() {
	}

}
