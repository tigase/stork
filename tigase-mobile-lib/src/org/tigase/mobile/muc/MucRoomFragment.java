package org.tigase.mobile.muc;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.chat.ChatView;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MucRoomFragment extends Fragment implements LoaderCallbacks<Cursor> {

	private static final boolean DEBUG = false;

	private static final String TAG = "MUC";

	public static Fragment newInstance(String account, long roomId) {
		MucRoomFragment f = new MucRoomFragment();

		Bundle args = new Bundle();
		args.putLong("roomId", roomId);
		args.putString("account", account);
		f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating MucRoomFragment id=" + roomId);

		return f;
	}

	private EditText ed;

	private ChatView layout;

	private ListView lv;

	private MucAdapter mucAdapter;

	private Listener<MucEvent> mucListener;

	private SharedPreferences prefs;

	private ProgressBar progressBar;

	private Room room;

	private ImageView stateImage;

	public MucRoomFragment() {
		this.mucListener = new Listener<MucModule.MucEvent>() {

			@Override
			public void handleEvent(MucEvent be) throws JaxmppException {
				onMucEvent(be);
			}
		};
	}

	public Room getRoom() {
		return room;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
		jaxmpp.addListener(MucModule.StateChange, this.mucListener);

		if (getArguments() != null) {
			long id = getArguments().getLong("roomId");
			ChatWrapper ch = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().getRoomById(id);
			this.room = ch.getRoom();
		}

		this.mucAdapter = new MucAdapter(getActivity().getApplicationContext(), R.layout.muc_chat_item, room);
		getLoaderManager().initLoader(0, null, this);
		mucAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				if (DEBUG)
					Log.i(TAG, "Changed!");
				if (lv != null)
					lv.post(new Runnable() {

						@Override
						public void run() {
							lv.setSelection(Integer.MAX_VALUE);
						}
					});
			}
		});
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(), Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ room.getRoomJid()), null, null, null, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.muc_conversation, container, false);

		this.stateImage = (ImageView) view.findViewById(R.id.user_presence);
		this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);

		TextView title = (TextView) view.findViewById(R.id.textView1);
		if (title != null) {
			title.setText(room.getRoomJid().toString());
		}

		this.ed = (EditText) view.findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = prefs.getBoolean(Preferences.ENTER_TO_SEND_KEY, true);
				if (ets && keyCode == KeyEvent.KEYCODE_ENTER) {
					sendMessage();
					return true;
				}
				return false;
			}
		});

		final Button b = (Button) view.findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		this.lv = (ListView) view.findViewById(R.id.chat_conversation_history);

		lv.setAdapter(mucAdapter);

		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

		return view;
	}

	@Override
	public void onDestroy() {
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
		jaxmpp.removeListener(MucModule.StateChange, this.mucListener);

		super.onDestroy();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mucAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mucAdapter.swapCursor(cursor);
	}

	protected void onMucEvent(MucEvent be) {
		updatePresenceImage();
	}

	@Override
	public void onResume() {
		super.onResume();
		updatePresenceImage();
	}

	protected void sendMessage() {
		if (ed == null)
			return;

		String t = ed.getText().toString();
		ed.setText("");

		if (t == null || t.length() == 0)
			return;
		if (DEBUG)
			Log.d(TAG, "Send: " + t);

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			public Void doInBackground(String... ts) {
				String t = ts[0];
				Log.d(TAG, "Send: " + t);
				try {
					room.sendMessage(t);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}

				return null;
			}
		};
		task.execute(t);
		((MessengerApplication) getActivity().getApplicationContext()).getTracker().trackEvent("MucView", // Category
				"Message", // Action
				"Send", // Label
				0);
		updatePresenceImage();
	}

	private void updatePresenceImage() {
		if (stateImage != null) {
			stateImage.post(new Runnable() {

				@Override
				public void run() {
					if (room.getState() == State.not_joined) {
						progressBar.setVisibility(View.GONE);
						stateImage.setImageResource(R.drawable.user_offline);
					} else if (room.getState() == State.requested) {
						progressBar.setVisibility(View.VISIBLE);
						stateImage.setVisibility(View.GONE);
					} else if (room.getState() == State.joined) {
						progressBar.setVisibility(View.GONE);
						stateImage.setImageResource(R.drawable.user_available);
					}
				}
			});
		}
	}
}
