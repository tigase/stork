/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.mobile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.tigase.mobile.security.SecureTrustManagerFactory;
import org.tigase.mobile.security.SecureTrustManagerFactory.DataCertificateException;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class TrustCertDialog extends DialogFragment {

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	protected static final String TAG = "TrustCertDialog";

	public static Dialog createDIalogInstance(final Context context, final String account,
			final DataCertificateException cause, final Resources res, final Runnable actionAfterOK) {
		final X509Certificate[] chain = cause.getChain();

		final StringBuilder chainInfo = new StringBuilder();
		MessageDigest sha1 = null;

		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
		}

		for (int i = 0; i < chain.length; i++) {
			chainInfo.append(res.getString(R.string.trustcert_certchain, i)).append("\n");
			chainInfo.append(
					res.getString(R.string.trustcert_subject,
							chain[i].getSubjectX500Principal().getName(X500Principal.CANONICAL).toString())).append("\n");
			chainInfo.append(
					res.getString(R.string.trustcert_issuer,
							chain[i].getIssuerX500Principal().getName(X500Principal.CANONICAL).toString())).append("\n");

			if (sha1 != null) {
				sha1.reset();
				try {
					char[] sha1sum = encodeHex(sha1.digest(chain[i].getEncoded()));
					chainInfo.append(res.getString(R.string.trustcert_fingerprint, new String(sha1sum))).append("\n");
				} catch (CertificateEncodingException e) {
				}
			}
		}

		Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.trustcert_dialog_title);

		builder.setMessage(res.getString(R.string.trustcert_question, chainInfo));

		builder.setCancelable(true);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				SecureTrustManagerFactory.add(chain);
				if (actionAfterOK != null)
					actionAfterOK.run();
			}
		});
		builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		return builder.create();
	}

	/*
	 * new AlertDialog.Builder(AccountSetupCheckSettings.this)
	 * .setTitle(getString
	 * (R.string.account_setup_failed_dlg_invalid_certificate_title))
	 * //.setMessage
	 * (getString(R.string.account_setup_failed_dlg_invalid_certificate)
	 * .setMessage(getString(msgResId, exMessage) + " " + chainInfo.toString() )
	 * .setCancelable(true) .setPositiveButton(
	 * getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
	 * new DialogInterface.OnClickListener() { public void
	 * onClick(DialogInterface dialog, int which) { try { String alias =
	 * mAccount.getUuid(); if (mCheckIncoming) { alias = alias + ".incoming"; }
	 * if (mCheckOutgoing) { alias = alias + ".outgoing"; }
	 * TrustManagerFactory.addCertificateChain(alias, chain); } catch
	 * (CertificateException e) { showErrorDialog(
	 * R.string.account_setup_failed_dlg_certificate_message_fmt, e.getMessage()
	 * == null ? "" : e.getMessage()); }
	 * AccountSetupCheckSettings.actionCheckSettings
	 * (AccountSetupCheckSettings.this, mAccount, mCheckIncoming,
	 * mCheckOutgoing); } }) .setNegativeButton(
	 * getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
	 * new DialogInterface.OnClickListener() { public void
	 * onClick(DialogInterface dialog, int which) { finish(); } }) .show();
	 */

	public static char[] encodeHex(byte[] data) {
		int l = data.length;
		char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}
		return out;
	}

	public static TrustCertDialog newInstance(String account, DataCertificateException cause) {
		TrustCertDialog frag = new TrustCertDialog();
		Bundle args = new Bundle();
		args.putString("account", account);
		args.putSerializable("cause", cause);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String account = getArguments().getString("account");
		final DataCertificateException cause = (DataCertificateException) getArguments().getSerializable("cause");

		return createDIalogInstance(getActivity(), account, cause, getResources(), new Runnable() {

			@Override
			public void run() {
				reconnectAccount(account);
			}
		});
	}

	private void reconnectAccount(final String account) {
		final JaxmppCore jaxmpp = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().get(
				BareJID.bareJIDInstance(account));
		new Thread() {
			@Override
			public void run() {
				try {
					JaxmppService.disable(jaxmpp.getSessionObject(), false);
					jaxmpp.login();
				} catch (JaxmppException ex) {
					Log.e(TAG, "error manually connecting account " + jaxmpp.getSessionObject().getUserBareJid().toString(), ex);
				}
			}
		}.start();
	}

}
