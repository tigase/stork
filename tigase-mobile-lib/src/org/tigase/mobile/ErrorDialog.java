package org.tigase.mobile;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ErrorDialog extends DialogFragment {

	public static ErrorDialog newInstance(final String account, final String message) {
		ErrorDialog frag = new ErrorDialog();
		Bundle args = new Bundle();
		args.putString("account", account);
		args.putString("message", message);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String account = getArguments().getString("account");
		final String message = getArguments().getString("message");

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("Error");

		builder.setMessage("Account: " + account + '\n' + '\n' + message);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		return builder.create();
	}
}
