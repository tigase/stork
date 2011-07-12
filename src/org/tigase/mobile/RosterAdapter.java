package org.tigase.mobile;

import org.tigase.mobile.db.RosterTableMetaData;

import android.content.Context;
import android.database.Cursor;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RosterAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { RosterTableMetaData.FIELD_JID, RosterTableMetaData.FIELD_DISPLAY_NAME,
			RosterTableMetaData.FIELD_PRESENCE };
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
		TextView itemDescription = (TextView) view.findViewById(R.id.roster_item_description);
		itemJid.setTransformationMethod(SingleLineTransformationMethod.getInstance());

		ImageView itemPresence = (ImageView) view.findViewById(R.id.roster_item_precence);

		String jid = cursor.getString(mFrom[1]);
		itemJid.setText(jid);

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

}
