package org.tigase.mobile.ui;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.filetransfer.AndroidFileTransferUtility;
import org.tigase.mobile.filetransfer.FileTransfer;
import org.tigase.mobile.filetransfer.FileTransferRequestEvent;
import org.tigase.mobile.filetransfer.IncomingFileActivity;
import org.tigase.mobile.roster.AuthRequestActivity;
import org.tigase.mobile.security.SecureTrustManagerFactory.DataCertificateException;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

public abstract class NotificationHelper {

	public static final int AUTH_REQUEST_NOTIFICATION_ID = 132108;

	public static final int CHAT_NOTIFICATION_ID = 132008;

	public static final String DEFAULT_NOTIFICATION_URI = "content://settings/system/notification_sound";

	public static final int ERROR_NOTIFICATION_ID = 5398717;

	public static final int FILE_TRANSFER_NOTIFICATION_ID = 132009;

	public static final int NOTIFICATION_ID = 5398777;

	protected static final String TAG = "NotificationHelper";

	public static Intent createFileTransferAcceptIntent(Context context, FileTransferRequestEvent ev, BareJID account,
			String tag) {
		// intent to accept
		Intent intentAccept = new Intent(context, JaxmppService.class);
		intentAccept.putExtra("tag", tag);
		intentAccept.putExtra("account", account.toString());
		intentAccept.putExtra("sender", ev.getSender().toString());
		intentAccept.putExtra("id", ev.getId());
		intentAccept.putExtra("sid", ev.getSid());
		intentAccept.putExtra("filename", ev.getFilename());
		if (ev.getFilesize() != null) {
			intentAccept.putExtra("filesize", ev.getFilesize());
		}
		intentAccept.putExtra("mimetype", ev.getMimetype());
		intentAccept.setAction(JaxmppService.ACTION_FILETRANSFER);
		intentAccept.putExtra("filetransferAction", "accept");

		return intentAccept;
	}

	public static Intent createFileTransferRejectIntent(Context context, FileTransferRequestEvent ev, BareJID account,
			String tag) {
		Intent intentReject = new Intent(context, JaxmppService.class);
		intentReject.putExtra("tag", tag);
		intentReject.putExtra("account", account.toString());
		intentReject.putExtra("sender", ev.getSender().toString());
		intentReject.putExtra("id", ev.getId());
		intentReject.putExtra("sid", ev.getSid());
		intentReject.putExtra("filename", ev.getFilename());
		if (ev.getFilesize() != null) {
			intentReject.putExtra("filesize", ev.getFilesize());
		}
		intentReject.putExtra("mimetype", ev.getMimetype());
		intentReject.setAction(JaxmppService.ACTION_FILETRANSFER);
		intentReject.putExtra("filetransferAction", "reject");

		return intentReject;
	}

	public static NotificationHelper createIntstance(Context context) {
		if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
			return new NotificationHelperJellyBean(context);
		} else if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT) {
			return new NotificationHelperICS(context);
		} else if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
			return new NotificationHelperHoneycomb(context);
		} else {
			return new NotificationHelperBase(context);
		}
	}

	protected final Context context;

	protected final NotificationManager notificationManager;

	protected NotificationHelper(Context context) {
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void cancelChatNotification(String tag) {
		notificationManager.cancel(tag, NotificationHelper.CHAT_NOTIFICATION_ID);
	}

	public void cancelFileTransferRequestNotification(String tag) {
		notificationManager.cancel(tag, FILE_TRANSFER_NOTIFICATION_ID);
	}

	public void cancelNotification() {
		notificationManager.cancel(NOTIFICATION_ID);
	}

	protected PendingIntent createFileTransferProgressPendingIntent(FileTransfer ft) {
		Context context = this.context.getApplicationContext();
		Intent intent = null;
		if (!ft.outgoing && ft.getState() == FileTransfer.State.finished && ft.mimetype != null) {
			intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(ft.destination), ft.mimetype);
		} else {
			intent = new Intent(context, TigaseMobileMessengerActivity.class);
		}
		return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
	}

	protected PendingIntent createFileTransferRequestPendingIntent(FileTransferRequestEvent ev, JaxmppCore jaxmpp, String tag) {
		Context context = this.context.getApplicationContext();
		Intent intent = new Intent(context, IncomingFileActivity.class);
		intent.putExtra("account", jaxmpp.getSessionObject().getUserBareJid().toString());
		intent.putExtra("sender", ev.getSender().toString());
		intent.putExtra("id", ev.getId());
		intent.putExtra("sid", ev.getSid());
		intent.putExtra("filename", ev.getFilename());
		intent.putExtra("tag", tag);
		if (ev.getFilesize() != null) {
			intent.putExtra("filesize", ev.getFilesize());
		}
		intent.putExtra("mimetype", ev.getMimetype());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_CANCEL_CURRENT
				| PendingIntent.FLAG_ONE_SHOT);
	}

	private final MultiJaxmpp getMulti() {
		return ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();
	}

	public void notificationUpdate(final int ico, final String notiticationTitle, final String expandedNotificationText) {
		long whenNotify = System.currentTimeMillis();
		Notification notification = new Notification(ico, notiticationTitle, whenNotify);

		// notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		Context context = this.context.getApplicationContext();
		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void notificationUpdateFail(SessionObject account, String message, String notificationMessage, Throwable cause) {
		String notiticationTitle = context.getResources().getString(R.string.service_error_notification_title);
		String expandedNotificationText;
		if (notificationMessage != null)
			expandedNotificationText = notificationMessage;
		else if (message == null && cause != null) {
			message = cause.getMessage();
		}
		if (message != null) {
			expandedNotificationText = message;
		} else
			expandedNotificationText = notiticationTitle;

		Notification notification = new Notification(R.drawable.ic_stat_warning, notiticationTitle, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		updateSound(notification, Preferences.NOTIFICATION_ERROR_KEY);
		updateLight(notification, Preferences.NOTIFICATION_ERROR_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_ERROR_KEY);

		final Context context = this.context.getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if (cause instanceof DataCertificateException) {
			expandedNotificationText = context.getResources().getString(R.string.trustcert_accountnotification,
					account.getUserBareJid().getDomain());
			intent.setAction(TigaseMobileMessengerActivity.CERT_UNTRUSTED_ACTION);
			intent.putExtra("cause", cause);
		} else {
			intent.setAction(TigaseMobileMessengerActivity.ERROR_ACTION);
		}
		intent.putExtra("message", message);
		intent.putExtra("account", account.getUserBareJid().toString());

		// intent.putExtra("details", message);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		account.setProperty("messenger#error", message);

		notificationManager.notify("error:" + account.getUserBareJid().toString(), ERROR_NOTIFICATION_ID, notification);

	}

	public void notifyFileTransferProgress(FileTransfer ft) {
		String tag = ft.toString();

		int ico = ft.outgoing ? android.R.drawable.stat_sys_upload : android.R.drawable.stat_sys_download;
		String notificationTitle;
		if (ft.outgoing)
			notificationTitle = context.getResources().getString(R.string.service_file_transfer_sending_file, ft.filename,
					(ft.buddyName != null) ? ft.buddyName : ft.buddyJid.toString());
		else
			notificationTitle = context.getResources().getString(R.string.service_file_transfer_receiving_file, ft.filename,
					(ft.buddyName != null) ? ft.buddyName : ft.buddyJid.toString());

		String notificationText = "";

		switch (ft.getState()) {
		case error:
			ico = android.R.drawable.stat_notify_error;
			notificationText = ft.errorMessage;
			break;

		case negotiating:
			notificationText = context.getResources().getString(R.string.service_file_transfer_negotiating);
			break;

		case connecting:
			notificationText = context.getResources().getString(R.string.service_file_transfer_connecting);
			break;

		case active:
			notificationText = context.getResources().getString(R.string.service_file_transfer_progress, ft.getProgress());
			break;

		case finished:
			notificationText = context.getResources().getString(R.string.service_file_transfer_finished);
			if (!ft.outgoing) {
				AndroidFileTransferUtility.refreshMediaScanner(context.getApplicationContext(), ft.destination);
			}
			break;
		default:
			break;
		}

		Notification notification = prepareFileTransferProgressNotification(ico, notificationTitle, notificationText, ft);
		notificationManager.notify(tag, FILE_TRANSFER_NOTIFICATION_ID, notification);
	}

	public void notifyFileTransferRequest(FileTransferRequestEvent ev) {
		Jaxmpp jaxmpp = getMulti().get(ev.getSessionObject());
		String tag = ev.getSender().toString() + " -> "
				+ jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID).toString() + " file = "
				+ ev.getFilename();

		RosterItem ri = jaxmpp.getRoster().get(ev.getSender().getBareJid());

		int ico = android.R.drawable.stat_sys_download;
		String title = context.getResources().getString(R.string.service_incoming_file_notification_title, ev.getFilename(),
				ri != null && ri.getName() != null ? ri.getName() : ev.getSender().getBareJid().toString());
		String text = context.getResources().getString(R.string.service_incoming_file_notification_text, ev.getFilename());

		Notification notification = prepareFileTransferRequestNotification(ico, title, text, ev, jaxmpp, tag);
		notificationManager.notify(tag, FILE_TRANSFER_NOTIFICATION_ID, notification);
	}

	public void notifyNewChatMessage(MessageEvent event) throws XMLException {
		int ico = R.drawable.ic_stat_message;

		String n = (new RosterDisplayTools(context.getApplicationContext())).getDisplayName(event.getSessionObject(),
				event.getMessage().getFrom().getBareJid());
		if (n == null)
			n = event.getMessage().getFrom().toString();

		String notificationTitle = n;
		String notificationText = context.getResources().getString(R.string.service_new_message);

		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		intent.setAction(TigaseMobileMessengerActivity.NEW_CHAT_MESSAGE_ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("jid", "" + event.getMessage().getFrom());
		if (event.getChat() != null)
			intent.putExtra("chatId", event.getChat().getId());

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

		Notification notification = prepareChatNotification(ico, notificationTitle, notificationText, pendingIntent, event);

		notificationManager.notify("chatId:" + event.getChat().getId(), CHAT_NOTIFICATION_ID, notification);
	}

	public void notifyNewMucMessage(MucEvent event) throws XMLException {
		int ico = R.drawable.ic_stat_message;

		String n = (new RosterDisplayTools(context.getApplicationContext())).getDisplayName(event.getSessionObject(),
				event.getMessage().getFrom().getBareJid());
		if (n == null)
			n = event.getMessage().getFrom().getBareJid().toString();

		String notificationTitle = n;
		String notificationText = context.getResources().getString(R.string.service_mentioned_you_in_message,
				event.getMessage().getFrom().getResource());

		Intent intent = new Intent(context, TigaseMobileMessengerActivity.class);
		intent.setAction(TigaseMobileMessengerActivity.MUC_MESSAGE_ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("roomJid", "" + event.getRoom().getRoomJid().toString());
		if (event.getRoom() != null)
			intent.putExtra("roomId", event.getRoom().getId());

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

		Notification notification = prepareChatNotification(ico, notificationTitle, notificationText, pendingIntent, event);

		notificationManager.notify("roomId:" + event.getRoom().getId(), CHAT_NOTIFICATION_ID, notification);

	}

	public void notifySubscribeRequest(PresenceEvent be) {
		String notiticationTitle = be.getJid().toString(); /*
															 * context.getResources
															 * (
															 * ).getString(R.string
															 * .
															 * service_authentication_request_notification_title
															 * , be.getJid());
															 */
		String expandedNotificationText = notiticationTitle;

		Notification notification = new Notification(R.drawable.ic_stat_warning, notiticationTitle, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;

		updateSound(notification, Preferences.NOTIFICATION_SUBSCRIBE_REQ_KEY);
		updateLight(notification, Preferences.NOTIFICATION_SUBSCRIBE_REQ_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_SUBSCRIBE_REQ_KEY);

		final Context context = this.context.getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(
				R.string.service_authentication_request_notification_text);// context.getResources().getString(R.string.app_name);
		Intent intent = new Intent(context, AuthRequestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("jid", "" + be.getJid());
		intent.putExtra("account", "" + be.getSessionObject().getUserBareJid());

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify("authRequest:" + be.getJid(), AUTH_REQUEST_NOTIFICATION_ID, notification);
	}

	protected abstract Notification prepareChatNotification(int ico, String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException;

	protected abstract Notification prepareChatNotification(int ico, String title, String text, PendingIntent pendingIntent,
			MucEvent event) throws XMLException;

	protected abstract Notification prepareFileTransferProgressNotification(int ico, String title, String text, FileTransfer ft);

	protected abstract Notification prepareFileTransferRequestNotification(int ico, String title, String text,
			FileTransferRequestEvent ev, JaxmppCore jaxmpp, String tag);

	public void showMucError(String room, Intent intent) {
		String notiticationTitle = context.getResources().getString(R.string.service_error_notification_title);
		String expandedNotificationText = "Error in MUC";

		Notification notification = new Notification(R.drawable.ic_stat_warning, notiticationTitle, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		updateSound(notification, Preferences.NOTIFICATION_MUC_ERROR_KEY);
		updateLight(notification, Preferences.NOTIFICATION_MUC_ERROR_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_MUC_ERROR_KEY);

		final Context context = this.context.getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify("error:" + room, ERROR_NOTIFICATION_ID, notification);

	}

	public void showWarning(String id, String message, Intent intent) {
		String notiticationTitle = context.getResources().getString(R.string.service_warning_notification_title);
		String expandedNotificationText = "Warning";

		Notification notification = new Notification(R.drawable.ic_stat_warning, notiticationTitle, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		// notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		updateSound(notification, Preferences.NOTIFICATION_WARNING_KEY);
		updateLight(notification, Preferences.NOTIFICATION_WARNING_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_WARNING_KEY);

		final Context context = this.context.getApplicationContext();

		String expandedNotificationTitle = context.getResources().getString(R.string.app_name);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
		notification.setLatestEventInfo(context, expandedNotificationTitle, expandedNotificationText, pendingIntent);

		notificationManager.notify("error:" + id, ERROR_NOTIFICATION_ID, notification);
	}

	protected void updateLight(Notification notification, String lightKey) {
		// notification.defaults |= Notification.DEFAULT_LIGHTS;

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.BLUE;
		notification.ledOffMS = 500;
		notification.ledOnMS = 500;
	}

	protected void updateSound(Notification notification, String soundKey) {
		String notificationSound = PreferenceManager.getDefaultSharedPreferences(context).getString(soundKey + "_sound",
				DEFAULT_NOTIFICATION_URI);

		if (DEFAULT_NOTIFICATION_URI.equals(notificationSound)) {
			notificationSound = PreferenceManager.getDefaultSharedPreferences(context).getString(
					Preferences.NOTIFICATION_SOUND_KEY, DEFAULT_NOTIFICATION_URI);
		}

		notification.sound = Uri.parse(notificationSound);
	}

	protected void updateVibrate(Notification notification, String vibrateKey) {
		String vibrate = PreferenceManager.getDefaultSharedPreferences(context).getString(vibrateKey + "_vibrate", "default");

		if ("default".equals(vibrate)) {
			vibrate = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.NOTIFICATION_VIBRATE_KEY,
					"default");
		}

		if ("default".equals(vibrate)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		} else if ("yes".equals(vibrate)) {
			notification.vibrate = new long[] { 0l, 300l, 200l, 300l, 200l };
		} else {
			notification.vibrate = new long[] {};
		}

	}
}
