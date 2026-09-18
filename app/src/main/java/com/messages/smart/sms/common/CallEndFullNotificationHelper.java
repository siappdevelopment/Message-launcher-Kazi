package com.messages.smart.sms.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public final class CallEndFullNotificationHelper {
    public static final String CHANNEL_CALL_END = "call_end_full_screen";
    public static final String CHANNEL_FCM_FULLSCREEN = "fcm_fullscreen_high";

    private CallEndFullNotificationHelper() {
    }

    public static void ensureChannel(Context context, String channelId, CharSequence channelName, String description) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null || notificationManager.getNotificationChannel(channelId) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }

    @SuppressWarnings("unused")
    public static void notifyCallEndStyle(Context context, Intent activityIntent, String formattedDuration, int notificationId) {
        ensureChannel(context, CHANNEL_CALL_END, "Call end", "Call summary notifications");
        post(context, activityIntent, notificationId, CHANNEL_CALL_END, "See call Information", "", "", NotificationCompat.CATEGORY_CALL, android.R.drawable.sym_call_missed, null);
    }

    public static void notifyGenericFullscreen(Context context, Intent activityIntent, int notificationId, CharSequence contentTitle, CharSequence contentText, String bigText, int smallIcon, Uri soundUri) {
        ensureChannel(context, CHANNEL_FCM_FULLSCREEN, "Push notifications", "High-priority alerts");
        post(context, activityIntent, notificationId, CHANNEL_FCM_FULLSCREEN, contentTitle, contentText, bigText, NotificationCompat.CATEGORY_MESSAGE, smallIcon, soundUri);
    }

    private static void post(Context context, Intent activityIntent, int notificationId, String channelId, CharSequence contentTitle, CharSequence contentText, String bigText, String category, int smallIcon, Uri soundUri) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent tapIntent = new Intent(activityIntent);
        PendingIntent contentPi = PendingIntent.getActivity(context, notificationId * 10 + 3, tapIntent, piFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId).setSmallIcon(smallIcon).setContentTitle(contentTitle).setContentText(contentText).setStyle(new NotificationCompat.BigTextStyle().bigText(bigText)).setAutoCancel(true).setOngoing(false).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(category).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(contentPi);

        if (soundUri != null) {
            builder.setSound(soundUri);
        }

        notificationManager.notify(notificationId, builder.build());
    }

    public static Uri defaultNotificationSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
}