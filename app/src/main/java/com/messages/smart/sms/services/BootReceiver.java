package com.messages.smart.sms.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.messages.smart.sms.models.ReminderModel;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = context.getSharedPreferences("reminder_preferences", Context.MODE_PRIVATE);
            String json = preferences.getString("reminder_list", "");
            if (json.isEmpty()) {
                return;
            }

            Type type = new TypeToken<ArrayList<ReminderModel>>() {
            }.getType();
            ArrayList<ReminderModel> reminderList = new Gson().fromJson(json, type);
            if (reminderList == null || reminderList.isEmpty()) {
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            for (int i = 0; i < reminderList.size(); i++) {
                ReminderModel reminder = reminderList.get(i);
                if (reminder.getTime() <= System.currentTimeMillis()) {
                    continue;
                }

                Intent reminderIntent = new Intent(context, ReminderReceiver.class);
                reminderIntent.putExtra("title", reminder.getTitle());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.getRequestCode(), reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.getTime(), pendingIntent);
                }
            }
        }
    }
}