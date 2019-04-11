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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class OnlyVerticalSwipeRefreshLayout
		extends SwipeRefreshLayout {

	private boolean declined;
	private float prevX;
	private int touchSlop;

	public OnlyVerticalSwipeRefreshLayout(Context context) {
		super(context);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	public OnlyVerticalSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				prevX = MotionEvent.obtain(event).getX();
				declined = false; // New action
				break;

			case MotionEvent.ACTION_MOVE:
				final float eventX = event.getX();
				float xDiff = Math.abs(eventX - prevX);
				if (declined || xDiff > touchSlop) {
					declined = true; // Memorize
					return false;
				}
				break;
		}
		return super.onInterceptTouchEvent(event);
	}
}