package com.messages.smartsms.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;

public final class SmsSendHelper {
    private SmsSendHelper() {
    }

    public static String ensureThreadId(Context context, String address, String threadId) {
        if (threadId != null && !threadId.isEmpty()) {
            return threadId;
        }
        return String.valueOf(Telephony.Threads.getOrCreateThreadId(context, address));
    }

    public static void sendAndStore(Context context, String address, String body, String threadId, long date) {
        SmsManager.getDefault().sendTextMessage(address, null, body, null, null);
        insertSentMessage(context, address, body, threadId, date);
    }

    public static void insertSentMessage(Context context, String address, String body, String threadId, long date) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, address);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, date);
        values.put(Telephony.Sms.DATE_SENT, date);
        values.put(Telephony.Sms.READ, 1);
        values.put(Telephony.Sms.SEEN, 1);
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);
        values.put(Telephony.Sms.THREAD_ID, Long.parseLong(threadId));

        try {
            Uri inserted = context.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
            if (inserted == null) {
                context.getContentResolver().insert(Uri.parse("content://sms"), values);
            }
        } catch (Exception ignored) {
            try {
                context.getContentResolver().insert(Uri.parse("content://sms"), values);
            } catch (Exception ignoredAgain) {
            }
        }
    }
}