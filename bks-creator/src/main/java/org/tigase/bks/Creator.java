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

package org.tigase.bks;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class Creator {

	private final static char[] PWD = "".toCharArray();

	private final static String KEYSTORE = "trust_store_bks";

	private static ArrayList<X509Certificate> load(File file) throws CertificateException, IOException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		final ArrayList<X509Certificate> result = new ArrayList<>();
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
			X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
			result.add(cert);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		final KeyStore ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME);
		ks.load(null, PWD);

		ClassLoader classLoader = Creator.class.getClassLoader();

		File r = new File(
				"/Users/bmalkow/Documents/workspace/tigase-messenger2/bks-creator/src/main/resources/certificates");

		File[] files = r.listFiles((f, name) -> name.endsWith(".cer") || name.endsWith(".pem"));
		System.out.println(Arrays.toString(files));

		for (File file : files) {
			String alias = file.getName().substring(0, file.getName().length() - 4);
			System.out.print(" * " + alias + "... ");
			ArrayList<X509Certificate> certs = load(file);
			System.out.print(certs.size() + " ");

			System.out.print("(");
			for (int i = 0; i < certs.size(); i++) {
				final X509Certificate cert = certs.get(i);
				if (cert.getIssuerDN().equals(cert.getSubjectDN())) {
					System.out.print((i + 1) + ":ROOT ");
				} else {
					System.out.print((i + 1) + ":CERT ");
				}
				ks.setCertificateEntry(alias + "-" + (i + 1), cert);
			}
			System.out.print(") ");

//
//
//			ks.set

			System.out.println("OK");
		}

//		File file = new File(r.getFile());
//		System.out.println(file);

//
//		CertificateFactory cf = CertificateFactory.getInstance("X.509");
//		try (BufferedInputStream bis = new BufferedInputStream(
//				new FileInputStream("/Users/bmalkow/Downloads/DST Root CA X3.cer"))) {
//			X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
//			System.out.println(cert);
//		}

		// **********
//

		System.out.println(ks.size());

		File outFile = new File(KEYSTORE);
		System.out.println("Save keystore to " + outFile.getAbsolutePath());
		try (FileOutputStream out = new FileOutputStream(outFile)) {
			ks.store(out, PWD);
		}

		System.out.println("Test:");
		test();
	}

	private static void test() throws NoSuchProviderException, KeyStoreException, IOException, CertificateException,
									  NoSuchAlgorithmException, UnrecoverableEntryException {
		KeyStore ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME);
		try (FileInputStream in = new FileInputStream(KEYSTORE)) {
			ks.load(in, PWD);
		}

		Enumeration<String> en = ks.aliases();
		while (en.hasMoreElements()) {
			final String alias = en.nextElement();
			KeyStore.Entry entry = ks.getEntry(alias, null);
			System.out.println(" * " + alias + ": " + entry.getClass().getSimpleName() + "  att=" + entry.getAttributes());
		}
	}

}
