package org.tigase.mobile.chatlist;

import java.util.ArrayList;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.roster.CPresence;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

		private final ArrayList<ChatWrapper> chats = new ArrayList<ChatWrapper>();

		private Context mContext;

		private LayoutInflater mInflater;

		private final MultiJaxmpp multi;

		public ImageAdapter(Context c) {
			this.multi = ((MessengerApplication) c.getApplicationContext()).getMultiJaxmpp();
			this.chats.addAll(multi.getChats());
			mContext = c;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			// XXX
			return this.chats.get(position).hashCode();
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

			final TextView tv = (TextView) imageView.findViewById(R.id.chat_list_item_name);
			final ImageView avatar = (ImageView) imageView.findViewById(R.id.imageView1);
			final ImageView itemPresence = (ImageView) imageView.findViewById(R.id.imageView2);

			ChatWrapper wrapper = this.chats.get(position);
			if (wrapper.isChat()) {
				Chat chat = wrapper.getChat();

				// final Cursor cursor = getContentResolver().query(
				// Uri.parse(RosterProvider.CONTENT_URI + "/" +
				// chat.getJid().getBareJid()), null, null, null, null);
				// Bitmap avatarBmp = null;
				// try {
				// cursor.moveToNext();
				// avatarBmp =
				// AvatarHelper.getAvatar(chat.getJid().getBareJid(), cursor,
				// RosterTableMetaData.FIELD_AVATAR);
				// } catch (Exception ex) {
				// Log.v("ChatListActivity", "no avatar for " +
				// chat.getJid().getBareJid().toString());
				// } finally {
				// cursor.close();
				// }

				String x;
				RosterStore roster = multi.get(chat.getSessionObject()).getRoster();
				RosterItem ri = roster.get(chat.getJid().getBareJid());
				if (ri == null)
					x = chat.getJid().toString();
				else
					x = RosterDisplayTools.getDisplayName(ri);

				final CPresence cp = RosterDisplayTools.getShowOf(chat.getSessionObject(), chat.getJid());

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

				tv.setText(x);

				// if (avatarBmp != null) {
				// avatar.setImageBitmap(avatarBmp);
				// } else {
				// avatar.setImageResource(R.drawable.user_avatar);
				// }
				AvatarHelper.setAvatarToImageView(chat.getJid().getBareJid(), avatar);
			} else {
				Room room = wrapper.getRoom();
				tv.setText(room.getRoomJid().toString());
				avatar.setImageResource(R.drawable.group_chat);
				itemPresence.setVisibility(View.INVISIBLE);
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
				ChatWrapper wrapper = (ChatWrapper) parent.getItemAtPosition(position);
				Intent result = new Intent();
				if (wrapper.isChat()) {
					result.putExtra("jid", wrapper.getChat().getJid().toString());
					result.putExtra("chatId", wrapper.getChat().getId());
				} else {
					result.putExtra("room", wrapper.getRoom().getRoomJid().toString());
					result.putExtra("roomId", wrapper.getRoom().getId());
				}
				setResult(Activity.RESULT_OK, result);
				finish();
			}
		});

		g.setAdapter(new ImageAdapter(this));
	}

}
