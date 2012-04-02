package org.tigase.mobile.chat;

import java.util.Date;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChatView extends LinearLayout {

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	public static Integer getResourceImage(final Presence presence, CapabilitiesModule capabilitiesModule, String nodeName)
			throws XMLException {
		if (presence == null)
			return null;
		Element c = presence.getChildrenNS("c", "http://jabber.org/protocol/caps");
		if (c == null)
			return null;
		final String node = c.getAttribute("node");
		final String ver = c.getAttribute("ver");
		final Identity id = capabilitiesModule.getCache().getIdentity(node + "#" + ver);

		if (id == null)
			return null;

		String cc = id.getCategory() + "/" + id.getType();

		if (node != null && node.equals("http://gajim.org")) {
			return null;
		} else if (node != null && node.equals("http://telepathy.freedesktop.org/caps")) {
			return null;
		} else if (id.getName() != null && id.getName().equalsIgnoreCase("kopete")) {
			return R.drawable.client_kopete;
		} else if (id.getName() != null && id.getName().equalsIgnoreCase("Google Talk User Account")) {
			return R.drawable.client_gtalk;
		} else if (id.getName() != null && id.getName().equalsIgnoreCase("adium")) {
			return R.drawable.client_adium;
		} else if (id.getName() != null && id.getName().equalsIgnoreCase("psi")) {
			return R.drawable.client_psi;
		} else if (nodeName != null && cc.equals("client/phone") && node.equals(nodeName)) {
			return R.drawable.client_messenger;
		} else if (cc.equals("client/phone")) {
			return R.drawable.client_mobile;
		} else
			return null;
	}

	private Chat chat;

	private EditText ed;

	private final SharedPreferences prefs;

	public ChatView(Context context) {
		super(context);
		this.prefs = getContext().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
	}

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.prefs = getContext().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
	}

	public Chat getChat() {
		return chat;
	}

	void init() {
		if (DEBUG)
			Log.i(TAG, "Zrobione");

		this.ed = (EditText) findViewById(R.id.chat_message_entry);
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

		final Button b = (Button) findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		final ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});
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
				int state;
				try {
					chat.sendMessage(t);
					state = ChatTableMetaData.STATE_OUT_SENT;
				} catch (Exception e) {
					state = ChatTableMetaData.STATE_OUT_NOT_SENT;
					Log.e(TAG, e.getMessage(), e);
				}
				// dbHelper.addChatHistory(1, chat, t);

				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
				values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
				values.put(ChatTableMetaData.FIELD_BODY, t);
				values.put(ChatTableMetaData.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
				values.put(ChatTableMetaData.FIELD_STATE, state);

				getContext().getContentResolver().insert(uri, values);

				return null;
			}
		};
		task.execute(t);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;
		TextView t = (TextView) findViewById(R.id.textView1);
		JaxmppCore jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get(
				chat.getSessionObject());

		if (jaxmpp == null)
			throw new RuntimeException("Account " + chat.getSessionObject().getUserBareJid() + " is unknown!");

		RosterItem ri = jaxmpp.getRoster().get(chat.getJid().getBareJid());
		t.setText("Chat with "
				+ (ri == null ? chat.getJid().getBareJid().toString()
						: (new RosterDisplayTools(getContext())).getDisplayName(ri)));
	}

	public void setImagePresence(final CPresence cp) {
		final ImageView itemPresence = (ImageView) findViewById(R.id.user_presence);

		itemPresence.post(new Runnable() {

			@Override
			public void run() {
				if (cp == null)
					itemPresence.setImageResource(R.drawable.user_offline);
				else
					switch (cp) {
					case chat:
						itemPresence.setImageResource(R.drawable.user_free_for_chat);
						break;
					case online:
						itemPresence.setImageResource(R.drawable.user_available);
						break;
					case away:
						itemPresence.setImageResource(R.drawable.user_away);
						break;
					case xa:
						itemPresence.setImageResource(R.drawable.user_extended_away);
						break;
					case dnd:
						itemPresence.setImageResource(R.drawable.user_busy);
						break;
					case requested:
						itemPresence.setImageResource(R.drawable.user_ask);
						break;
					case error:
						itemPresence.setImageResource(R.drawable.user_error);
						break;
					case offline_nonauth:
						itemPresence.setImageResource(R.drawable.user_noauth);
						break;
					default:
						itemPresence.setImageResource(R.drawable.user_offline);
						break;
					}
			}
		});

	}

	public void updateClientIndicator() {
		final ImageView clientTypeIndicator = (ImageView) findViewById(R.id.client_type_indicator);
		clientTypeIndicator.setVisibility(View.INVISIBLE);
		if (chat != null) {
			try {
				final String nodeName = chat.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
				JID jid = chat.getJid();
				final Presence p = chat.getSessionObject().getPresence().getPresence(jid);
				final CapabilitiesModule capabilitiesModule = ((MessengerApplication) (getContext().getApplicationContext())).getMultiJaxmpp().get(
						chat.getSessionObject()).getModulesManager().getModule(CapabilitiesModule.class);

				final Integer pp = getResourceImage(p, capabilitiesModule, nodeName);

				if (pp != null) {
					Runnable r = new Runnable() {

						@Override
						public void run() {
							clientTypeIndicator.setImageResource(pp);
							clientTypeIndicator.setVisibility(View.VISIBLE);
						}
					};

					clientTypeIndicator.post(r);

				}
			} catch (Exception e) {
			}
		}
	}

}
