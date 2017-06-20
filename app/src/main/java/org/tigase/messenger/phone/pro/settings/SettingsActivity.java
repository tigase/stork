/*
 * SettingsActivity.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.*;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.Authenticator;
import org.tigase.messenger.phone.pro.account.LoginActivity;
import org.tigase.messenger.phone.pro.account.NewAccountActivity;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AsyncDrawable;
import org.tigase.messenger.phone.pro.utils.BitmapWorkerTask;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.eventbus.DefaultEventBus;
import tigase.jaxmpp.core.client.eventbus.EventBus;
import tigase.jaxmpp.core.client.eventbus.EventListener;

import java.util.List;

import static org.tigase.messenger.phone.pro.utils.AvatarHelper.getCroppedBitmap;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See
 * <a href="http://developer.android.com/design/patterns/settings.html"> Android
 * Design: Settings</a> for design guidelines and the
 * <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity
		extends AppCompatPreferenceActivity {

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
		String stringValue = value.toString();

		if (preference instanceof ListPreference) {
			// For list preferences, look up the correct display value in
			// the preference's 'entries' list.
			ListPreference listPreference = (ListPreference) preference;
			int index = listPreference.findIndexOfValue(stringValue);

			// Set the summary to reflect the new value.
			preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

		} else if (preference instanceof RingtonePreference) {
			// For ringtone preferences, look up the correct display value
			// using RingtoneManager.
			if (TextUtils.isEmpty(stringValue)) {
				// Empty values correspond to 'silent' (no ringtone).
				preference.setSummary(R.string.pref_ringtone_silent);

			} else {
				Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

				if (ringtone == null) {
					// Clear the summary if there was a lookup error.
					preference.setSummary(null);
				} else {
					// Set the summary to reflect the new ringtone display
					// name.
					String name = ringtone.getTitle(preference.getContext());
					preference.setSummary(name);
				}
			}

		} else {
			// For all other preferences, set the summary to the value's
			// simple string representation.
			preference.setSummary(stringValue);
		}
		return true;
	};
	private final DefaultEventBus d = new DefaultEventBus();
	private final EventListener eventsRepeater = SettingsActivity.this.d::fire;
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			getService().getMultiJaxmpp().addListener(eventsRepeater);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getService().getMultiJaxmpp().remove(eventsRepeater);
			super.onServiceDisconnected(name);
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
																 PreferenceManager.getDefaultSharedPreferences(
																		 preference.getContext())
																		 .getString(preference.getKey(), ""));
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
				Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	private EventBus getEventBus() {
		return d;
	}

	Connector.State getState(String account) {
		XMPPService s = mConnection.getService();
		if (s != null) {
			Connector.State r = s.getJaxmpp(account).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
			return r == null ? Connector.State.disconnected : r;
		}
		return null;
	}

	/**
	 * This method stops fragment injection in malicious applications. Make sure
	 * to deny any unknown fragments here.
	 */
	protected boolean isValidFragment(String fragmentName) {
		return PreferenceFragment.class.getName().equals(fragmentName) ||
				GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
				DataSyncPreferenceFragment.class.getName().equals(fragmentName) ||
				AccountsPreferenceFragment.class.getName().equals(fragmentName) ||
				NotificationPreferenceFragment.class.getName().equals(fragmentName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			if (!super.onMenuItemSelected(featureId, item)) {
				NavUtils.navigateUpFromSameTask(this);
			}
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent service = new Intent(getApplicationContext(), XMPPService.class);
		bindService(service, mConnection, 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mConnection);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	private static class AccountCat
			extends Preference {

		private final String account;
		private final boolean active;
		private final SettingsActivity activity;
		private final Bitmap mPlaceHolderBitmap;

		AccountCat(Context context, String account, boolean active, SettingsActivity activity) {
			super(context);
			setTitle(account);
			this.activity = activity;
			this.account = account;
			this.active = active;
			setLayoutResource(R.layout.preference_account_item);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher, options);
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inJustDecodeBounds = false;
			this.mPlaceHolderBitmap = getCroppedBitmap(
					BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher, options));
		}

		@Override
		protected void onBindView(View view) {
			try {
				if (!active) {
					setSummary(R.string.account_status_disabled);
					setIcon(R.drawable.ic_account_disconnected);
				} else {
					final Connector.State state = activity.getState(account);
					switch (state) {
						case connected:
							setSummary(R.string.account_status_connected);
							setIcon(R.drawable.ic_account_connected);
							break;
						case connecting:
							setSummary(R.string.account_status_connecting);
							setIcon(R.drawable.ic_account_connected);
							break;
						case disconnecting:
							setSummary(R.string.account_status_disconnecting);
							setIcon(R.drawable.ic_account_disconnected);
							break;
						default:
						case disconnected:
							setSummary(R.string.account_status_disconnected);
							setIcon(R.drawable.ic_account_disconnected);
							break;
					}
				}

				final TextView titleView = (TextView) view.findViewById(R.id.account_name);
				if (titleView != null) {
					final CharSequence title = getTitle();
					if (!TextUtils.isEmpty(title)) {
						titleView.setText(title);
						titleView.setVisibility(View.VISIBLE);
					} else {
						titleView.setVisibility(View.GONE);
					}
				}

				final TextView summaryView = (TextView) view.findViewById(R.id.summary);
				if (summaryView != null) {
					final CharSequence summary = getSummary();
					if (!TextUtils.isEmpty(summary)) {
						summaryView.setText(summary);
						summaryView.setVisibility(View.VISIBLE);
					} else {
						summaryView.setVisibility(View.GONE);
					}
				}

				ImageView statusView = (ImageView) view.findViewById(R.id.account_status);
				if (statusView != null) {
					Drawable ic = getIcon();
					if (ic != null) {
						statusView.setImageDrawable(ic);
						statusView.setVisibility(View.VISIBLE);
					} else {
						statusView.setVisibility(View.GONE);
					}
				}

				ImageView avatarView = (ImageView) view.findViewById(R.id.contact_avatar);
				if (avatarView != null) {
					final BitmapWorkerTask task = new BitmapWorkerTask(getContext(), avatarView, null);

					final AsyncDrawable asyncDrawable = new AsyncDrawable(getContext().getResources(),
																		  mPlaceHolderBitmap, task);
					avatarView.setImageDrawable(asyncDrawable);
					try {
						task.execute(BareJID.bareJIDInstance(getTitle().toString()));
					} catch (java.util.concurrent.RejectedExecutionException e) {
						// ignoring: probably avatar big as cow
						Log.e("Settings", "Cannot load avatar for account " + getTitle(), e);
					}
				}

			} catch (Exception e) {
				Log.wtf("SettingsActivity", e);
				setSummary(R.string.account_status_unknown);
				setIcon(R.drawable.ic_account_disconnected);
			}
		}

	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AccountsPreferenceFragment
			extends PreferenceFragment {

		private PreferenceScreen screen;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_accounts_list);
			this.screen = this.getPreferenceScreen();
			((SettingsActivity) getActivity()).getEventBus()
					.addListener(Connector.StateChangedHandler.StateChangedEvent.class, event -> {
						BaseAdapter adapter = (BaseAdapter) screen.getRootAdapter();
						getActivity().runOnUiThread(adapter::notifyDataSetChanged);
					});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}

		@Override
		public void onResume() {
			super.onResume();
			screen.removeAll();

			setHasOptionsMenu(true);

			AccountManager am = AccountManager.get(screen.getContext());

			Preference addAccountPref = new Preference(screen.getContext());
			addAccountPref.setIntent(new Intent(screen.getContext(), NewAccountActivity.class));
			addAccountPref.setTitle(getActivity().getString(R.string.pref_accounts_newaccount));
			addAccountPref.setIcon(android.R.drawable.ic_input_add);
			screen.addPreference(addAccountPref);

			for (Account account : am.getAccountsByType(Authenticator.ACCOUNT_TYPE)) {
				boolean active = Boolean.parseBoolean(am.getUserData(account, AccountsConstants.FIELD_ACTIVE));

				AccountCat category = new AccountCat(screen.getContext(), account.name, active,
													 (SettingsActivity) getActivity());
				Intent x = new Intent(screen.getContext(), LoginActivity.class);
				x.putExtra("account_name", account.name);
				category.setIntent(x);
				screen.addPreference(category);
			}
		}
	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DataSyncPreferenceFragment
			extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_data_sync);
			setHasOptionsMenu(true);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("sync_frequency"));
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment
			extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);
			setHasOptionsMenu(true);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("away_delay_seconds"));
			bindPreferenceSummaryToValue(findPreference("xa_delay_seconds"));
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * This fragment shows notification preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class NotificationPreferenceFragment
			extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_notification);
			setHasOptionsMenu(true);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
			bindPreferenceSummaryToValue(findPreference("notifications_new_groupmessage_ringtone"));
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}
}
