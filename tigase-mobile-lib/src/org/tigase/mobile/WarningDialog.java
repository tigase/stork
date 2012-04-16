package org.tigase.mobile;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class WarningDialog extends DialogFragment {

	public static WarningDialog newInstance(final int messageId) {
		WarningDialog frag = new WarningDialog();
		Bundle args = new Bundle();
		args.putInt("messageId", messageId);
		frag.setArguments(args);
		return frag;
	}

	public static WarningDialog newInstance(final String message) {
		WarningDialog frag = new WarningDialog();
		Bundle args = new Bundle();
		args.putString("message", message);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String message = getArguments().getString("message");
		final int messageId = getArguments().getInt("messageId", -1);

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning_dialog_title);
		if (messageId != -1) {
			builder.setMessage(messageId);
		} else {
			builder.setMessage(message);
		}
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		return builder.create();
	}
}
