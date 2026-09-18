package com.messages.smartsms.helpers;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;

import com.messages.smartsms.common.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SmsUnreadHelper {
    private static final String PREFS_NAME = "sms_read_state";
    private static final String KEY_LOCAL_READ_THREADS = "local_read_threads";

    private SmsUnreadHelper() {
    }

    public static Map<String, Integer> getUnreadCountsByThread(Context context) {
        Map<String, Integer> unreadCounts = new HashMap<>();
        if (context == null) {
            return unreadCounts;
        }
        Set<String> locallyReadThreads = getLocallyReadThreads(context);
        try (Cursor cursor = querySms(context, new String[]{"thread_id"}, "read = 0", null, null)) {
            if (cursor == null) {
                return unreadCounts;
            }
            int threadIndex = cursor.getColumnIndexOrThrow("thread_id");
            while (cursor.moveToNext()) {
                String threadId = cursor.getString(threadIndex);
                if (locallyReadThreads.contains(threadId)) {
                    continue;
                }
                if (MessageFilterHelper.isHiddenFromMainList(context, threadId)) {
                    continue;
                }
                unreadCounts.put(threadId, unreadCounts.getOrDefault(threadId, 0) + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unreadCounts;
    }

    public static int getThreadUnreadCount(Context context, String threadId) {
        if (context == null || TextUtils.isEmpty(threadId) || isLocallyRead(context, threadId)) {
            return 0;
        }
        try (Cursor cursor = querySms(context, new String[]{"_id"}, "thread_id = ? AND read = 0", new String[]{threadId}, null)) {
            if (cursor == null) {
                return 0;
            }
            return cursor.getCount();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void markThreadAsRead(Context context, String threadId, String address) {
        if (context == null || TextUtils.isEmpty(threadId)) {
            return;
        }

        markThreadAsReadInDatabase(context, threadId);
        markThreadAsReadLocally(context, threadId);
        cancelNotification(context, address);
    }

    public static void markAllAsRead(Context context) {
        if (context == null) {
            return;
        }

        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.READ, 1);
            context.getContentResolver().update(Uri.parse("content://sms"), values, "read = 0", null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Set<String> locallyReadThreads = new HashSet<>();
        try (Cursor cursor = querySms(context, new String[]{"thread_id"}, null, null, null)) {
            if (cursor != null) {
                int threadIndex = cursor.getColumnIndexOrThrow("thread_id");
                while (cursor.moveToNext()) {
                    locallyReadThreads.add(cursor.getString(threadIndex));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveLocallyReadThreads(context, locallyReadThreads);
    }

    public static void clearLocalReadForThread(Context context, String threadId) {
        if (context == null || TextUtils.isEmpty(threadId)) {
            return;
        }
        Set<String> locallyReadThreads = getLocallyReadThreads(context);
        if (locallyReadThreads.remove(threadId)) {
            saveLocallyReadThreads(context, locallyReadThreads);
        }
    }

    private static Cursor querySms(Context context, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            return context.getContentResolver().query(Uri.parse("content://sms"), projection, selection, selectionArgs, sortOrder);
        } catch (Exception first) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            try {
                return context.getContentResolver().query(Uri.parse("content://sms"), projection, selection, selectionArgs, sortOrder);
            } catch (Exception retry) {
                retry.printStackTrace();
                return null;
            }
        }
    }

    private static void markThreadAsReadInDatabase(Context context, String threadId) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.READ, 1);
            context.getContentResolver().update(Uri.parse("content://sms"), values, "thread_id = ? AND read = 0", new String[]{threadId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void markThreadAsReadLocally(Context context, String threadId) {
        Set<String> locallyReadThreads = getLocallyReadThreads(context);
        if (locallyReadThreads.add(threadId)) {
            saveLocallyReadThreads(context, locallyReadThreads);
        }
    }

    private static boolean isLocallyRead(Context context, String threadId) {
        return getLocallyReadThreads(context).contains(threadId);
    }

    private static Set<String> getLocallyReadThreads(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(preferences.getStringSet(KEY_LOCAL_READ_THREADS, new HashSet<>()));
    }

    private static void saveLocallyReadThreads(Context context, Set<String> threadIds) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putStringSet(KEY_LOCAL_READ_THREADS, new HashSet<>(threadIds)).apply();
    }

    private static void cancelNotification(Context context, String address) {
        if (TextUtils.isEmpty(address)) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(Utils.getNotificationId(address));
        }
    }
}