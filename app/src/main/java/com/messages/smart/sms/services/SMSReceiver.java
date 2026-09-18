package com.messages.smart.sms.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;

import com.messages.smart.sms.R;
import com.messages.smart.sms.activities.MessagesContentActivity;
import com.messages.smart.sms.activities.MessagesPopActivity;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.helpers.BlockHelper;
import com.messages.smart.sms.helpers.ContactUtils;
import com.messages.smart.sms.helpers.SmsUnreadHelper;

public class SMSReceiver extends BroadcastReceiver {
    private static final String REFRESH_SMS_ACTION = "com.messages.smart.sms.REFRESH_SMS";
    private static final String SMS_CHANNEL_ID = "sms_channel";

    public interface SmsListener {
        void onSmsReceived();
    }

    private static SmsListener listener;

    public static void setListener(SmsListener smsListener) {
        listener = smsListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        boolean isDefaultSmsApp = isDefaultSmsApp(context);

        if (isDefaultSmsApp && Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            return;
        }

        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action) && !Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        String sender = "";
        StringBuilder messageBody = new StringBuilder();
        long timestamp = 0;

        for (int i = 0; i < pdus.length; i++) {
            SmsMessage smsMessage;
            String format = bundle.getString("format");
            smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            if (i == 0) {
                sender = smsMessage.getDisplayOriginatingAddress();
                timestamp = smsMessage.getTimestampMillis();
            }
            messageBody.append(smsMessage.getMessageBody());
        }

        String message = messageBody.toString();
        if (sender == null) {
            sender = "";
        }
        sender = sender.trim();
        if (sender.isEmpty()) {
            sender = "Unknown";
        }

        String threadId = resolveThreadId(context, sender);

        if (BlockHelper.isBlockedByAddress(context, sender)) {
            return;
        }

        try {
            if (!threadId.isEmpty()) {
                SmsUnreadHelper.clearLocalReadForThread(context, threadId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            try {
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, sender);
                values.put(Telephony.Sms.BODY, message);
                values.put(Telephony.Sms.DATE, timestamp);
                values.put(Telephony.Sms.READ, 0);
                values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
                context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showNotification(context, sender, message, threadId);

        Intent refreshIntent = new Intent(REFRESH_SMS_ACTION);
        refreshIntent.setPackage(context.getPackageName());
        context.sendBroadcast(refreshIntent);

        if (listener != null) {
            listener.onSmsReceived();
        }
    }

    private boolean isDefaultSmsApp(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = context.getSystemService(RoleManager.class);
                return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
            return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String resolveThreadId(Context context, String sender) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return String.valueOf(Telephony.Threads.getOrCreateThreadId(context, sender));
            } catch (Exception e) {
                e.printStackTrace();
                if (attempt < 2) {
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return "";
    }

    private void showNotification(Context context, String sender, String message, String threadId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(SMS_CHANNEL_ID, "SMS Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String contactName = sender;
        try {
            contactName = ContactUtils.lookup(context, sender).getName();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent contentIntent = new Intent(context, MessagesContentActivity.class);
        contentIntent.putExtra(MessagesContentActivity.EXTRA_THREAD_ID, threadId);
        contentIntent.putExtra(MessagesContentActivity.EXTRA_NAME, contactName);
        contentIntent.putExtra(MessagesContentActivity.EXTRA_ADDRESS, sender);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), contentIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Intent popIntent = new Intent(context, MessagesPopActivity.class);
        popIntent.putExtra("name", contactName);
        popIntent.putExtra("number", sender);
        popIntent.putExtra("message", message);
        popIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        boolean isCurrentConversationOpen = isCurrentConversationOpen(sender);
        if (!isCurrentConversationOpen) {
            try {
                context.startActivity(popIntent);
                Utils.clearActivityTransition(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final Context appContext = context.getApplicationContext();
        final String finalContactName = contactName;
        final String finalMessage = message;
        final PendingIntent finalPendingIntent = pendingIntent;
        final int notificationId = Utils.getNotificationId(sender);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SMS_CHANNEL_ID).setSmallIcon(R.drawable.img_app_icon).setContentTitle(contactName).setContentText(message).setAutoCancel(true).setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_MESSAGE);
        notificationManager.notify(notificationId, builder.build());
        if (!isCurrentConversationOpen) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null) {
                        NotificationCompat.Builder updateBuilder = new NotificationCompat.Builder(appContext, SMS_CHANNEL_ID).setSmallIcon(R.drawable.img_app_icon).setContentTitle(finalContactName).setContentText(finalMessage).setAutoCancel(true).setContentIntent(finalPendingIntent).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_MESSAGE);
                        manager.notify(notificationId, updateBuilder.build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 3000);
        }
    }

    private boolean isCurrentConversationOpen(String sender) {
        if (MessagesContentActivity.currentOpenNumber == null || MessagesContentActivity.currentOpenNumber.isEmpty()) {
            return false;
        }
        return PhoneNumberUtils.compare(sender, MessagesContentActivity.currentOpenNumber) || sender.equals(MessagesContentActivity.currentOpenNumber);
    }
}