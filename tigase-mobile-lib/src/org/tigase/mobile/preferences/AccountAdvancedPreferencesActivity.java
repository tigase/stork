package org.tigase.mobile.preferences;

import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class AccountAdvancedPreferencesActivity extends Activity {

	private CompoundButton mobileOptimizations;
	private Spinner presenceQueueTimeout;
	private CompoundButton geolocation;
	private Spinner geolocationPrecision;

	private Account getAccount(final AccountManager accountManager) {
		Account account = null;

		if (getIntent().getExtras().get("account") != null) {
			account = (Account) getIntent().getExtras().get("account");
		} else {
			String jidStr = (String) getIntent().getExtras().get("account_jid");
			for (Account acc : accountManager.getAccounts()) {
				if (jidStr.equals(jidStr)) {
					account = acc;
					break;
				}
			}
		}

		return account;
	}

	private final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
	}

	private final boolean isMobileAvailable(JaxmppCore jaxmpp, String feature) {
		final Element sf = jaxmpp.getSessionObject().getStreamFeatures();
		if (sf == null)
			return false;

		try {
			Element m = sf.getChildrenNS("mobile", feature);
			if (m == null)
				return false;
		} catch (XMLException e) {
			return false;
		}

		return true;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		final Account account = getAccount(accountManager);
		final BareJID accountJid = BareJID.bareJIDInstance(account.name);

		setContentView(R.layout.account_advanced_preferences);

		mobileOptimizations = (CompoundButton) findViewById(R.id.mobile_optimizations);
		presenceQueueTimeout = (Spinner) findViewById(R.id.presence_queue_timeout);

		boolean available_v1 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V1);
		boolean available_v2 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V2);
		mobileOptimizations.setEnabled(available_v1 || available_v2);
		presenceQueueTimeout.setEnabled(available_v1 && !available_v2);

		String valueStr = accountManager.getUserData(account, JaxmppService.MOBILE_OPTIMIZATIONS_ENABLED);
		boolean enabled = valueStr == null || Boolean.valueOf(valueStr);
		mobileOptimizations.setChecked(enabled);
		mobileOptimizations.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				accountManager.setUserData(account, JaxmppService.MOBILE_OPTIMIZATIONS_ENABLED, String.valueOf(isChecked));
				getMulti().get(accountJid).getSessionObject().setUserProperty(JaxmppService.MOBILE_OPTIMIZATIONS_ENABLED,
						isChecked);
			}

		});

		valueStr = accountManager.getUserData(account, JaxmppService.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT);
		int position = (valueStr == null) ? 1 : ((Integer.parseInt(valueStr) / 3) - 1);
		presenceQueueTimeout.setSelection(position);
		presenceQueueTimeout.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				int value = 3 * (position + 1);
				accountManager.setUserData(account, JaxmppService.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT, String.valueOf(value));
				getMulti().get(accountJid).getSessionObject().setUserProperty(JaxmppService.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT,
						value);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		geolocation = (CompoundButton) findViewById(R.id.geolocation);
		geolocationPrecision = (Spinner) findViewById(R.id.geolocation_percision);
		valueStr = accountManager.getUserData(account, JaxmppService.GEOLOCATION_ENABLED);
		if (geolocation != null) {
			geolocation.setChecked(valueStr != null && Boolean.parseBoolean(valueStr));
			geolocation.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					accountManager.setUserData(account, JaxmppService.GEOLOCATION_ENABLED, String.valueOf(isChecked));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					if (isChecked) {
						jaxmpp.getSessionObject().setUserProperty(JaxmppService.GEOLOCATION_ENABLED, isChecked);
						JaxmppService.sendCurrentLocation(jaxmpp, getApplicationContext());
					} else {
						try {
							JaxmppService.updateLocation(jaxmpp, null, getApplicationContext());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						jaxmpp.getSessionObject().setUserProperty(JaxmppService.GEOLOCATION_ENABLED, isChecked);
					}
					geolocationPrecision.setEnabled(isChecked);
				}
			});			
			valueStr = accountManager.getUserData(account, JaxmppService.GEOLOCATION_PRECISION);
			int precision = (valueStr != null) ? Integer.parseInt(valueStr) : 0;
			geolocationPrecision.setSelection(precision);
			geolocationPrecision.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
					int precision = position;
					accountManager.setUserData(account, JaxmppService.GEOLOCATION_PRECISION, String.valueOf(precision));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					jaxmpp.getSessionObject().setUserProperty(JaxmppService.GEOLOCATION_PRECISION, precision);
					JaxmppService.sendCurrentLocation(jaxmpp, getApplicationContext());
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}				
			});
			geolocationPrecision.setEnabled(geolocation.isChecked());
		}

		if (!available_v1 && !available_v2 && getMulti().get(accountJid).isConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.mobile_optimizations_not_supported).setIcon(R.drawable.icon);
			builder.setTitle(R.string.mobile_optimizations).setCancelable(true);
			AlertDialog dlg = builder.create();
			dlg.show();
		}
	}
	
}
