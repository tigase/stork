package org.tigase.mobile;

import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class RosterAdapter extends SimpleCursorTreeAdapter {

	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE };
	private final static int[] names = new int[] { R.id.roster_item_jid };

	static Context staticContext;

	private Context context;

	protected int[] mFrom;

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

		if (mFrom == null) {
			findColumns(cols, cursor);
		}

		TextView itemJid = (TextView) view.findViewById(R.id.roster_item_jid);
		TextView itemDescription = (TextView) view.findViewById(R.id.roster_item_description);
		itemJid.setTransformationMethod(SingleLineTransformationMethod.getInstance());

		View openChatNotifier = view.findViewById(R.id.openChatNotifier);

		ImageView itemPresence = (ImageView) view.findViewById(R.id.roster_item_precence);

		String name = cursor.getString(mFrom[1]);
		itemJid.setText(name);

		boolean co = XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChatManager().isChatOpenFor(
				BareJID.bareJIDInstance(cursor.getString(mFrom[0])));
		openChatNotifier.setVisibility(co ? View.VISIBLE : View.INVISIBLE);

		Integer p = cursor.getInt(mFrom[2]);
		CPresence cp = CPresence.valueOf(p);

		itemDescription.setText(cp == null ? CPresence.offline.name() : cp.name());

		if (cp == null)
			itemPresence.setImageResource(R.drawable.user_offline);
		else
			switch (cp) {
			case chat:
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
			default:
				itemPresence.setImageResource(R.drawable.user_offline);
				break;
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

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		if (context == null)
			context = staticContext;
		String group = groupCursor.getString(1);
		return context.getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, new String[] { group },
				null);
	}

}
