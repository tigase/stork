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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.github.abdularis.civ.StorkAvatarView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.core.client.Connector;

import static org.tigase.messenger.phone.pro.utils.AvatarHelper.getCroppedBitmap;

@Deprecated
public class AccountCat
		extends Preference {

	private String accountName;
	private AppCompatTextView accountNameView;
	private boolean active;
	private StorkAvatarView avatarImage;
	private XMPPService.DisconnectionCauses cause;
	private Bitmap mPlaceHolderBitmap;
	private Connector.State state = Connector.State.disconnected;
	//	@Override
//	public void onBindViewHolder(PreferenceViewHolder holder) {
//		super.onBindViewHolder(holder);
//		((AppCompatTextView) holder.findViewById(R.id.account_name)).setText(accountName);
//		this.statusImage = (ImageView) holder.findViewById(R.id.account_status);
//	}
	private AppCompatTextView summaryView;

	public AccountCat(Context context) {
		super(context);
		init();
		setLayoutResource(R.layout.preference_account_item);
	}

	public AccountCat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
		setLayoutResource(R.layout.preference_account_item);
	}

	public AccountCat(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
		setLayoutResource(R.layout.preference_account_item);
	}

	public AccountCat(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		setLayoutResource(R.layout.preference_account_item);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		updateStatusImage();
	}

	public XMPPService.DisconnectionCauses getCause() {
		return cause;
	}

	public void setCause(XMPPService.DisconnectionCauses cause) {
		this.cause = cause;
	}

	public Connector.State getState() {
		return state;
	}

	public void setState(Connector.State newState) {
		this.state = newState;
		updateStatusImage();
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder view) {
		this.avatarImage = (StorkAvatarView) view.findViewById(R.id.contact_avatar);
		this.accountNameView = (AppCompatTextView) view.findViewById(R.id.account_name);
//		this.summaryView = (AppCompatTextView) view.findViewById(R.id.summary);

		this.accountNameView.setText(accountName);
		updateStatusImage();

//		try {
//
//
//
//			ImageView avatarView = (ImageView) view.findViewById(R.id.contact_avatar);
//			if (avatarView != null) {
//				final BitmapWorkerTask task = new BitmapWorkerTask(getContext(), avatarView, null);
//
//				final AsyncDrawable asyncDrawable = new AsyncDrawable(getContext().getResources(), mPlaceHolderBitmap,
//																	  task);
//				avatarView.setImageDrawable(asyncDrawable);
//				avatarView.setImageResource(R.drawable.stork_logo);
//				try {
//					task.execute(BareJID.bareJIDInstance(getTitle().toString()));
//				} catch (java.util.concurrent.RejectedExecutionException e) {
//					// ignoring: probably avatar big as cow
//					Log.e("Settings", "Cannot load avatar for account " + getTitle(), e);
//				}
//			}
//
//		} catch (Exception e) {
//			Log.wtf("SettingsActivity", e);
//			setSummary(R.string.account_status_unknown);
//			setIcon(R.drawable.ic_account_disconnected);
//		}

	}

	private void updateStatusImage() {
//		if (summaryView == null || statusImage == null) {
//			return;
//		}
//		if (!active) {
//			summaryView.setText(R.string.account_status_disabled);
//			statusImage.setImageResource(R.drawable.ic_account_disconnected);
//		} else {
//			switch (state) {
//				case connected:
//					summaryView.setText(R.string.account_status_connected);
//					statusImage.setImageResource(R.drawable.ic_account_connected);
//					break;
//				case connecting:
//					summaryView.setText(R.string.account_status_connecting);
//					statusImage.setImageResource(R.drawable.ic_account_connected);
//					break;
//				case disconnecting:
//					summaryView.setText(R.string.account_status_disconnecting);
//					statusImage.setImageResource(R.drawable.ic_account_disconnected);
//					break;
//				default:
//				case disconnected:
//					summaryView.setText(SettingsActivity.getDisconnectedCauseMessage(getContext(), cause));
//					statusImage.setImageResource(R.drawable.ic_account_disconnected);
//					break;
//			}
//		}
	}

	private void init() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getContext().getResources(), R.drawable.stork_logo, options);
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.inJustDecodeBounds = false;
		this.mPlaceHolderBitmap = getCroppedBitmap(
				BitmapFactory.decodeResource(getContext().getResources(), R.drawable.stork_logo, options));
	}
}
