package org.tigase.mobile.roster;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.GeolocationTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.pubsub.GeolocationModule;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
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
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.itemJid = (TextView) view.findViewById(R.id.roster_item_jid);
			holder.itemDescription = (TextView) view.findViewById(R.id.roster_item_description);
			holder.openChatNotifier = view.findViewById(R.id.openChatNotifier);

			holder.itemAvatar = (ImageView) view.findViewById(R.id.imageView1);
			holder.itemPresence = (ImageView) view.findViewById(R.id.roster_item_precence);
			holder.clientTypeIndicator = (ImageView) view.findViewById(R.id.client_type_indicator);
		}

		holder.itemJid.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		String name = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_DISPLAY_NAME));
		holder.itemJid.setText(name);

		BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

		final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
		final Jaxmpp jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(account);

		holder.clientTypeIndicator.setVisibility(View.INVISIBLE);

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
						holder.clientTypeIndicator.setImageResource(R.drawable.client_messenger);
						holder.clientTypeIndicator.setVisibility(View.VISIBLE);
						break;
					} else if (tmp.equals("client/phone")) {
						holder.clientTypeIndicator.setImageResource(R.drawable.client_mobile);
						holder.clientTypeIndicator.setVisibility(View.VISIBLE);
						break;
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		boolean co = jaxmpp.getModulesManager().getModule(MessageModule.class).getChatManager().isChatOpenFor(jid);
		holder.openChatNotifier.setVisibility(co ? View.VISIBLE : View.INVISIBLE);

		Integer p = cursor.getInt(cursor.getColumnIndex(RosterTableMetaData.FIELD_PRESENCE));
		CPresence cp = CPresence.valueOf(p);

		if (cp == null)
			holder.itemPresence.setImageResource(R.drawable.user_offline);
		else
			switch (cp) {
			case chat:
				holder.itemPresence.setImageResource(R.drawable.user_free_for_chat);
				break;
			case online:
				holder.itemPresence.setImageResource(R.drawable.user_available);
				break;
			case away:
				holder.itemPresence.setImageResource(R.drawable.user_away);
				break;
			case xa:
				holder.itemPresence.setImageResource(R.drawable.user_extended_away);
				break;
			case dnd:
				holder.itemPresence.setImageResource(R.drawable.user_busy);
				break;
			case requested:
				holder.itemPresence.setImageResource(R.drawable.user_ask);
				break;
			case error:
				holder.itemPresence.setImageResource(R.drawable.user_error);
				break;
			case offline_nonauth:
				holder.itemPresence.setImageResource(R.drawable.user_noauth);
				break;
			default:
				holder.itemPresence.setImageResource(R.drawable.user_offline);
				break;
			}

		String status = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_STATUS_MESSAGE));
		if (status != null) {
			holder.itemDescription.setText(Html.fromHtml(status));
		} else {
			status = "";
			// TODO: is it fast enough?
			GeolocationModule geoModule = jaxmpp.getModulesManager().getModule(GeolocationModule.class);
			if (geoModule != null) {
				ContentValues geoValue = geoModule.getLocationForJid(jid);
				if (geoValue != null) {
					String locality = geoValue.getAsString(GeolocationTableMetaData.FIELD_LOCALITY);
					if (locality != null) {
						status = locality;
					}
					String country = geoValue.getAsString(GeolocationTableMetaData.FIELD_COUNTRY);
					if (country != null) {
						if (!status.isEmpty()) {
							status += ", ";
						}				
						status += country;
					}
				}
			}
			holder.itemDescription.setText(status);
		}

//		Bitmap avatarBmp = AvatarHelper.getAvatar(jid, cursor, RosterTableMetaData.FIELD_AVATAR);
//		if (avatarBmp != null) {			
//			itemAvatar.setImageBitmap(avatarBmp);
//		} else {
//			itemAvatar.setImageResource(R.drawable.user_avatar);
//		}
		AvatarHelper.setAvatarToImageView(jid, holder.itemAvatar);
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
	
	static class ViewHolder {
		TextView itemJid;
		TextView itemDescription;
		View openChatNotifier;
		ImageView itemAvatar;
		ImageView itemPresence;
		ImageView clientTypeIndicator;		
	}
}
