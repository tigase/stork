package org.tigase.mobile.chatlist;

import java.util.ArrayList;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatListActivity extends Activity {

	private class ImageAdapter extends BaseAdapter {

		private final ArrayList<Chat> chats = new ArrayList<Chat>();

		private Context mContext;

		private LayoutInflater mInflater;

		private final MultiJaxmpp multi;

		private RosterDisplayTools rdt;

		public ImageAdapter(Context c) {
			this.multi = ((MessengerApplication) c.getApplicationContext()).getMultiJaxmpp();
			this.chats.addAll(multi.getChats());
			mContext = c;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.rdt = new RosterDisplayTools(c);
		}

		@Override
		public int getCount() {
			return chats.size();
		}

		@Override
		public Object getItem(int position) {
			return this.chats.get(position);
		}

		@Override
		public long getItemId(int position) {
			return this.chats.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View imageView;
			if (convertView == null) {
				imageView = mInflater.inflate(R.layout.chat_list_item, parent, false);
				// imageView.setLayoutParams(new GridView.LayoutParams(128,
				// 128));
				imageView.setMinimumWidth(300);
				// imageView.setAdjustViewBounds(false);
				// imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(4, 4, 4, 4);
			} else {
				imageView = convertView;
			}

			Chat chat = this.chats.get(position);

			final Cursor cursor = getContentResolver().query(
					Uri.parse(RosterProvider.CONTENT_URI + "/" + chat.getJid().getBareJid()), null, null, null, null);
			byte[] avatarData = null;
			try {
				cursor.moveToNext();
				avatarData = cursor.getBlob(cursor.getColumnIndex(RosterTableMetaData.FIELD_AVATAR));
			} finally {
				cursor.close();
			}

			String x;
			RosterStore roster = multi.get(chat.getSessionObject()).getRoster();
			RosterItem ri = roster.get(chat.getJid().getBareJid());
			if (ri == null)
				x = chat.getJid().toString();
			else
				x = rdt.getDisplayName(ri);

			final CPresence cp = rdt.getShowOf(ri.getSessionObject(), chat.getJid());

			ImageView itemPresence = (ImageView) imageView.findViewById(R.id.imageView2);
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

			TextView tv = (TextView) imageView.findViewById(R.id.chat_list_item_name);
			tv.setText(x);

			ImageView avatar = (ImageView) imageView.findViewById(R.id.imageView1);

			if (avatarData != null) {
				Bitmap bmp = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length);
				avatar.setImageBitmap(bmp);
			} else {
				avatar.setImageResource(R.drawable.user_avatar);
			}

			return imageView;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chats_list);

		GridView g = (GridView) findViewById(R.id.chatsGrid);
		g.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Chat chat = (Chat) parent.getItemAtPosition(position);
				Intent result = new Intent();
				result.putExtra("jid", chat.getJid().toString());
				result.putExtra("chatId", chat.getId());
				setResult(Activity.RESULT_OK, result);
				finish();
			}
		});

		g.setAdapter(new ImageAdapter(this));
	}

}
