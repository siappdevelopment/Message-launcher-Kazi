package com.messages.smart.sms.helpers;

public final class MessageListSyncHelper {
    public interface Listener {
        void onOutboundMessage(String threadId, String name, String address, String body, long date, String photoUri);

        void onSyncRequired();

        void onThreadDeleted(String threadId);

        void onContactUpdated(String threadId, String name, String address, String photoUri);
    }

    private static Listener listener;

    private MessageListSyncHelper() {
    }

    public static void setListener(Listener syncListener) {
        listener = syncListener;
    }

    public static void notifyOutboundMessage(String threadId, String name, String address, String body, long date, String photoUri) {
        if (listener != null) {
            listener.onOutboundMessage(threadId, name, address, body, date, photoUri);
        }
    }

    public static void notifySyncRequired() {
        if (listener != null) {
            listener.onSyncRequired();
        }
    }

    public static void notifyThreadDeleted(String threadId) {
        if (listener != null) {
            listener.onThreadDeleted(threadId);
        }
    }

    public static void notifyContactUpdated(String threadId, String name, String address, String photoUri) {
        if (listener != null) {
            listener.onContactUpdated(threadId, name, address, photoUri);
        }
    }
}