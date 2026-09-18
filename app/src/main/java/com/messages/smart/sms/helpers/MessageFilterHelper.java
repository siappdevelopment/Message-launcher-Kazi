package com.messages.smart.sms.helpers;

import android.content.Context;

public final class MessageFilterHelper {
    private MessageFilterHelper() {
    }

    public static boolean isHiddenFromMainList(Context context, String threadId) {
        return ArchiveHelper.isArchived(context, threadId) || BlockHelper.isBlocked(context, threadId) || RecycleBinHelper.isInRecycleBin(context, threadId);
    }
}