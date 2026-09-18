package com.messages.smartsms.models;

import android.graphics.drawable.Drawable;

public class AppsModel {
    public String appName;
    public String packageName;
    public Drawable appIcon;
    public long installTime;

    public AppsModel(String appName, String packageName, Drawable appIcon, long installTime) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.installTime = installTime;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public long getInstallTime() {
        return installTime;
    }
}