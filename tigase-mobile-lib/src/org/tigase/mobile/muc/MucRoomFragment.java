package org.tigase.mobile.muc;

import org.tigase.mobile.FragmentWithUID;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.chat.ChatView;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.providers.ChatHistoryProvider;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MucRoomFragment extends FragmentWithUID implements LoaderCallbacks<Cursor> {

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

	private Button sendButton;

	private ImageView stateImage;

	private View view;

	public MucRoomFragment() {
		this.mucListener = new Listener<MucModule.MucEvent>() {

			@Override
			public void handleEvent(MucEvent be) throws JaxmppException {
				onMucEvent(be);
			}
		};
	}

	void cancelEdit() {
		if (ed == null)
			return;
		final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

		ed.post(new Runnable() {

			@Override
			public void run() {
				ed.clearComposingText();
				imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
			}
		});

	}

	public Room getRoom() {
		return room;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			long id = getArguments().getLong("roomId");
			MultiJaxmpp multi = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp();
			ChatWrapper ch = multi.getRoomById(id);

			if (ch == null) {
				String msg = ChatHistoryFragment.prepareAdditionalDebug(multi);
				Log.v(TAG, "ChatWrapper is null with id = " + id + '\n' + msg);
				((TigaseMobileMessengerActivity) getActivity()).viewPager.getAdapter().notifyDataSetChanged();
			} else {
				if (ch.getRoom() == null) {
					throw new NullPointerException("ChatWrapper.getRoom() is null with id = " + id);
				}
				if (ch.getRoom().getSessionObject() == null) {
					throw new NullPointerException("ChatWrapper.getRoom().getSessionObject() is null with id = " + id);
				}

				this.room = ch.getRoom();
			}
		}

		this.mucAdapter = new MucAdapter(getActivity(), R.layout.muc_chat_item, room);
		getLoaderManager().initLoader(fragmentUID, null, this);
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

		TextView title = (TextView) view.findViewById(R.id.textView1);
		if (title != null) {
			title.setText(room.getRoomJid().toString());
		}
		ed.setEnabled(room.getState() == State.joined);
		sendButton.setEnabled(room.getState() == State.joined);
		lv.setAdapter(mucAdapter);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TigaseMobileMessengerActivity.SHOW_OCCUPANTS && resultCode == Activity.RESULT_OK) {
			String n = data.getStringExtra("nickname");
			if (n != null) {
				String ttt = ed.getText().toString();
				if (ttt == null || ttt.length() == 0) {
					ed.append(n + ": ");
				} else {
					ed.append(" " + n);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		this.setHasOptionsMenu(true);

		this.prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
		jaxmpp.addListener(MucModule.StateChange, this.mucListener);

	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(), Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ room.getRoomJid()), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.muc_main_menu, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.view = inflater.inflate(R.layout.muc_conversation, container, false);

		this.stateImage = (ImageView) view.findViewById(R.id.user_presence);
		this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);

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
		this.ed.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					cancelEdit();
			}
		});

		this.sendButton = (Button) view.findViewById(R.id.chat_send_button);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		this.lv = (ListView) view.findViewById(R.id.chat_conversation_history);

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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.showOccupantsButton) {
			Intent chatListActivity = new Intent(getActivity(), OccupantsListActivity.class);
			chatListActivity.putExtra("roomId", room.getId());
			chatListActivity.putExtra("roomJid", room.getRoomJid().toString());
			chatListActivity.putExtra("account", room.getSessionObject().getUserBareJid().toString());

			this.startActivityForResult(chatListActivity, TigaseMobileMessengerActivity.SHOW_OCCUPANTS);
		} else if (item.getItemId() == R.id.showChatsButton) {
			Intent chatListActivity = new Intent(getActivity(), ChatListActivity.class);
			this.getActivity().startActivityForResult(chatListActivity, TigaseMobileMessengerActivity.REQUEST_CHAT);
		} else if (item.getItemId() == R.id.closeChatButton) {
			cancelEdit();

			final ViewPager viewPager = ((TigaseMobileMessengerActivity) this.getActivity()).viewPager;
			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
					room.getSessionObject());
			final MucModule cm = jaxmpp.getModule(MucModule.class);

			viewPager.setCurrentItem(1);
			AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					try {
						cm.leave(room);
					} catch (JaxmppException e) {
						Log.w(TAG, "Chat close problem!", e);
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void param) {
					viewPager.setCurrentItem(1);
				}
			};

			t.execute();
		}
		return true;
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
		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				Log.i(TAG, "STATE: " + room.getState());

				if (ed != null) {
					if (room.getState() == State.joined && !ed.isEnabled()) {
						ed.setEnabled(true);
						sendButton.setEnabled(true);
					} else if (room.getState() != State.joined && ed.isEnabled()) {
						ed.setEnabled(false);
						sendButton.setEnabled(false);
					}
				}
				System.out.println();
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

				TigaseMobileMessengerActivity activity = ((TigaseMobileMessengerActivity) getActivity());
				if (activity != null && activity.helper != null && room != null) {
					activity.helper.updateActionBar(room.hashCode());
				}
			}
		};
		if (view != null) {
			view.post(r);
		}
	}
}
