package com.messages.smartsms.helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public final class ContactUtils {
    public static final class ContactDetails {
        private final String name;
        private final String photoUri;

        public ContactDetails(String name, String photoUri) {
            this.name = name;
            this.photoUri = photoUri;
        }

        public String getName() {
            return name;
        }

        public String getPhotoUri() {
            return photoUri;
        }
    }

    private ContactUtils() {
    }

    public static ContactDetails lookup(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null) {
            return new ContactDetails(phoneNumber == null ? "" : phoneNumber, null);
        }

        String normalized = phoneNumber.trim();
        if (normalized.isEmpty()) {
            return new ContactDetails("", null);
        }

        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized));
            Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(0);
                        String photoUri = cursor.getString(1);
                        if (name == null || name.isEmpty()) {
                            name = normalized;
                        }
                        return new ContactDetails(name, photoUri);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ContactDetails(normalized, null);
    }

    public static boolean isSavedInContacts(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null) {
            return false;
        }
        String normalized = phoneNumber.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized));
            Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);
            if (cursor != null) {
                try (cursor) {
                    return cursor.moveToFirst();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}