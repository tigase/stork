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