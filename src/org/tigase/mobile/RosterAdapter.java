package org.tigase.mobile;

import org.tigase.mobile.db.RosterTableMetaData;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RosterAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_NAME };
	private final static int[] names = new int[] { R.id.roster_item_jid };

	private final Context context;
	protected int[] mFrom;

	private int resource;

	public RosterAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
		this.context = context;
		this.resource = layout;
		findColumns(cols, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		// super.bindView(view, context, cursor);

		TextView itemJid = (TextView) view.findViewById(R.id.roster_item_jid);
		TextView itemPresence = (TextView) view.findViewById(R.id.roster_item_precence);

		String jid = cursor.getString(mFrom[0]);
		itemJid.setText(jid);

		try {
			Presence presence = XmppService.jaxmpp().getPresence().getBestPresence(BareJID.bareJIDInstance(jid));
			itemPresence.setText(presence == null ? "-" : presence.getShow().name());
		} catch (XMLException e) {
			e.printStackTrace();
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
