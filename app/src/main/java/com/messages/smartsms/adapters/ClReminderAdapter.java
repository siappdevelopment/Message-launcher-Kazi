package com.messages.smartsms.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smartsms.R;
import com.messages.smartsms.interfaces.OnReminderDeleteListener;
import com.messages.smartsms.models.ReminderModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ClReminderAdapter extends RecyclerView.Adapter<ClReminderAdapter.MyViewHolder> {
    private final Context context;
    private final ArrayList<ReminderModel> arrayListReminder;
    private final OnReminderDeleteListener onReminderDeleteListener;

    public ClReminderAdapter(Context context, ArrayList<ReminderModel> arrayListReminder, OnReminderDeleteListener onReminderDeleteListener) {
        this.context = context;
        this.arrayListReminder = arrayListReminder;
        this.onReminderDeleteListener = onReminderDeleteListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_clreminder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ReminderModel reminderModel = arrayListReminder.get(position);

        String date = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date(reminderModel.getTime()));

        holder.ivColor.setBackgroundTintList(ColorStateList.valueOf(reminderModel.getColor()));
        holder.tvTitle.setText(reminderModel.getTitle());
        holder.tvTime.setText(date);

        holder.ivDeleteReminder.setOnClickListener(v -> {
            if (onReminderDeleteListener != null) {
                onReminderDeleteListener.onReminderDeleteListener(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayListReminder.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivColor, ivDeleteReminder;
        private final AppCompatTextView tvTitle, tvTime;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivColor = itemView.findViewById(R.id.ivColor);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivDeleteReminder = itemView.findViewById(R.id.ivDeleteReminder);
        }
    }
}