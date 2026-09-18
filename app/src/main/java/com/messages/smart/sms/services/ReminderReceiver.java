package com.messages.smart.sms.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messages.smart.sms.R;
import com.messages.smart.sms.activities.MainActivity;
import com.messages.smart.sms.models.ReminderModel;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        int requestCode = intent.getIntExtra("requestCode", -1);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, requestCode, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.img_app_icon).setContentTitle("Reminder").setContentText(title).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(contentPendingIntent);
        notificationManager.notify(requestCode, builder.build());

        SharedPreferences preferences = context.getSharedPreferences("reminder_preferences", Context.MODE_PRIVATE);
        String json = preferences.getString("reminder_list", "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<ReminderModel>>() {
            }.getType();
            ArrayList<ReminderModel> reminderList = new Gson().fromJson(json, type);
            if (reminderList != null) {
                for (int i = 0; i < reminderList.size(); i++) {
                    if (reminderList.get(i).getRequestCode() == requestCode) {
                        reminderList.remove(i);
                        break;
                    }
                }

                preferences.edit().putString("reminder_list", new Gson().toJson(reminderList)).apply();
            }
        }
    }
}