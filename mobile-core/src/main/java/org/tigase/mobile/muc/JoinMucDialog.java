package org.tigase.mobile.muc;

import java.util.ArrayList;

import org.tigase.mobile.Constants;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.bookmarks.BookmarksActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class JoinMucDialog extends DialogFragment {

	public static JoinMucDialog newInstance() {
		Bundle args = new Bundle();
		return newInstance(args);
	}

	public static JoinMucDialog newInstance(Bundle args) {
		JoinMucDialog frag = new JoinMucDialog();
		frag.setArguments(args);
		return frag;
	}

	private AsyncTask<Room, Void, Void> task;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Dialog dialog = new Dialog(getActivity());
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		dialog.setContentView(R.layout.join_room_dialog);
		dialog.setTitle(getString(R.string.aboutButton));

		ArrayList<String> accounts = new ArrayList<String>();
		for (Account account : AccountManager.get(getActivity()).getAccountsByType(Constants.ACCOUNT_TYPE)) {
			accounts.add(account.name);
		}

		final Spinner accountSelector = (Spinner) dialog.findViewById(R.id.muc_accountSelector);
		final Button joinButton = (Button) dialog.findViewById(R.id.muc_joinButton);
		final Button cancelButton = (Button) dialog.findViewById(R.id.muc_cancelButton);
		final TextView name = (TextView) dialog.findViewById(R.id.muc_name);
		final TextView roomName = (TextView) dialog.findViewById(R.id.muc_roomName);
		final TextView mucServer = (TextView) dialog.findViewById(R.id.muc_server);
		final TextView nickname = (TextView) dialog.findViewById(R.id.muc_nickname);
		final TextView password = (TextView) dialog.findViewById(R.id.muc_password);
		final CheckBox autojoin = (CheckBox) dialog.findViewById(R.id.muc_autojoin);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item,
				accounts.toArray(new String[] {}));
		accountSelector.setAdapter(adapter);

		Bundle data = getArguments();
		final boolean editMode = data != null && data.containsKey("editMode") && data.getBoolean("editMode");
		final String id = data != null ? data.getString("id") : null;

		if (data != null) {
			accountSelector.setSelection(adapter.getPosition(data.getString("account")));

			name.setText(data.getString("name"));
			roomName.setText(data.getString("room"));
			mucServer.setText(data.getString("server"));
			nickname.setText(data.getString("nick"));
			password.setText(data.getString("password"));
			autojoin.setChecked(data.getBoolean("autojoin"));
		}

		if (!editMode) {
			name.setVisibility(View.GONE);
			autojoin.setVisibility(View.GONE);
		} else {
			joinButton.setText("Save");
		}

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

			}
		});
		joinButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (editMode) {

					BareJID account = BareJID.bareJIDInstance(accountSelector.getSelectedItem().toString());
					final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
							account);

					Bundle data = new Bundle();

					data.putString("id", id);
					data.putString("account", account.toString());
					data.putString("name", name.getText().toString());
					data.putString("room", roomName.getText().toString());
					data.putString("server", mucServer.getText().toString());
					data.putString("nick", nickname.getText().toString());
					data.putString("password", password.getText().toString());
					data.putBoolean("autojoin", autojoin.isChecked());

					((BookmarksActivity) getActivity()).saveItem(data);

					dialog.dismiss();
					return;
				}

				BareJID account = BareJID.bareJIDInstance(accountSelector.getSelectedItem().toString());
				final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
						account);

				Runnable r = new Runnable() {

					@Override
					public void run() {
						try {
							Room room = jaxmpp.getModule(MucModule.class).join(roomName.getEditableText().toString(),
									mucServer.getEditableText().toString(), nickname.getEditableText().toString(),
									password.getEditableText().toString());
							if (task != null)
								task.execute(room);
						} catch (Exception e) {
							Log.w("MUC", "", e);
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				(new Thread(r)).start();
				dialog.dismiss();
			}
		});

		return dialog;
	}

	public void setAsyncTask(AsyncTask<Room, Void, Void> r) {
		this.task = r;
	}
}
