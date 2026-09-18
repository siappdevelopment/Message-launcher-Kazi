package com.messages.smart.sms.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.messages.smart.sms.activities.MainActivity;
import com.messages.smart.sms.common.Utils;

public class MMSReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "android.provider.Telephony.WAP_PUSH_DELIVER".equals(intent.getAction())) {
            String mimeType = intent.getType();
            if ("application/vnd.wap.mms-message".equals(mimeType)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    byte[] data = intent.getByteArrayExtra("data");
                    Utils.isFromContacts = false;
                    Intent intentInside = new Intent(context, MainActivity.class);
                    intentInside.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intentInside);
                    Utils.clearActivityTransition(context);
                }
            }
        }
    }
}