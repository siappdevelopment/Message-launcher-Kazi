package com.messages.smartsms.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class PinHelper {
    private static final String PREFS_NAME = "message_pin_prefs";
    private static final String KEY_PINNED_IDS = "pinned_thread_ids";

    private PinHelper() {
    }

    public static Set<String> getPinnedIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> pinned = prefs.getStringSet(KEY_PINNED_IDS, null);
        if (pinned == null) {
            return new HashSet<>();
        }
        return new HashSet<>(pinned);
    }

    public static boolean isPinned(Context context, String threadId) {
        return getPinnedIds(context).contains(threadId);
    }

    public static void pin(Context context, String threadId) {
        Set<String> pinned = getPinnedIds(context);
        pinned.add(threadId);
        savePinnedIds(context, pinned);
    }

    public static void unpin(Context context, String threadId) {
        Set<String> pinned = getPinnedIds(context);
        pinned.remove(threadId);
        savePinnedIds(context, pinned);
    }

    private static void savePinnedIds(Context context, Set<String> pinnedIds) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putStringSet(KEY_PINNED_IDS, pinnedIds).apply();
    }
}