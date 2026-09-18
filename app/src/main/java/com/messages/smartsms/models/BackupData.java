package com.messages.smartsms.models;

import java.util.ArrayList;
import java.util.List;

public class BackupData {
    private int messageCount;
    private List<BackupMessage> messages = new ArrayList<>();

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public List<BackupMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<BackupMessage> messages) {
        this.messages = messages;
    }
}