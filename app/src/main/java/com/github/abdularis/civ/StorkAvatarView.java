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
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.BareJID;

public class StorkAvatarView
		extends AvatarImageView {

	private final static int[] COLORS = {0xf44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
										 0x009688, 0x4CAF50, 0x8BC34A, 0xFF9800, 0xFF5722, 0x795548, 0x9E9E9E,
										 0x607D8B};
	protected static LruCache<BareJID, Bitmap> memCache = memCache = new LruCache<>(1024 * 1024 * 10);

	public StorkAvatarView(Context context) {
		super(context);
		initialize();
	}

	public StorkAvatarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	private void initialize(){
		setAvatarBackgroundColor(0xff607D8B);
		setStrokeWidth(1);
		setStrokeColor(0xffcdcdcd);
	}

	@Override
	public void setText(@Nullable String text) {
		super.setText(text == null ? null : text.toUpperCase());
	}

	public void setJID(final BareJID jid, final String name) {
		Bitmap avatar = getAvatar(jid);
		setAvatarBackgroundColor(0xff000000 | COLORS[Math.abs(jid.hashCode()) % COLORS.length]);
		String normalizedName = name.replaceAll("[^a-zA-Z0-9]", "");
		setText(name == normalizedName || normalizedName.isEmpty() ? jid.toString() : normalizedName);
		if (avatar != null) {
			setImageBitmap(avatar);
			setState(AvatarImageView.SHOW_IMAGE);
		} else {
			setState(AvatarImageView.SHOW_INITIAL);
		}
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
