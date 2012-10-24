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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class TrustCertDialog extends DialogFragment {

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	protected static final String TAG = "TrustCertDialog";

	public static char[] encodeHex(byte[] data) {
		int l = data.length;
		char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}
		return out;
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
		final X509Certificate[] chain = cause.getChain();

		final StringBuilder chainInfo = new StringBuilder();
		MessageDigest sha1 = null;

		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
		}

		for (int i = 0; i < chain.length; i++) {
			chainInfo.append(getResources().getString(R.string.trustcert_certchain, i)).append("\n");
			chainInfo.append(
					getResources().getString(R.string.trustcert_subject,
							chain[i].getSubjectX500Principal().getName(X500Principal.CANONICAL).toString())).append("\n");
			chainInfo.append(
					getResources().getString(R.string.trustcert_issuer,
							chain[i].getIssuerX500Principal().getName(X500Principal.CANONICAL).toString())).append("\n");

			if (sha1 != null) {
				sha1.reset();
				try {
					char[] sha1sum = encodeHex(sha1.digest(chain[i].getEncoded()));
					chainInfo.append(getResources().getString(R.string.trustcert_fingerprint, new String(sha1sum))).append("\n");
				} catch (CertificateEncodingException e) {
				}
			}
		}

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.trustcert_dialog_title);

		builder.setMessage(getResources().getString(R.string.trustcert_question, chainInfo));

		builder.setCancelable(true);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				SecureTrustManagerFactory.add(chain);
				reconnectAccount(account);
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
