package com.messages.smart.sms.models;

public class MessagesModel {
    private final String threadId;
    private final String name;
    private final String address;
    private final String body;
    private final long date;
    private final int type;
    private final int read;
    private final String contactPhotoUri;
    public String category;
    private boolean selected;
    private int unreadCount;
    private boolean pinned;

    public MessagesModel(String threadId, String name, String address, String body, long date, int type, int read, String contactPhotoUri) {
        this(threadId, name, address, body, date, type, read, contactPhotoUri, 0);
    }

    public MessagesModel(String threadId, String name, String address, String body, long date, int type, int read, String contactPhotoUri, int unreadCount) {
        this.threadId = threadId;
        this.name = name;
        this.address = address;
        this.body = body;
        this.date = date;
        this.type = type;
        this.read = read;
        this.contactPhotoUri = contactPhotoUri;
        this.unreadCount = unreadCount;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getBody() {
        return body;
    }

    public long getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    public int getRead() {
        return read;
    }

    public String getContactPhotoUri() {
        return contactPhotoUri;
    }

    public boolean hasContactPhoto() {
        return contactPhotoUri != null && !contactPhotoUri.isEmpty();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}