package com.messages.smart.sms.models;

public class ChatMessageItem {

    public static final int TYPE_DATE_HEADER = 0;
    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 2;

    private final int itemType;
    private final String body;
    private final long date;
    private final String dateLabel;
    private final long messageId;
    private boolean selected;

    private ChatMessageItem(int itemType, String body, long date, String dateLabel, long messageId) {
        this.itemType = itemType;
        this.body = body;
        this.date = date;
        this.dateLabel = dateLabel;
        this.messageId = messageId;
    }

    public static ChatMessageItem dateHeader(String dateLabel) {
        return new ChatMessageItem(TYPE_DATE_HEADER, null, 0, dateLabel, -1);
    }

    public static ChatMessageItem sent(String body, long date, long messageId) {
        return new ChatMessageItem(TYPE_SENT, body, date, null, messageId);
    }

    public static ChatMessageItem received(String body, long date, long messageId) {
        return new ChatMessageItem(TYPE_RECEIVED, body, date, null, messageId);
    }

    public int getItemType() {
        return itemType;
    }

    public String getBody() {
        return body;
    }

    public long getDate() {
        return date;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public long getMessageId() {
        return messageId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDateHeader() {
        return itemType == TYPE_DATE_HEADER;
    }

    public boolean isMessage() {
        return !isDateHeader();
    }
}