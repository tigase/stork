/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import org.tigase.messenger.phone.pro.utils.AsyncDrawable;
import org.tigase.messenger.phone.pro.utils.BitmapWorkerTask;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;

import static org.tigase.messenger.phone.pro.utils.AvatarHelper.getCroppedBitmap;

public class AccountCat
		extends Preference {

	private Account account;
	private SettingsActivity activity;
	private Bitmap mPlaceHolderBitmap;

	public static AccountCat instance(Context context, Account account, SettingsActivity activity) {
		AccountCat result = new AccountCat(context);
		result.setTitle(account.name);
		result.activity = activity;
		result.account = account;

		return result;
	}

	public AccountCat(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public AccountCat(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AccountCat(Context context) {
		super(context);
		init();
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
		setTitle(account.name);
	}

	@SuppressLint("MissingSuperCall")
	@Override
	protected void onBindView(View view) {
		try {
			if (activity != null) {
				boolean active = activity.isAccountActive(account);

				if (!active) {
					setSummary(R.string.account_status_disabled);
					setIcon(R.drawable.ic_account_disconnected);
				} else {
					final Connector.State state = activity.getState(account.name);
					XMPPService.DisconnectionCauses cause = activity.getDisconectionProblemDescription(account);
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
							setSummary(SettingsActivity.getDisconnectedCauseMessage(getContext(), cause));
							setIcon(R.drawable.ic_account_disconnected);
							break;
					}
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

				final AsyncDrawable asyncDrawable = new AsyncDrawable(getContext().getResources(), mPlaceHolderBitmap,
																	  task);
//				avatarView.setImageDrawable(asyncDrawable);
				avatarView.setImageResource(R.drawable.stork_logo);
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

	private void init() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getContext().getResources(), R.drawable.stork_logo, options);
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.inJustDecodeBounds = false;
		this.mPlaceHolderBitmap = getCroppedBitmap(
				BitmapFactory.decodeResource(getContext().getResources(), R.drawable.stork_logo, options));
		setLayoutResource(R.layout.preference_account_item);
	}

}
