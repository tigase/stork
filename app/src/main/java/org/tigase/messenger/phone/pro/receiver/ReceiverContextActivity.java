package org.tigase.messenger.phone.pro.receiver;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import org.tigase.messenger.phone.pro.DividerItemDecoration;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.RosterProvider;

public class ReceiverContextActivity
		extends AppCompatActivity {

	private final OnItemSelected selectionHandler = ReceiverContextActivity.this::onChatSelected;
	private DBUpdateTask dbUpdateTask;
	private CursorViewAdapter adapter = new CursorViewAdapter(this, null, selectionHandler) {
		@Override
		protected void onContentChanged() {
			loadData();
		}
	};
	private RecyclerView recyclerView;

	private void handleSendImageToChat(String account, String chatJid, Intent intent) {
		Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

		Intent i = new Intent(this, ChatActivity.class);
		i.setData(imageUri);
		i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		i.setAction(ChatActivity.SEND_IMAGE_ACTION);
		i.putExtra(ChatActivity.JID_KEY, chatJid);
		i.putExtra(ChatActivity.ACCOUNT_KEY, account);

		startActivity(i);
		finish();
	}

	private void handleSendTextToChat(String account, String chatJid, Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

		Intent i = new Intent(this, ChatActivity.class);
		i.setAction(ChatActivity.SEND_TEXT_ACTION);
		i.putExtra(ChatActivity.JID_KEY, chatJid);
		i.putExtra(ChatActivity.TEXT, sharedText);
		i.putExtra(ChatActivity.ACCOUNT_KEY, account);

		startActivity(i);
		finish();
	}

	private void loadData() {
		if (dbUpdateTask == null || dbUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
			dbUpdateTask = new DBUpdateTask(this, adapter);
			dbUpdateTask.execute();
		}
	}

	private void onChatSelected(String account, String chatJid) {
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if ("text/plain".equals(type)) {
				handleSendTextToChat(account, chatJid, intent);
			} else if (type.startsWith("image/")) {
				handleSendImageToChat(account, chatJid, intent);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_receiver);
		setTitle("Send to...");

		recyclerView = (RecyclerView) findViewById(R.id.contacts_list);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		recyclerView.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		recyclerView.setAdapter(null);
		adapter.swapCursor(null);
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		loadData();
	}

	interface OnItemSelected {

		void onItemSelected(String account, String chatJid);
	}

	private static class DBUpdateTask
			extends AsyncTask<Void, Void, Cursor> {

		private final CursorViewAdapter adapter;
		private final String[] columnsToReturn = new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
															  DatabaseContract.RosterItemsCache.FIELD_ACCOUNT,
															  DatabaseContract.RosterItemsCache.FIELD_JID,
															  DatabaseContract.RosterItemsCache.FIELD_NAME,
															  DatabaseContract.RosterItemsCache.FIELD_STATUS};
		private final Context context;

		private DBUpdateTask(Context context, CursorViewAdapter adapter) {
			this.context = context;
			this.adapter = adapter;
		}

		@Override
		protected Cursor doInBackground(Void... voids) {
			String selection = "1=1 ";

			String sort = DatabaseContract.RosterItemsCache.FIELD_NAME + " COLLATE NOCASE ASC";

			Cursor cursor = context.getContentResolver()
					.query(RosterProvider.ROSTER_URI, columnsToReturn, selection, null, sort);

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
		}

	}

}
