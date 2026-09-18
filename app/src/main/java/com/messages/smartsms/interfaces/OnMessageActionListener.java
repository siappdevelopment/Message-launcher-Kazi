package com.messages.smartsms.interfaces;

public interface OnMessageActionListener {
    void onMessageClick(int position, int selectedCount);

    void onMessageLongClick(int position, int selectedCount);
}