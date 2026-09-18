package com.messages.smartsms.interfaces;

import com.messages.smartsms.models.MessagesModel;

import java.util.ArrayList;

public interface OnUnblockClickListener {
    void onUnblockClick(ArrayList<MessagesModel> arrayListMessagesModel, int position);
}