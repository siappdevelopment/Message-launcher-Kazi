package com.messages.smart.sms.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.messages.smart.sms.models.BackupData;
import com.messages.smart.sms.models.BackupFileInfo;
import com.messages.smart.sms.models.BackupMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BackupHelper {
    private static final String PREFS_NAME = "sms_backup_prefs";
    private static final String KEY_TREE_URI = "backup_tree_uri";
    private static final String KEY_FOLDER_NAME = "backup_folder_name";
    private static final String BACKUP_PREFIX = "backup-";
    private static final Gson GSON = new Gson();

    public interface ProgressListener {
        void onProgress(String stage, int current, int total);
    }

    private BackupHelper() {
    }

    public static void saveBackupLocation(Context context, Uri treeUri, String folderName) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TREE_URI, treeUri.toString()).putString(KEY_FOLDER_NAME, folderName).apply();
    }

    public static Uri getBackupTreeUri(Context context) {
        String uri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TREE_URI, null);
        return uri != null ? Uri.parse(uri) : null;
    }

    public static String getBackupFolderName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FOLDER_NAME, null);
    }

    public static boolean hasBackupLocation(Context context) {
        return getBackupTreeUri(context) != null;
    }

    public static BackupData readAllSms(Context context, ProgressListener listener) {
        BackupData backupData = new BackupData();
        List<BackupMessage> messages = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC");
        if (cursor != null) {
            int total = cursor.getCount();
            int index = 0;
            while (cursor.moveToNext()) {
                BackupMessage message = cursorToMessage(cursor);
                messages.add(message);
                index++;
                if (listener != null && (index % 50 == 0 || index == total)) {
                    listener.onProgress("running", index, total);
                }
            }
            cursor.close();
        }

        backupData.setMessages(messages);
        backupData.setMessageCount(messages.size());
        return backupData;
    }

    public static Uri writeBackup(Context context, Uri treeUri, BackupData backupData, ProgressListener listener) throws Exception {
        DocumentFile folder = DocumentFile.fromTreeUri(context, treeUri);
        if (folder == null || !folder.canWrite()) {
            throw new IllegalStateException("Cannot write to backup folder");
        }

        String fileName = BACKUP_PREFIX + new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date()) + ".json";
        DocumentFile existing = folder.findFile(fileName);
        if (existing != null) {
            existing.delete();
        }

        DocumentFile backupFile = folder.createFile("application/json", fileName);
        if (backupFile == null) {
            throw new IllegalStateException("Could not create backup file");
        }

        if (listener != null) {
            listener.onProgress("saving", backupData.getMessageCount(), backupData.getMessageCount());
        }

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(backupFile.getUri()); OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            GSON.toJson(backupData, writer);
            writer.flush();
        }

        return backupFile.getUri();
    }

    public static List<BackupFileInfo> listBackupFiles(Context context, Uri treeUri) {
        List<BackupFileInfo> files = new ArrayList<>();
        DocumentFile folder = DocumentFile.fromTreeUri(context, treeUri);
        if (folder == null) {
            return files;
        }

        DocumentFile[] children = folder.listFiles();
        if (children == null) {
            return files;
        }

        Arrays.sort(children, Comparator.comparingLong(DocumentFile::lastModified).reversed());

        for (DocumentFile file : children) {
            if (file.isFile() && file.getName() != null && file.getName().endsWith(".json")) {
                int messageCount = readMessageCount(context, file.getUri());
                files.add(new BackupFileInfo(file.getUri(), file.getName(), file.lastModified(), file.length(), messageCount));
            }
        }
        return files;
    }

    public static BackupData readBackupFile(Context context, Uri fileUri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            BackupData backupData = GSON.fromJson(reader, BackupData.class);
            if (backupData == null || backupData.getMessages() == null) {
                throw new IllegalStateException("Invalid backup file");
            }
            if (backupData.getMessageCount() <= 0) {
                backupData.setMessageCount(backupData.getMessages().size());
            }
            return backupData;
        }
    }

    public static int restoreBackup(Context context, BackupData backupData, ProgressListener listener, AtomicBoolean cancelFlag) {
        List<BackupMessage> messages = backupData.getMessages();
        int total = messages.size();
        int restored = 0;

        if (listener != null) {
            listener.onProgress("parsing", total, total);
        }

        for (int i = 0; i < total; i++) {
            if (cancelFlag != null && cancelFlag.get()) {
                break;
            }
            BackupMessage message = messages.get(i);
            if (insertMessage(context, message)) {
                restored++;
            }
            if (listener != null && (i % 50 == 0 || i == total - 1)) {
                listener.onProgress("running", i + 1, total);
            }
        }

        if (cancelFlag != null && cancelFlag.get()) {
            return restored;
        }

        if (listener != null) {
            listener.onProgress("syncing", total, total);
        }
        return restored;
    }

    private static BackupMessage cursorToMessage(Cursor cursor) {
        BackupMessage message = new BackupMessage();
        message.setAddress(getString(cursor, Telephony.Sms.ADDRESS));
        message.setBody(getString(cursor, Telephony.Sms.BODY));
        message.setDate(getLong(cursor, Telephony.Sms.DATE));
        message.setDateSent(getLong(cursor, Telephony.Sms.DATE_SENT));
        message.setLocked(getInt(cursor, Telephony.Sms.LOCKED) == 1);
        message.setProtocol(getInt(cursor, Telephony.Sms.PROTOCOL));
        message.setRead(getInt(cursor, Telephony.Sms.READ) == 1);
        message.setStatus(getInt(cursor, Telephony.Sms.STATUS));
        message.setSubId(getSubId(cursor));
        message.setType(getInt(cursor, Telephony.Sms.TYPE));
        return message;
    }

    private static boolean insertMessage(Context context, BackupMessage message) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, message.getAddress() != null ? message.getAddress() : "");
        values.put(Telephony.Sms.BODY, message.getBody() != null ? message.getBody() : "");
        values.put(Telephony.Sms.DATE, message.getDate());
        values.put(Telephony.Sms.DATE_SENT, message.getDateSent());
        values.put(Telephony.Sms.LOCKED, message.isLocked() ? 1 : 0);
        values.put(Telephony.Sms.PROTOCOL, message.getProtocol());
        values.put(Telephony.Sms.READ, message.isRead() ? 1 : 0);
        values.put(Telephony.Sms.STATUS, message.getStatus());
        values.put(Telephony.Sms.TYPE, message.getType());

        if (message.getSubId() >= 0) {
            values.put(Telephony.Sms.SUBSCRIPTION_ID, message.getSubId());
        }

        try {
            Uri inserted = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
            return inserted != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int readMessageCount(Context context, Uri fileUri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri); JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("messageCount".equals(name)) {
                    return reader.nextInt();
                }
                reader.skipValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 ? cursor.getString(index) : "";
    }

    private static long getLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 ? cursor.getLong(index) : 0L;
    }

    private static int getInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    private static int getSubId(Cursor cursor) {
        int index = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID);
        if (index < 0) {
            index = cursor.getColumnIndex("sub_id");
        }
        return index >= 0 ? cursor.getInt(index) : -1;
    }
}