package com.messages.smartsms.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.messages.smartsms.common.Utils;

public class SMSService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            String phoneNumber = uri.getSchemeSpecificPart();
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(message)) {
                sendSms(phoneNumber, message);
            }
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendSms(String phoneNumber, String message) {
        Utils.sendSms(this, phoneNumber, message);
        try {
            ContentValues values = new ContentValues();
            values.put("address", phoneNumber);
            values.put("body", message);
            values.put("date", System.currentTimeMillis());
            values.put("type", 2);
            values.put("read", 1);
            getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}