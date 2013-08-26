/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile;

import org.tigase.mobile.roster.RosterFragment;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public abstract class MyFragmentPageAdapter extends FragmentStatePagerAdapter {

	private static final boolean DEBUG = false;

	private static final String TAG = "MyFragmentPageAdapter";

	private Fragment mCurrentPrimaryItem = null;

	private FragmentTransaction mCurTransaction = null;
	private final FragmentManager mFragmentManager;
	protected boolean refreshRoster;

	public MyFragmentPageAdapter(FragmentManager fm) {
		super(fm);
		this.mFragmentManager = fm;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		if (DEBUG)
			Log.v(TAG, "Detaching item #" + position + ": f=" + object + " v=" + ((Fragment) object).getView());
		mCurTransaction.detach((Fragment) object);
		mCurTransaction.remove((Fragment) object);
	}

	@Override
	public void finishUpdate(ViewGroup container) {
		if (mCurTransaction != null) {
			mCurTransaction.commitAllowingStateLoss();
			mCurTransaction = null;
			mFragmentManager.executePendingTransactions();
		}
	}

	/**
	 * Return the Fragment associated with a specified position.
	 */
	@Override
	public abstract Fragment getItem(int position);

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}

		// Do we already have this fragment?
		String name = makeFragmentName(container.getId(), position);
		Fragment fragment = mFragmentManager.findFragmentByTag(name);

		if (refreshRoster && fragment instanceof RosterFragment) {
			refreshRoster = false;
			mCurTransaction.detach(fragment);
			fragment = null;
		}

		if (fragment != null) {
			if (DEBUG)
				Log.v(TAG, "Attaching item #" + position + ": f=" + fragment);
			mCurTransaction.attach(fragment);
		} else {
			fragment = getItem(position);
			if (DEBUG)
				Log.v(TAG, "Adding item #" + position + ": f=" + fragment);
			int containerId = container.getId();
			String fragmentName = makeFragmentName(container.getId(), position);
			mCurTransaction.add(containerId, fragment, fragmentName);
		}
		if (fragment != mCurrentPrimaryItem) {
			fragment.setMenuVisibility(false);
			fragment.setUserVisibleHint(false);
		}

		return fragment;
	}

	public boolean isRefreshRoster() {
		return refreshRoster;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return ((Fragment) object).getView() == view;
	}

	protected String makeFragmentName(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		Fragment fragment = (Fragment) object;
		if (fragment != mCurrentPrimaryItem) {
			if (mCurrentPrimaryItem != null) {
				mCurrentPrimaryItem.setMenuVisibility(false);
				mCurrentPrimaryItem.setUserVisibleHint(false);
			}
			if (fragment != null) {
				fragment.setMenuVisibility(true);
				fragment.setUserVisibleHint(true);
			}
			mCurrentPrimaryItem = fragment;
		}
	}

	public void setRefreshRoster(boolean refreshRoster) {
		this.refreshRoster = refreshRoster;
	}

	@Override
	public void startUpdate(ViewGroup container) {
	}
}
