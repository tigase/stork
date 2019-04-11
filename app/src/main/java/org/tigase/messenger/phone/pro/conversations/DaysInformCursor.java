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
package org.tigase.messenger.phone.pro.conversations;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.format.DateUtils;
import android.util.Log;
import org.tigase.messenger.phone.pro.db.DatabaseContract;

public class DaysInformCursor
		extends MatrixCursor {

	public final static int ITEM_TYPE_DAY_INFO = 0x4000 | 1;

	public static final String TAG = "DaysInformCursor";
	private final String timestampColumn;

//	public static Cursor build(Cursor orig){}

	public DaysInformCursor(final ContentResolver contentResolver, final Cursor cursor, final String timestampColumn) {
		super(cursor.getColumnNames());
		this.timestampColumn = timestampColumn;
		copyDataFromCursor(cursor);
		setNotificationUri(contentResolver, cursor.getNotificationUri());
	}

	private void copyCurrentRow(final Cursor cursor) {
		final MatrixCursor.RowBuilder row = newRow();
		for (int i = 0; i < getColumnCount(); i++) {
			Object v;
			int type = cursor.getType(i);
			switch (type) {
				case Cursor.FIELD_TYPE_NULL:
					v = null;
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					v = cursor.getLong(i);
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					v = cursor.getFloat(i);
					break;
				case Cursor.FIELD_TYPE_STRING:
					v = cursor.getString(i);
					break;
				case Cursor.FIELD_TYPE_BLOB:
					v = cursor.getBlob(i);
					break;
				default:
					Log.wtf(TAG, "Unsupported field type " + type);
					throw new RuntimeException("Unsupported field type " + type);
			}
			row.add(v);
		}
	}

	private void copyDataFromCursor(final Cursor cursor) {
		final long now = System.currentTimeMillis();
		final int dtCol = cursor.getColumnIndex(timestampColumn);

		long dtLastIdx = 0;
		long dtLast = 0;

		while (cursor.moveToNext()) {
			long dtCur = cursor.getLong(dtCol);
			int days = (int) ((dtCur / (1000 * 60 * 60 * 24)));

			CharSequence t = DateUtils.getRelativeTimeSpanString(dtLast, now, DateUtils.DAY_IN_MILLIS);

			if (dtLastIdx != 0 && dtLastIdx != days) {
				newRow().add(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, ITEM_TYPE_DAY_INFO)
						.add(DatabaseContract.ChatHistory.FIELD_BODY, t)
						.add(DatabaseContract.ChatHistory.FIELD_ID, -1)
						.add(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, dtLast);
			}
			dtLastIdx = days;
			dtLast = dtCur;

			copyCurrentRow(cursor);
		}
	}

}
