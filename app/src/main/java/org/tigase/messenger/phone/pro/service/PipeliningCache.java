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

import android.content.Context;
import android.util.Log;
import org.tigase.messenger.phone.pro.utils.Parser;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;

import java.io.*;
import java.util.ArrayList;

public class PipeliningCache
		implements StreamFeaturesModule.CacheProvider {

	private final static String TAG = "PipeliningCache";
	private final Context context;

	public PipeliningCache(Context context) {
		this.context = context;
	}

	@Override
	public ArrayList<Element> load(final SessionObject sessionObject) {
		final File file = cacheFile(sessionObject);

		if (!file.exists()) {
			return null;
		}

		try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String x = read(reader);
			Element e = Parser.parseElement(x);
			return new ArrayList<Element>(e.getChildren("features"));
		} catch (Exception e) {
			Log.e(TAG, "Cannot read cache file", e);
			return null;
		}
	}

	@Override
	public void save(final SessionObject sessionObject, final ArrayList<Element> features) {
		final File file = cacheFile(sessionObject);
		try (final Writer out = new FileWriter(file)) {
			out.write("<cache>");
			out.write("\n");
			for (Element element : features) {
				try {
					out.write(element.getAsString());
					out.write("\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out.write("</cache>");
		} catch (Exception e) {
			Log.e(TAG, "Cannot store cache file", e);
		}
	}

	private File cacheFile(final SessionObject sessionObject) {
		String domain = sessionObject.getUserBareJid().getDomain();
		File file = new File(context.getApplicationContext().getDir(".", Context.MODE_PRIVATE),
							 "features_" + domain + ".cache.xml");
		Log.d(TAG, "Using cache file " + file);
		return file;
	}

	private String read(BufferedReader reader) throws IOException {
		final StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}
}
