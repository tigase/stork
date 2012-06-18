package org.tigase.mobile.roster;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class FlatRosterAdapter extends SimpleCursorAdapter {
	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE };
	private final static int[] names = new int[] { R.id.roster_item_jid };
	protected int[] mFrom;

	public FlatRosterAdapter(Context context, Cursor c) {
		super(context, R.layout.roster_item, c, cols, names);
			    
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		// super.bindView(view, context, cursor);

		TextView itemJid = (TextView) view.findViewById(R.id.roster_item_jid);
		TextView itemDescription = (TextView) view.findViewById(R.id.roster_item_description);
		itemJid.setTransformationMethod(SingleLineTransformationMethod.getInstance());

		View openChatNotifier = view.findViewById(R.id.openChatNotifier);

		ImageView itemAvatar = (ImageView) view.findViewById(R.id.imageView1);
		ImageView itemPresence = (ImageView) view.findViewById(R.id.roster_item_precence);

		String name = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_DISPLAY_NAME));
		itemJid.setText(name);

		BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

		final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
		final Jaxmpp jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(account);

		final ImageView clientTypeIndicator = (ImageView) view.findViewById(R.id.client_type_indicator);
		clientTypeIndicator.setVisibility(View.INVISIBLE);

		CapabilitiesModule capabilitiesModule = jaxmpp.getModulesManager().getModule(CapabilitiesModule.class);
		try {
			final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
			for (Presence p : jaxmpp.getPresence().getPresences(jid).values()) {
				if (p.getType() != null)
					continue;
				Element c = p.getChildrenNS("c", "http://jabber.org/protocol/caps");
				if (c == null)
					continue;
				String node = c.getAttribute("node");
				String ver = c.getAttribute("ver");

				Identity id = capabilitiesModule.getCache().getIdentity(node + "#" + ver);
				if (id != null) {
					String tmp = id.getCategory() + "/" + id.getType();
					if (tmp.equals("client/phone") && node.equals(nodeName)) {
						clientTypeIndicator.setImageResource(R.drawable.client_messenger);
						clientTypeIndicator.setVisibility(View.VISIBLE);
						break;
					} else if (tmp.equals("client/phone")) {
						clientTypeIndicator.setImageResource(R.drawable.client_mobile);
						clientTypeIndicator.setVisibility(View.VISIBLE);
						break;
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		boolean co = jaxmpp.getModulesManager().getModule(MessageModule.class).getChatManager().isChatOpenFor(jid);
		openChatNotifier.setVisibility(co ? View.VISIBLE : View.INVISIBLE);

		Integer p = cursor.getInt(cursor.getColumnIndex(RosterTableMetaData.FIELD_PRESENCE));
		CPresence cp = CPresence.valueOf(p);

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

		String status = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_STATUS_MESSAGE));
		if (status != null) {
			itemDescription.setText(Html.fromHtml(status));
		} else
			itemDescription.setText("");

		Bitmap avatarBmp = AvatarHelper.getAvatar(jid, cursor, RosterTableMetaData.FIELD_AVATAR);
		if (avatarBmp != null) {			
			itemAvatar.setImageBitmap(avatarBmp);
		} else {
			itemAvatar.setImageResource(R.drawable.user_avatar);
		}

	}
	
	private void findColumns(String[] from, Cursor mCursor) {
		int i;
		int count = from.length;
		if (mFrom == null) {
			mFrom = new int[count];
		}
		if (mCursor != null) {
			for (i = 0; i < count; i++) {
				mFrom[i] = mCursor.getColumnIndexOrThrow(from[i]);
			}
		} else {
			for (i = 0; i < count; i++) {
				mFrom[i] = -1;
			}
		}
	}
}
