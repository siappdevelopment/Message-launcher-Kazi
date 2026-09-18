package com.messages.smartsms.helpers;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.messages.smartsms.common.Utils;
import com.messages.smartsms.models.MessagesModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BlockHelper {
    private static final String PREFS_NAME = "message_block_prefs";
    private static final String KEY_BLOCKED = "blocked_messages";

    public interface BlockChangeListener {
        void onBlocked(MessagesModel model);

        void onUnblocked(MessagesModel model);
    }

    private static BlockChangeListener listener;

    private BlockHelper() {
    }

    public static void setListener(BlockChangeListener blockListener) {
        listener = blockListener;
    }

    public static void block(Context context, MessagesModel model) {
        List<MessagesModel> blocked = getBlockedMessages(context);
        MessageJsonUtil.removeByThreadId(blocked, model.getThreadId());
        blocked.add(0, model);
        saveBlockedMessages(context, blocked);
        cancelNotification(context, model.getAddress());
        if (listener != null) {
            listener.onBlocked(model);
        }
    }

    public static void unblock(Context context, MessagesModel model) {
        List<MessagesModel> blocked = getBlockedMessages(context);
        MessageJsonUtil.removeByThreadId(blocked, model.getThreadId());
        saveBlockedMessages(context, blocked);
        if (listener != null) {
            listener.onUnblocked(model);
        }
    }

    public static void removeBlocked(Context context, String threadId) {
        List<MessagesModel> blocked = getBlockedMessages(context);
        MessageJsonUtil.removeByThreadId(blocked, threadId);
        saveBlockedMessages(context, blocked);
    }

    public static Set<String> getBlockedIds(Context context) {
        Set<String> ids = new HashSet<>();
        for (MessagesModel model : getBlockedMessages(context)) {
            ids.add(model.getThreadId());
        }
        return ids;
    }

    public static boolean isBlocked(Context context, String threadId) {
        if (TextUtils.isEmpty(threadId)) {
            return false;
        }
        return getBlockedIds(context).contains(threadId);
    }

    public static boolean isBlockedByAddress(Context context, String address) {
        if (context == null || TextUtils.isEmpty(address)) {
            return false;
        }
        try {
            String threadId = String.valueOf(Telephony.Threads.getOrCreateThreadId(context, address));
            if (isBlocked(context, threadId)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (MessagesModel model : getBlockedMessages(context)) {
            if (model.getAddress() != null && PhoneNumberUtils.compare(model.getAddress(), address)) {
                return true;
            }
        }
        return false;
    }

    public static List<MessagesModel> getBlockedMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new ArrayList<>(MessageJsonUtil.fromJsonString(prefs.getString(KEY_BLOCKED, "[]")));
    }

    private static void saveBlockedMessages(Context context, List<MessagesModel> messages) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_BLOCKED, MessageJsonUtil.toJsonString(messages)).apply();
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