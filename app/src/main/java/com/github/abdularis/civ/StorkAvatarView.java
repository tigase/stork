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
package com.github.abdularis.civ;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class StorkAvatarView
		extends AvatarImageView {

	protected static LruCache<BareJID, Bitmap> memCache = memCache = new LruCache<>(1024 * 1024 * 10);

	@DrawableRes
	private Integer statusResource = null;

	public StorkAvatarView(Context context) {
		super(context);
		initialize();
	}

	public StorkAvatarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	@Override
	public void setText(@Nullable String text) {
		super.setText(text == null ? null : text.toUpperCase());
	}

	public void setJID(final BareJID jid, final String name) {
		Bitmap avatar = getAvatar(jid);
		setAvatarBackgroundColor(AvatarHelper.getAvatarBackgroundColor(jid));
		setText(AvatarHelper.getInitials(jid, name));
		if (avatar != null) {
			setImageBitmap(avatar);
			setState(AvatarImageView.SHOW_IMAGE);
		} else {
			setState(AvatarImageView.SHOW_INITIAL);
		}
	}

	public void setJID(final BareJID jid, final String name, @DrawableRes final int statusDrawableId) {
		this.statusResource = statusDrawableId;
		setJID(jid, name);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (statusResource != null) {
			Drawable dr = getResources().getDrawable(statusResource);
			int dh = getHeight() / (48 / 14);
			int dw = getWidth() / (48 / 14);
			int marginX = 3;
			int marginY = 3;
			dr.setBounds(getWidth() - dw - marginX, getHeight() - dh - marginY, getWidth() - marginX,
						 getHeight() - marginY);
			dr.draw(canvas);
		}
	}

	private void initialize() {
		setAvatarBackgroundColor(0xff607D8B);
		setStrokeWidth(1);
		setStrokeColor(0xffcdcdcd);
	}

	private Bitmap getAvatar(final BareJID jid) {
		Bitmap result = memCache.get(jid);
		if (result != null) {
			return result;
		}
		result = AvatarHelper.getAvatar(getContext(), jid, true);
		if (result != null) {
			memCache.put(jid, result);
		}
		return result;
	}

}
