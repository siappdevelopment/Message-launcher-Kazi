package com.messages.smart.sms.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.messages.smart.sms.models.MessagesModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecycleBinHelper {
    private static final String PREFS_NAME = "message_recycle_bin_prefs";
    private static final String KEY_RECYCLE_BIN = "recycle_bin_messages";

    public interface RecycleBinChangeListener {
        void onMovedToBin(MessagesModel model);

        void onRestoredFromBin(MessagesModel model);
    }

    private static RecycleBinChangeListener listener;

    private RecycleBinHelper() {
    }

    public static void setListener(RecycleBinChangeListener recycleBinListener) {
        listener = recycleBinListener;
    }

    public static void moveToBin(Context context, MessagesModel model) {
        List<MessagesModel> binMessages = getRecycleBinMessages(context);
        MessageJsonUtil.removeByThreadId(binMessages, model.getThreadId());
        binMessages.add(0, model);
        saveRecycleBinMessages(context, binMessages);
        if (listener != null) {
            listener.onMovedToBin(model);
        }
    }

    public static void restoreFromBin(Context context, MessagesModel model) {
        model.setSelected(false);
        List<MessagesModel> binMessages = getRecycleBinMessages(context);
        MessageJsonUtil.removeByThreadId(binMessages, model.getThreadId());
        saveRecycleBinMessages(context, binMessages);
        if (listener != null) {
            listener.onRestoredFromBin(model);
        }
    }

    public static void removeFromRecycleBin(Context context, String threadId) {
        List<MessagesModel> binMessages = getRecycleBinMessages(context);
        MessageJsonUtil.removeByThreadId(binMessages, threadId);
        saveRecycleBinMessages(context, binMessages);
    }

    public static void permanentlyDelete(Context context, MessagesModel model) {
        ConversationDeleteHelper.deleteConversation(context, model);
    }

    public static Set<String> getRecycleBinIds(Context context) {
        Set<String> ids = new HashSet<>();
        for (MessagesModel model : getRecycleBinMessages(context)) {
            ids.add(model.getThreadId());
        }
        return ids;
    }

    public static boolean isInRecycleBin(Context context, String threadId) {
        return getRecycleBinIds(context).contains(threadId);
    }

    public static List<MessagesModel> getRecycleBinMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new ArrayList<>(MessageJsonUtil.fromJsonString(prefs.getString(KEY_RECYCLE_BIN, "[]")));
    }

    private static void saveRecycleBinMessages(Context context, List<MessagesModel> messages) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_RECYCLE_BIN, MessageJsonUtil.toJsonString(messages)).apply();
    }
}