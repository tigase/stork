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

package org.tigase.messenger.phone.pro.service;

import android.util.Log;

import java.util.logging.*;

/**
 * Make JUL work on Android.
 */
public class AndroidLoggingHandler
		extends Handler {

	static int getAndroidLevel(Level level) {
		int value = level.intValue();
		if (value >= 1000) {
			return Log.ERROR;
		} else if (value >= 900) {
			return Log.WARN;
		} else if (value >= 800) {
			return Log.INFO;
		} else {
			return Log.DEBUG;
		}
	}

	public static void reset(Handler rootHandler) {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}
		LogManager.getLogManager().getLogger("").addHandler(rootHandler);
	}

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		if (!super.isLoggable(record)) {
			return;
		}

		String name = record.getLoggerName();
		int maxLength = 30;
		String tag = name.length() > maxLength ? name.substring(name.length() - maxLength) : name;

		try {
			int level = getAndroidLevel(record.getLevel());
			Log.println(level, tag, record.getMessage());
			if (record.getThrown() != null) {
				Log.println(level, tag, Log.getStackTraceString(record.getThrown()));
			}
		} catch (RuntimeException e) {
			Log.e("AndroidLoggingHandler", "Error logging message.", e);
		}
	}
}