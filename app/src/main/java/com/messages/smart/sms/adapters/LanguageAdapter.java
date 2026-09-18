package com.messages.smart.sms.adapters;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.interfaces.OnLanguageClickListener;

import java.util.ArrayList;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Integer> arrayListIcon;
    private final ArrayList<String> arrayListName;
    private final ArrayList<String> arrayListSubName;
    private final ArrayList<String> arrayListCode;
    private final OnLanguageClickListener onLanguageClickListenerNext;

    private boolean isItemClick = false;
    private int selectedPosition = -1;

    public LanguageAdapter(Context context, ArrayList<Integer> arrayListIcon, ArrayList<String> arrayListName, ArrayList<String> arrayListSubName, ArrayList<String> arrayListCode, OnLanguageClickListener onLanguageClickListenerNext) {
        this.context = context;
        this.arrayListIcon = arrayListIcon;
        this.arrayListName = arrayListName;
        this.arrayListSubName = arrayListSubName;
        this.arrayListCode = arrayListCode;
        this.onLanguageClickListenerNext = onLanguageClickListenerNext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_language, parent, false));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (isItemClick) {
            if (selectedPosition == position) {
                holder.tvLanguageName.setTextColor(context.getResources().getColor(R.color.primary));
                holder.ivLanguageSelect.setImageResource(R.drawable.ic_language_select);
            } else {
                holder.tvLanguageName.setTextColor(context.getResources().getColor(R.color.black));
                holder.ivLanguageSelect.setImageResource(R.drawable.ic_language_un_select);
            }
        } else {
            if (Utils.getAppLanguageNew(context).equals(arrayListCode.get(position))) {
                holder.tvLanguageName.setTextColor(context.getResources().getColor(R.color.primary));
                holder.ivLanguageSelect.setImageResource(R.drawable.ic_language_select);
            } else {
                holder.tvLanguageName.setTextColor(context.getResources().getColor(R.color.black));
                holder.ivLanguageSelect.setImageResource(R.drawable.ic_language_un_select);
            }
        }

        holder.ivLanguageIcon.setImageResource(arrayListIcon.get(position));
        holder.tvLanguageName.setText(arrayListName.get(position));
        holder.tvLanguageSubName.setText(arrayListSubName.get(position));

        if (position == arrayListIcon.size() - 1) {
            holder.ivDividerLine.setVisibility(INVISIBLE);
        } else {
            holder.ivDividerLine.setVisibility(VISIBLE);
        }

        holder.llLanguageSelect.setOnClickListener(view -> {
            isItemClick = true;
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            onLanguageClickListenerNext.onLanguageClick(arrayListCode.get(position));
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedLanguage(String languageCode) {
        isItemClick = true;
        selectedPosition = arrayListCode.indexOf(languageCode);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetSelection(String languageCode) {
        isItemClick = false;
        selectedPosition = arrayListCode.indexOf(languageCode);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return arrayListIcon.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llLanguageSelect;
        private final AppCompatImageView ivLanguageIcon, ivLanguageSelect, ivDividerLine;
        private final AppCompatTextView tvLanguageName, tvLanguageSubName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llLanguageSelect = itemView.findViewById(R.id.llLanguageSelect);
            ivLanguageIcon = itemView.findViewById(R.id.ivLanguageIcon);
            tvLanguageName = itemView.findViewById(R.id.tvLanguageName);
            tvLanguageSubName = itemView.findViewById(R.id.tvLanguageSubName);
            ivLanguageSelect = itemView.findViewById(R.id.ivLanguageSelect);
            ivDividerLine = itemView.findViewById(R.id.ivDividerLine);
        }
    }
}