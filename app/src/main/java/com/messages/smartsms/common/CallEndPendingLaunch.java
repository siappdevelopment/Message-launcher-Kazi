package com.messages.smartsms.common;

import android.app.NotificationManager;
import android.content.Context;

public final class CallEndPendingLaunch {
    private static final String PREFS = "call_end_pending_launch";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_NUMBER = "mobile_number";
    private static final String KEY_START = "start_ms";
    private static final String KEY_END = "end_ms";
    private static final String KEY_TYPE = "call_type";
    private static final String KEY_DURATION = "formatted_duration";

    public static final int NOTIFICATION_ID = 1001;

    private CallEndPendingLaunch() {
    }

    public static void save(Context context, String number, long startMs, long endMs, String callType, String formattedDuration) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PENDING, true).putString(KEY_NUMBER, number != null ? number : "").putLong(KEY_START, startMs).putLong(KEY_END, endMs).putString(KEY_TYPE, callType != null ? callType : "").putString(KEY_DURATION, formattedDuration != null ? formattedDuration : "00:00").apply();
    }

    public static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void cancelCallEndNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
}