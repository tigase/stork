package org.tigase.messenger.phone.pro.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.chat.ChatActivity;
import org.tigase.messenger.phone.pro.conversations.muc.MucActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.providers.RosterProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

public class MessageNotification {

	final String GROUP_KEY_MESSAGES = "group_key_messages";

	public static void cancelSummaryNotification(Context context) {
		getCotificationManager(context).cancel("summaryNotification".hashCode());
	}

	public static NotificationManagerCompat getCotificationManager(Context context) {
		return NotificationManagerCompat.from(context);
	}

	private void _show(Context context, String senderName, String account, String chatJid, String messageBody, PendingIntent resultPendingIntent, Bitmap avatar, Uri soundUri, long[] vibrationPattern, int notificationId) throws JaxmppException {
		Uri chatHistoryUri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + chatJid);

		final String[] chatHistoryCols = new String[]{
				DatabaseContract.ChatHistory.FIELD_BODY};

		Cursor chatHistoryCursor = context.getContentResolver().query(chatHistoryUri, chatHistoryCols,
				DatabaseContract.ChatHistory.FIELD_STATE + "=?",
				new String[]{"" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD},
				DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC");
		int unread = 0;

		StringBuilder body = new StringBuilder();
		try {
			unread = chatHistoryCursor.getCount();
			int c = 0;
			while (chatHistoryCursor.moveToNext() && (++c) <= 5) {
				String t = chatHistoryCursor.getString(chatHistoryCursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY)) + "\n";
				body.insert(0, t);
			}
		} finally {
			chatHistoryCursor.close();
		}

		// Build the notification, setting the group appropriately
		NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.notification_new_message_from, senderName))
				.setStyle(new NotificationCompat.BigTextStyle()
						.bigText(body.toString()))
				.setContentText(messageBody)
				.setContentIntent(resultPendingIntent)
				.setSmallIcon(R.drawable.ic_messenger_icon)
				.setLargeIcon(avatar)
				.setAutoCancel(true)
				.setGroup(GROUP_KEY_MESSAGES);

		if (unread > 1) {
			notifBuilder.setNumber(unread);
		}

		notifBuilder.setSound(soundUri);
		notifBuilder.setCategory(Notification.CATEGORY_MESSAGE);
		if (vibrationPattern != null) {
			notifBuilder.setVibrate(vibrationPattern);
		}


		// ----------========*******========----------

		final String[] unreadChatsCols = new String[]{
				DatabaseContract.OpenChats.FIELD_ID,
				ChatProvider.FIELD_NAME,
				ChatProvider.FIELD_UNREAD_COUNT,
				ChatProvider.FIELD_LAST_MESSAGE,
				ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP, ChatProvider.FIELD_LAST_MESSAGE_STATE};

		Cursor unreadChatsCursor = context.getContentResolver().query(ChatProvider.OPEN_CHATS_URI,
				unreadChatsCols,
				ChatProvider.FIELD_UNREAD_COUNT + ">0",
				null,
				ChatProvider.FIELD_LAST_MESSAGE_TIMESTAMP + " DESC");

		Log.d("MessageNotification", "Found " + unreadChatsCursor.getCount() + " unread conversations");

		// Create an InboxStyle notification
		NotificationCompat.Builder summaryNotificationBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_messenger_icon)
				.setGroup(GROUP_KEY_MESSAGES)
				.setAutoCancel(true)
				.setGroupSummary(true);

		summaryNotificationBuilder.setSound(soundUri);
		summaryNotificationBuilder.setCategory(Notification.CATEGORY_MESSAGE);
		if (vibrationPattern != null) {
			summaryNotificationBuilder.setVibrate(vibrationPattern);
		}

		if (unreadChatsCursor.getCount() > 1) {
			int count = 0;
			NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
			while (unreadChatsCursor.moveToNext()) {
				count += unreadChatsCursor.getInt(unreadChatsCursor.getColumnIndex(ChatProvider.FIELD_UNREAD_COUNT));
				style.addLine(Html
						.fromHtml("<b>"
								+ unreadChatsCursor.getString(unreadChatsCursor.getColumnIndex(ChatProvider.FIELD_NAME))
								+ "</b>&nbsp;&nbsp;" + unreadChatsCursor.getString(unreadChatsCursor.getColumnIndex(ChatProvider.FIELD_LAST_MESSAGE))));
			}
			style.setBigContentTitle(context.getString(R.string.notification_new_messages, count));
			style.setSummaryText(context.getString(R.string.notification_unread_conversations, unreadChatsCursor.getCount()));

			summaryNotificationBuilder.setContentTitle(context.getString(R.string.notification_new_messages, count));
			summaryNotificationBuilder.setStyle(style);
			summaryNotificationBuilder.setNumber(unreadChatsCursor.getCount());

			Intent summaryResultIntent = new Intent(context, MainActivity.class);
			summaryResultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
					Intent.FLAG_ACTIVITY_CLEAR_TASK);
			summaryResultIntent.putExtra("open", "chats");
			summaryNotificationBuilder.setContentIntent(PendingIntent.getActivity(
					context,
					0,
					summaryResultIntent,
					PendingIntent.FLAG_UPDATE_CURRENT
			));
		} else {
			summaryNotificationBuilder.setContentTitle(senderName)
					.setContentIntent(resultPendingIntent)
					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(body.toString()))
					.setContentText(messageBody)
					.setContentIntent(resultPendingIntent)
					.setLargeIcon(avatar);
			if (unread > 1) {
				summaryNotificationBuilder.setNumber(unread);
			}
		}


		unreadChatsCursor.close();


		// Issue the notification
		//NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		notificationManager.notify(notificationId, notifBuilder.build());
		notificationManager.notify("summaryNotification".hashCode(), summaryNotificationBuilder.build());
	}


	public void show(Context context, Room room, Message msg) throws JaxmppException {
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		final String account = room.getSessionObject().getUserBareJid().toString();
		final String chatJid = room.getRoomJid().toString();
		final String senderName = msg.getFrom().getResource();

		Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_groupchat_24dp);
		final Bitmap avatar = AvatarHelper.getCroppedBitmap(bm);

		Intent resultIntent = new Intent(context, MucActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("openChatId", (int) room.getId());
		resultIntent.putExtra("jid", room.getRoomJid().toString());
		resultIntent.putExtra("account", account);
		PendingIntent resultPendingIntent =
				PendingIntent.getActivity(
						context,
						0,
						resultIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		String sound = sharedPref.getString("notifications_new_groupmessage_ringtone", null);
		boolean vibrate = sharedPref.getBoolean("notifications_new_groupmessage_vibrate", true);

		final Uri soundUri = sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(sound);
		final long[] vibrationPattern = !vibrate ? null : new long[]{0, 400, 200, 100, 200, 100};


		_show(context, context.getResources().getString(R.string.notification_new_groupmessage_from, chatJid), account, chatJid, msg.getBody(), resultPendingIntent, avatar, soundUri, vibrationPattern, ("muc:" + room.getId()).hashCode());
	}


	public void show(Context context, Chat chat, Message msg) throws JaxmppException {
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		final String account = chat.getSessionObject().getUserBareJid().toString();
		final String chatJid = chat.getJid().getBareJid().toString();
		final Bitmap avatar = AvatarHelper.getAvatar(chat.getJid().getBareJid());

		Intent resultIntent = new Intent(context, ChatActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("openChatId", (int) chat.getId());
		resultIntent.putExtra("jid", chat.getJid().getBareJid().toString());
		resultIntent.putExtra("account", account);
		PendingIntent resultPendingIntent =
				PendingIntent.getActivity(
						context,
						0,
						resultIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		String senderName = chatJid;
		Cursor chatHistoryCursor = context.getContentResolver().query(Uri.parse(RosterProvider.ROSTER_URI + "/" + account + "/" + chatJid),
				new String[]{
						DatabaseContract.RosterItemsCache.FIELD_ID,
						DatabaseContract.RosterItemsCache.FIELD_NAME
				}, null, null, null);
		if (chatHistoryCursor != null) {
			try {
				if (chatHistoryCursor.moveToNext()) {
					String name = chatHistoryCursor.getString(chatHistoryCursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
					if (name != null && !name.isEmpty())
						senderName = name;
				}
			} finally {
				chatHistoryCursor.close();
			}
		}

		String sound = sharedPref.getString("notifications_new_message_ringtone", null);
		boolean vibrate = sharedPref.getBoolean("notifications_new_message_vibrate", true);

		final Uri soundUri = sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(sound);
		final long[] vibrationPattern = !vibrate ? null : new long[]{0, 400, 200, 100, 200, 100};

		_show(context, context.getResources().getString(R.string.notification_new_message_from, senderName), account, chatJid, msg.getBody(), resultPendingIntent, avatar, soundUri, vibrationPattern, ("chat:" + chat.getId()).hashCode());
	}
}
