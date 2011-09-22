package org.tigase.mobile;

import java.util.ArrayList;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
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
import android.widget.TextView;

public class ChatListActivity extends Activity {

	public class ImageAdapter extends BaseAdapter {

		private final ArrayList<Chat> chats = new ArrayList<Chat>();

		private Context mContext;

		private LayoutInflater mInflater;

		public ImageAdapter(Context c) {
			this.chats.addAll(XmppService.jaxmpp().getModulesManager().getModule(MessageModule.class).getChats());
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
			return this.chats.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View imageView;
			if (convertView == null) {
				imageView = mInflater.inflate(R.layout.chat_list_item, parent, false);
				// imageView.setLayoutParams(new GridView.LayoutParams(45, 45));
				// imageView.setAdjustViewBounds(false);
				// imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(8, 8, 8, 8);
			} else {
				imageView = convertView;
			}

			Chat chat = this.chats.get(position);

			TextView tv = (TextView) imageView.findViewById(R.id.chat_list_item_name);
			tv.setText(chat.getJid().toString());

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
