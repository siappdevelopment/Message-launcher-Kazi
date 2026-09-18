package com.messages.smartsms.models;

import android.net.Uri;

public class BackupFileInfo {
    private final Uri uri;
    private final String name;
    private final long lastModified;
    private final long size;
    private final int messageCount;

    public BackupFileInfo(Uri uri, String name, long lastModified, long size, int messageCount) {
        this.uri = uri;
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.messageCount = messageCount;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public int getMessageCount() {
        return messageCount;
    }
}