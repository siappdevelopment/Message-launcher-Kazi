package com.messages.smartsms.helpers;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.messages.smartsms.common.Utils;
import com.messages.smartsms.models.MessagesModel;

public final class ConversationDeleteHelper {
    private ConversationDeleteHelper() {
    }

    public static void deleteConversation(Context context, MessagesModel model) {
        if (model == null) {
            return;
        }
        deleteConversation(context, model.getThreadId(), model.getAddress());
    }

    public static void deleteConversation(Context context, String threadId, String address) {
        if (context == null || TextUtils.isEmpty(threadId)) {
            return;
        }

        if (ArchiveHelper.isArchived(context, threadId)) {
            ArchiveHelper.removeFromArchive(context, threadId);
        }
        if (BlockHelper.isBlocked(context, threadId)) {
            BlockHelper.removeBlocked(context, threadId);
        }
        if (RecycleBinHelper.isInRecycleBin(context, threadId)) {
            RecycleBinHelper.removeFromRecycleBin(context, threadId);
        }

        try {
            context.getContentResolver().delete(Uri.parse("content://sms"), "thread_id = ?", new String[]{threadId});
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(address)) {
            Utils.setDraft(context, address, "");
        }

        MessageListSyncHelper.notifyThreadDeleted(threadId);
    }
}