/*
 * SecureTrustManagerFactory.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
import org.tigase.messenger.phone.pro.R;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SecureTrustManagerFactory {

	private final static char[] DEFAULT_PASSWORD = "Tigase".toCharArray();
	private final static String TAG = "TrustManager";
	private static SecureTrustManagerFactory instance;

	static {
	}

	private final TrustManagerFactory factory;
	private final KeyStore keyStore;
	private X509TrustManager defaultTrustManager;
	private File keyStoreFile;

	public static void addCertificate(Context ctx, X509Certificate crt) {
		initIfRequired(ctx);
		try {
			instance.addTrustKey(new X509Certificate[]{crt});
		} catch (KeyStoreException e) {
			Log.e(TAG, "Can't add certificate to store", e);
		}
	}

	public static TrustManager[] getTrustManagers(Context ctx) {
		initIfRequired(ctx);
		return instance.getManagers(ctx);
	}

	private static void initIfRequired(Context ctx) {
		if (instance != null) {
			return;
		}

		try {
			instance = new SecureTrustManagerFactory();
			instance.init(ctx);
		} catch (Exception e) {
			Log.e(TAG, "Can't initialize TrustManagerFactory!", e);
		}

	}

	private SecureTrustManagerFactory() throws KeyStoreException, NoSuchAlgorithmException {
		String ksType = KeyStore.getDefaultType();
		String mfAlg = TrustManagerFactory.getDefaultAlgorithm();
		Log.d(TAG, "Creating Factory with KeyStore type " + ksType + " and TrustManagert algoritm  " + mfAlg);
		keyStore = KeyStore.getInstance(ksType);
		factory = TrustManagerFactory.getInstance(mfAlg);
	}

	private void addTrustKey(X509Certificate[] chain) throws KeyStoreException {
		for (X509Certificate c : chain) {
			String alias = c.getSubjectDN().toString();
			Log.d(TAG, "Adding certificate " + alias);
			keyStore.setCertificateEntry(alias, c);
		}

		storeKeystore(keyStoreFile);

		factory.init(keyStore);

		for (TrustManager tm : factory.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				defaultTrustManager = (X509TrustManager) tm;
				break;
			}
		}
	}

	private TrustManager[] getManagers(Context ctx) {
		if (defaultTrustManager != null) {
			Log.d(TAG, "Using wrapped TrustManager");
			return new TrustManager[]{new TrustManagerWrapper()};
		} else {
			Log.d(TAG, "Using system TrustManager");
			return factory.getTrustManagers();
		}
	}

	private void init(Context ctx)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		loadKeystore(ctx.getResources().openRawResource(R.raw.trust_store_bks), null);
		loadKeystore(System.getProperty("javax.net.ssl.trustStore"));
		this.keyStoreFile = new File(
				ctx.getApplicationContext().getDir("TrustStore", Context.MODE_PRIVATE) + File.separator +
						"TrustStore.bks");
		loadKeystore(keyStoreFile, DEFAULT_PASSWORD);

		factory.init(keyStore);

		for (TrustManager tm : factory.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				defaultTrustManager = (X509TrustManager) tm;
				break;
			}
		}

		Log.i(TAG, "Factory initialized! (known ca: " + keyStore.size() + ")");
	}

	private void loadKeystore(File file, char[] password) {
		try {
			Log.d(TAG, "Loading keystore from " + file);
			InputStream in = new FileInputStream(file);
			loadKeystore(in, password);
		} catch (Exception e1) {
			Log.w(TAG, "Can't load keystore from file " + file);
		}

	}

	private void loadKeystore(InputStream in, char[] password) {
		try {
			try {
				keyStore.load(in, password);
			} finally {
				in.close();
			}
		} catch (Exception e1) {
			Log.w(TAG, "Can't load keystore from stream");
		}

	}

	private void loadKeystore(String file) {
		try {
			loadKeystore(new File(file), null);
		} catch (NullPointerException e) {
			Log.w(TAG, "Can't load keystore from file " + file);
		}
	}

	private void storeKeystore(File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			try {
				keyStore.store(out, DEFAULT_PASSWORD);
			} finally {
				out.close();
			}
		} catch (Exception e1) {
			Log.w(TAG, "Can't store keystore to file " + file);
		}

	}

	public static class DataCertificateException
			extends CertificateException {

		private static final long serialVersionUID = 1L;

		private X509Certificate[] chain;

		public DataCertificateException(CertificateException e, X509Certificate[] chain, String authType) {
			super(e);
			this.chain = chain;

		}

		public X509Certificate[] getChain() {
			return chain;
		}

		public void setChain(X509Certificate[] chain) {
			this.chain = chain;
		}

	}

	private class TrustManagerWrapper
			implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws DataCertificateException {
			try {
				defaultTrustManager.checkClientTrusted(chain, authType);
			} catch (CertificateException e) {
				throw new DataCertificateException(e, chain, authType);
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws DataCertificateException {
			try {
				defaultTrustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				Log.e(TAG, "certificate validation failed = " + e.getMessage());
				throw new DataCertificateException(e, chain, authType);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			X509Certificate[] result = defaultTrustManager.getAcceptedIssuers();
			return result;
		}
	}
}
