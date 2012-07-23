package org.tigase.mobile.muc;

import java.util.ArrayList;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.R;

import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class OccupantsListActivity extends Activity {

	private class OccupantsAdapter extends BaseAdapter {

		private static final String TAG = "OccupantsAdapter";

		private final LayoutInflater mInflater;

		private final ArrayList<Occupant> occupants = new ArrayList<Occupant>();

		private final Room room;

		public OccupantsAdapter(Context mContext, Room room) {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.room = room;
			occupants.addAll(room.getPresences().values());
		}

		@Override
		public int getCount() {
			return occupants.size();
		}

		@Override
		public Object getItem(int arg0) {
			Occupant o = occupants.get(arg0);
			return o;
		}

		@Override
		public long getItemId(int position) {
			return occupants.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.muc_occupants_list_item, parent, false);
			} else {
				view = convertView;
			}

			final Occupant occupant = (Occupant) getItem(position);

			final TextView nicknameTextView = (TextView) view.findViewById(R.id.occupant_nickname);

			try {
				nicknameTextView.setText(occupant.getNickname());
			} catch (XMLException e) {
				Log.e(TAG, "Can't show occupant", e);
			}

			return view;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.muc_occupants_list);

		long roomId = getIntent().getLongExtra("roomId", -1);

		ChatWrapper room = ((MessengerApplication) getApplication()).getMultiJaxmpp().getRoomById(roomId);

		ListView occupantsList = (ListView) findViewById(R.id.occupants_list);

		OccupantsAdapter adapter = new OccupantsAdapter(getApplicationContext(), room.getRoom());
		occupantsList.setAdapter(adapter);

	}

}
