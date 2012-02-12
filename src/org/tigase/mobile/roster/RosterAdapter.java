package org.tigase.mobile.roster;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Html;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class RosterAdapter extends SimpleCursorTreeAdapter {

	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE, RosterTableMetaData.FIELD_STATUS_MESSAGE, RosterTableMetaData.FIELD_AVATAR };
	private final static int[] names = new int[] { R.id.roster_item_jid };

	static Context staticContext;

	private Context context;

	// protected int[] mFrom;

	private int resource;

	public RosterAdapter(Context context, Cursor c) {
		// Context context, Cursor cursor, int groupLayout, String[] groupFrom,
		// int[] groupTo, int childLayout, String[] childFrom, int[] childTo
		super(context, c, R.layout.roster_group_item, new String[] { RosterTableMetaData.FIELD_GROUP_NAME },
				new int[] { R.id.roster_group_name }, R.layout.roster_item, cols, names);

		this.context = context;
		this.resource = R.layout.roster_item;
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
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

				String id = capabilitiesModule.getCache().getIdentity(node + "#" + ver);
				if (id != null && id.equals("client/phone") && node.equals(nodeName)) {
					clientTypeIndicator.setImageResource(R.drawable.messenger_client);
					clientTypeIndicator.setVisibility(View.VISIBLE);
					break;
				} else if (id != null && id.equals("client/phone")) {
					clientTypeIndicator.setImageResource(R.drawable.mobile_client);
					clientTypeIndicator.setVisibility(View.VISIBLE);
					break;
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

		byte[] avatar = cursor.getBlob(cursor.getColumnIndex(RosterTableMetaData.FIELD_AVATAR));
		if (avatar != null) {
			Bitmap bmp = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
			itemAvatar.setImageBitmap(bmp);
		} else {
			itemAvatar.setImageResource(R.drawable.user_avatar);
		}

	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		if (context == null)
			context = staticContext;
		String group = groupCursor.getString(1);
		return context.getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, new String[] { group },
				null);
	}

}
