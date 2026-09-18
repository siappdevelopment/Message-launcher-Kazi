package com.messages.smart.sms.models;

public class ReminderModel {
    public String title;
    public long time;
    public int color;
    public int requestCode;

    public ReminderModel(String title, long time, int color, int requestCode) {
        this.title = title;
        this.time = time;
        this.color = color;
        this.requestCode = requestCode;
    }

    public String getTitle() {
        return title;
    }

    public long getTime() {
        return time;
    }

    public int getColor() {
        return color;
    }

    public int getRequestCode() {
        return requestCode;
    }
}