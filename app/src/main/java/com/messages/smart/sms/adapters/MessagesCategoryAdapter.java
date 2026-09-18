package com.messages.smart.sms.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.models.MessagesCategoryModel;

import java.util.List;

public class MessagesCategoryAdapter extends RecyclerView.Adapter<MessagesCategoryAdapter.ViewHolder> {
    public interface OnCategoryClickListener {
        void onCategoryClick(int position);
    }

    private final Context context;
    private final List<MessagesCategoryModel> categories;
    private final OnCategoryClickListener listener;

    public MessagesCategoryAdapter(Context context, List<MessagesCategoryModel> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    public void updateCategoryLabel(int position, String label) {
        if (position >= 0 && position < categories.size()) {
            categories.get(position).setName(label);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_messages_category, parent, false));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void selectTab(int position) {
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setSelected(i == position);
        }
        notifyDataSetChanged();
    }

    @SuppressLint({"RecyclerView", "NotifyDataSetChanged"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (categories.get(position).isSelected()) {
            holder.llMessageCategoryName.setBackgroundResource(R.drawable.custom_tab_select);
            holder.tvMessageCategoryName.setTextColor(context.getResources().getColor(R.color.primary));
        } else {
            holder.llMessageCategoryName.setBackgroundResource(R.drawable.custom_tab_un_select);
            holder.tvMessageCategoryName.setTextColor(context.getResources().getColor(R.color.gray));
        }

        holder.tvMessageCategoryName.setText(categories.get(position).getName());

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onCategoryClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llMessageCategoryName;
        private final AppCompatTextView tvMessageCategoryName;

        public ViewHolder(View itemView) {
            super(itemView);
            llMessageCategoryName = itemView.findViewById(R.id.llMessageCategoryName);
            tvMessageCategoryName = itemView.findViewById(R.id.tvMessageCategoryName);
        }
    }
}