package com.messages.smart.sms.interfaces;

import com.messages.smart.sms.models.MessagesModel;

public interface OnMessageOpenListener {
    void onMessageOpen(MessagesModel messagesModel);
}