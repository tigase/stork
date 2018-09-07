package org.tigase.messenger.phone.pro.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

	final static int SUMMARY_ID = "SUMMARY_NOTIFICATION_ID".hashCode();
	final String CHANNEL_ID = "MESSAGES_CHANNEL";
	final String GROUP_KEY_MESSAGES = "group_key_messages";

	public static void cancelSummaryNotification(Context context) {
		getCotificationManager(context).cancel(SUMMARY_ID);
	}

	public static NotificationManagerCompat getCotificationManager(Context context) {
		return NotificationManagerCompat.from(context);
	}

	private UnreadConversation createChatSyle(Context context, Room room) {
		final String account = room.getSessionObject().getUserBareJid().toString();
		final String chatJid = room.getRoomJid().toString();

		final NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

		Uri chatHistoryUri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + chatJid);
		final String[] chatHistoryCols = new String[]{DatabaseContract.ChatHistory.FIELD_BODY,
													  DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME};

		try (Cursor chatHistoryCursor = context.getContentResolver()
				.query(chatHistoryUri, chatHistoryCols, DatabaseContract.ChatHistory.FIELD_STATE + "=?",
					   new String[]{"" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD},
					   DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC")) {
			int unread = chatHistoryCursor.getCount();
			int c = 0;
			while (chatHistoryCursor.moveToNext() && (++c) <= 5) {
				String txt = chatHistoryCursor.getString(
						chatHistoryCursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
				String author = chatHistoryCursor.getString(
						chatHistoryCursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME));
				style.addLine(" " + author + ": " + txt);
			}
			return new UnreadConversation(style, unread);
		}
	}

	private UnreadConversation createChatSyle(Context context, Chat chat) {
		final String account = chat.getSessionObject().getUserBareJid().toString();
		final String chatJid = chat.getJid().getBareJid().toString();

		final NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

		Uri chatHistoryUri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + account + "/" + chatJid);
		final String[] chatHistoryCols = new String[]{DatabaseContract.ChatHistory.FIELD_BODY};

		try (Cursor chatHistoryCursor = context.getContentResolver()
				.query(chatHistoryUri, chatHistoryCols, DatabaseContract.ChatHistory.FIELD_STATE + "=?",
					   new String[]{"" + DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD},
					   DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC")) {
			int unread = chatHistoryCursor.getCount();
			int c = 0;
			while (chatHistoryCursor.moveToNext() && (++c) <= 5) {
				String t = chatHistoryCursor.getString(
						chatHistoryCursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
				style.addLine("  " + t);
			}
			return new UnreadConversation(style, unread);
		}
	}

	private NotificationCompat.Builder createNotificationBuilder(Context context, Uri soundUri, long[] vibrationPattern,
																 PendingIntent resultPendingIntent) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(
				R.drawable.ic_messenger_icon)
				.setAutoCancel(true)
				.setGroup(GROUP_KEY_MESSAGES)
				.setCategory(Notification.CATEGORY_MESSAGE);

		if (soundUri != null) {
			builder.setSound(soundUri);
		}
		if (vibrationPattern != null) {
			builder.setVibrate(vibrationPattern);
		}
		if (resultPendingIntent != null) {
			builder.setContentIntent(resultPendingIntent);
		}

		return builder;
	}

	public void createNotificationChannel(Context context) {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(R.string.channel_name);
			String description = context.getString(R.string.channel_description);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	public void show(Context context, Chat chat, Message msg) throws JaxmppException {
		createNotificationChannel(context);
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext());

		final String account = chat.getSessionObject().getUserBareJid().toString();
		final String chatJid = chat.getJid().getBareJid().toString();
		final Bitmap avatar = AvatarHelper.getAvatar(chat.getJid().getBareJid());

		Intent resultIntent = new Intent(context, ChatActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("openChatId", (int) chat.getId());
		resultIntent.putExtra(ChatActivity.JID_KEY, chat.getJid().getBareJid().toString());
		resultIntent.putExtra(ChatActivity.ACCOUNT_KEY, account);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
																	  PendingIntent.FLAG_UPDATE_CURRENT);

		String senderName = chatJid;
		Cursor chatHistoryCursor = context.getContentResolver()
				.query(Uri.parse(RosterProvider.ROSTER_URI + "/" + account + "/" + chatJid),
					   new String[]{DatabaseContract.RosterItemsCache.FIELD_ID,
									DatabaseContract.RosterItemsCache.FIELD_NAME}, null, null, null);
		if (chatHistoryCursor != null) {
			try {
				if (chatHistoryCursor.moveToNext()) {
					String name = chatHistoryCursor.getString(
							chatHistoryCursor.getColumnIndex(DatabaseContract.RosterItemsCache.FIELD_NAME));
					if (name != null && !name.isEmpty()) {
						senderName = name;
					}
				}
			} finally {
				chatHistoryCursor.close();
			}
		}

		String sound = sharedPref.getString("notifications_new_message_ringtone", null);
		boolean vibrate = sharedPref.getBoolean("notifications_new_message_vibrate", true);

		final Uri soundUri =
				sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(sound);
		final long[] vibrationPattern = !vibrate ? null : new long[]{0, 400, 200, 100, 200, 100};

		NotificationCompat.Builder notification = createNotificationBuilder(context, soundUri, vibrationPattern,
																			resultPendingIntent);
		UnreadConversation unreadCnversation = createChatSyle(context, chat);
		notification.setStyle(unreadCnversation.style);
		notification.setContentTitle(context.getResources()
											 .getQuantityString(R.plurals.notification_unread_muc,
																unreadCnversation.unread, senderName,
																unreadCnversation.unread));
		notification.setContentText(msg.getBody());
		notification.setLargeIcon(avatar);

		NotificationCompat.Builder summaryNotification = createNotificationBuilder(context, soundUri, vibrationPattern,
																				   resultPendingIntent);
		summaryNotification.setGroupSummary(true);
		summaryNotification.setContentText(msg.getBody());
		summaryNotification.setContentIntent(resultPendingIntent);

		final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

		int notificationId = ("chat:" + chat.getId()).hashCode();
		notificationManager.notify(notificationId, notification.build());
		notificationManager.notify(SUMMARY_ID, summaryNotification.build());
	}

	public void show(Context context, Room room, Message msg) throws JaxmppException {
		createNotificationChannel(context);
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext());

		final String account = room.getSessionObject().getUserBareJid().toString();
		final String chatJid = room.getRoomJid().toString();
		final String senderName = msg.getFrom().getResource();

		Intent resultIntent = new Intent(context, MucActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("openChatId", (int) room.getId());
		resultIntent.putExtra("jid", room.getRoomJid().toString());
		resultIntent.putExtra("account", account);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
																	  PendingIntent.FLAG_UPDATE_CURRENT);

		String sound = sharedPref.getString("notifications_new_groupmessage_ringtone", null);
		boolean vibrate = sharedPref.getBoolean("notifications_new_groupmessage_vibrate", true);

		final Uri soundUri =
				sound == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(sound);
		final long[] vibrationPattern = !vibrate ? null : new long[]{0, 400, 200, 100, 200, 100};

		NotificationCompat.Builder notification = createNotificationBuilder(context, soundUri, vibrationPattern,
																			resultPendingIntent);
		UnreadConversation unreadCnversation = createChatSyle(context, room);
		notification.setStyle(unreadCnversation.style);
		notification.setContentTitle(context.getResources()
											 .getQuantityString(R.plurals.notification_unread_muc,
																unreadCnversation.unread, room.getRoomJid(),
																unreadCnversation.unread));
		notification.setContentText(senderName + ": " + msg.getBody());

		NotificationCompat.Builder summaryNotification = createNotificationBuilder(context, soundUri, vibrationPattern,
																				   resultPendingIntent);
		summaryNotification.setGroupSummary(true);
		summaryNotification.setContentText(msg.getBody());
		summaryNotification.setContentIntent(resultPendingIntent);

		final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

		int notificationId = ("muc:" + room.getId()).hashCode();
		notificationManager.notify(notificationId, notification.build());
		notificationManager.notify(SUMMARY_ID, summaryNotification.build());
	}

	private class UnreadConversation {

		final NotificationCompat.Style style;
		final int unread;

		public UnreadConversation(NotificationCompat.Style style, int unread) {
			this.style = style;
			this.unread = unread;
		}
	}

}
