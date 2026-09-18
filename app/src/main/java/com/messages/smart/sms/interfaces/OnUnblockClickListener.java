package com.messages.smart.sms.interfaces;

import com.messages.smart.sms.models.MessagesModel;

import java.util.ArrayList;

public interface OnUnblockClickListener {
    void onUnblockClick(ArrayList<MessagesModel> arrayListMessagesModel, int position);
}