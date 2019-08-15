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

package org.tigase.messenger.phone.pro.account;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import org.tigase.messenger.phone.pro.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class CertificateDialogBuilder {

	private final static String TAG = "CertificateDialog";
	private final AlertDialog.Builder builder;
	private final X509Certificate[] chain;
	private final Context context;
	private CharSequence message;

	private static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		StringBuilder result = new StringBuilder();
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			result.append(hexArray[v >>> 4]);
			result.append(hexArray[v & 0x0F]);
			if (j < bytes.length) {
				result.append(":");
			}
		}
		return result.toString();
	}

	public CertificateDialogBuilder(@NonNull Context context, X509Certificate[] chain) {
		this.context = context;
		this.chain = chain;
		this.builder = new AlertDialog.Builder(context);

	}

	public AlertDialog create() {
		String cd = buildCertDetails();

		if (this.message != null) {
			builder.setMessage(this.message + " " + cd);
		} else {
			builder.setMessage(cd);
		}

		return builder.create();
	}

	public AlertDialog.Builder setMessage(int messageId) {
		this.message = context.getText(messageId);
		return builder.setMessage(this.message);
	}

	public AlertDialog.Builder setMessage(@Nullable CharSequence message) {
		this.message = message;
		return builder.setMessage(message);
	}

	public AlertDialog.Builder setTitle(String string) {
		builder.setTitle(string);
		return builder;
	}

	private String buildCertDetails() {
		StringBuilder msg = new StringBuilder(100);

		MessageDigest sha1 = null;
		MessageDigest md5 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			Log.wtf(TAG, "SHA1 should be here!", e);
		}
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.wtf(TAG, "MD5 should be here!", e);
		}

		for (int i = 0; i < chain.length; i++) {
			msg.append(context.getString(R.string.account_certificate_info_chain, String.valueOf(i)));
			msg.append(
					context.getString(R.string.account_certificate_info_subject, chain[i].getSubjectDN().toString()));
			msg.append(context.getString(R.string.account_certificate_info_issuer, chain[i].getIssuerDN().toString()));
			if (sha1 != null) {
				sha1.reset();
				try {
					msg.append(context.getString(R.string.account_certificate_info_fingerprint_sha,
												 bytesToHex(sha1.digest(chain[i].getEncoded()))));

				} catch (CertificateEncodingException e) {
					Log.e(TAG, "Cannot add SHA1 to info", e);
				}
			}
			if (md5 != null) {
				md5.reset();
				try {
					msg.append(context.getString(R.string.account_certificate_info_fingerprint_md5,
												 bytesToHex(md5.digest(chain[i].getEncoded()))));

				} catch (CertificateEncodingException e) {
					Log.e(TAG, "Cannot add MD5 to info", e);
				}
			}
		}
		return msg.toString();
	}

}
