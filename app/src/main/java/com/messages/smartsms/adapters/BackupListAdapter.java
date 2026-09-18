package com.messages.smartsms.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smartsms.R;
import com.messages.smartsms.models.BackupFileInfo;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
    public interface OnBackupClickListener {
        void onBackupClick(BackupFileInfo backupFileInfo);
    }

    private final List<BackupFileInfo> backupFiles = new ArrayList<>();
    private final OnBackupClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());

    public BackupListAdapter(OnBackupClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<BackupFileInfo> items) {
        backupFiles.clear();
        if (items != null) {
            backupFiles.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.backup_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BackupFileInfo info = backupFiles.get(position);
        holder.tvBackupDate.setText(dateFormat.format(new Date(info.getLastModified())));
        holder.tvBackupCount.setText(holder.itemView.getContext().getString(R.string.backup_message_count, info.getMessageCount()));
        holder.tvBackupSize.setText(formatSize(info.getSize()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBackupClick(info);
            }
        });
    }

    @Override
    public int getItemCount() {
        return backupFiles.size();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f);
        }
        return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvBackupDate, tvBackupCount, tvBackupSize;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBackupDate = itemView.findViewById(R.id.tvBackupDate);
            tvBackupCount = itemView.findViewById(R.id.tvBackupCount);
            tvBackupSize = itemView.findViewById(R.id.tvBackupSize);
        }
    }
}