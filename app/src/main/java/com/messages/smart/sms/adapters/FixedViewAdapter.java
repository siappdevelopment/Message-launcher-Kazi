package com.messages.smart.sms.adapters;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FixedViewAdapter extends RecyclerView.Adapter<FixedViewAdapter.Holder> {
    private final View view;
    private boolean included = true;

    public FixedViewAdapter(@NonNull View view) {
        this.view = view;
        setHasStableIds(true);
    }

    public FixedViewAdapter(@NonNull View view, boolean included) {
        this.view = view;
        this.included = included;
        setHasStableIds(true);
    }

    public boolean isIncluded() {
        return included;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setIncluded(boolean included) {
        if (this.included == included) {
            return;
        }
        this.included = included;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return included ? 1 : 0;
    }

    @Override
    public long getItemId(int position) {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}