package com.messages.smart.sms.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.messages.smart.sms.models.MessagesModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArchiveHelper {
    private static final String PREFS_NAME = "message_archive_prefs";
    private static final String KEY_ARCHIVED = "archived_messages";

    public interface ArchiveChangeListener {
        void onArchived(MessagesModel model);

        void onUnarchived(MessagesModel model);
    }

    private static ArchiveChangeListener listener;

    private ArchiveHelper() {
    }

    public static void setListener(ArchiveChangeListener archiveListener) {
        listener = archiveListener;
    }

    public static void archive(Context context, MessagesModel model) {
        model.setSelected(false);
        List<MessagesModel> archived = getArchivedMessages(context);
        MessageJsonUtil.removeByThreadId(archived, model.getThreadId());
        archived.add(0, model);
        saveArchivedMessages(context, archived);
        if (listener != null) {
            listener.onArchived(model);
        }
    }

    public static void unarchive(Context context, MessagesModel model) {
        model.setSelected(false);
        removeFromArchive(context, model.getThreadId());
        if (listener != null) {
            listener.onUnarchived(model);
        }
    }

    public static void removeFromArchive(Context context, String threadId) {
        List<MessagesModel> archived = getArchivedMessages(context);
        MessageJsonUtil.removeByThreadId(archived, threadId);
        saveArchivedMessages(context, archived);
    }

    public static Set<String> getArchivedIds(Context context) {
        Set<String> ids = new HashSet<>();
        for (MessagesModel model : getArchivedMessages(context)) {
            ids.add(model.getThreadId());
        }
        return ids;
    }

    public static boolean isArchived(Context context, String threadId) {
        return getArchivedIds(context).contains(threadId);
    }

    public static List<MessagesModel> getArchivedMessages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new ArrayList<>(MessageJsonUtil.fromJsonString(prefs.getString(KEY_ARCHIVED, "[]")));
    }

    private static void saveArchivedMessages(Context context, List<MessagesModel> messages) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ARCHIVED, MessageJsonUtil.toJsonString(messages)).apply();
    }
}