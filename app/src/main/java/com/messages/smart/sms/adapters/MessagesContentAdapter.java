package com.messages.smart.sms.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.interfaces.OnMessageContentItemLongClickListener;
import com.messages.smart.sms.models.ChatMessageItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<ChatMessageItem> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
    private final OnMessageContentItemLongClickListener selectionListener;
    private boolean selectionMode;

    public MessagesContentAdapter(OnMessageContentItemLongClickListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessageItem.TYPE_DATE_HEADER) {
            return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_messages_date, parent, false));
        }
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_messages_content, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvDate.setText(item.getDateLabel());
            return;
        }

        MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
        String time = timeFormat.format(new Date(item.getDate())).toUpperCase(Locale.ENGLISH);

        if (item.getItemType() == ChatMessageItem.TYPE_RECEIVED) {
            messageViewHolder.llMessageRec.setVisibility(VISIBLE);
            messageViewHolder.llMessageSend.setVisibility(GONE);
            messageViewHolder.tvMessageRec.setText(item.getBody());
            messageViewHolder.tvMessageRecTime.setText(time);
            updateSelectionUi(messageViewHolder.llMessageRec, item.isSelected());
        } else {
            messageViewHolder.llMessageRec.setVisibility(GONE);
            messageViewHolder.llMessageSend.setVisibility(VISIBLE);
            messageViewHolder.tvMessageSend.setText(item.getBody());
            messageViewHolder.tvMessageSendTime.setText(time);
            updateSelectionUi(messageViewHolder.llMessageSend, item.isSelected());
        }

        holder.itemView.setOnLongClickListener(view -> {
            if (!item.isMessage()) {
                return false;
            }
            if (!selectionMode) {
                selectionMode = true;
                item.setSelected(true);
                notifyItemChanged(position);
                notifySelectionChanged();
            }
            return true;
        });

        holder.itemView.setOnClickListener(view -> {
            if (!selectionMode || !item.isMessage()) {
                return;
            }
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);
            notifySelectionChanged();
        });
    }

    private void updateSelectionUi(LinearLayout bubble, boolean selected) {
        bubble.setAlpha(selected ? 0.55f : 1f);
    }

    private void notifySelectionChanged() {
        if (selectionListener == null) {
            return;
        }
        int selectedCount = getSelectedCount();
        if (selectedCount == 0) {
            selectionMode = false;
            selectionListener.onMessageContentItemLongClickListener(false, 0);
            return;
        }
        selectionListener.onMessageContentItemLongClickListener(true, selectedCount);
    }

    private int getSelectedCount() {
        int count = 0;
        for (ChatMessageItem item : items) {
            if (item.isMessage() && item.isSelected()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<ChatMessageItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        selectionMode = false;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void appendSentMessage(String body, long date, String dateLabel) {
        if (!dateLabel.equals(findLastDateLabel())) {
            items.add(ChatMessageItem.dateHeader(dateLabel));
        }
        items.add(ChatMessageItem.sent(body, date, -1));
        notifyDataSetChanged();
    }

    private String findLastDateLabel() {
        for (int i = items.size() - 1; i >= 0; i--) {
            ChatMessageItem item = items.get(i);
            if (item.isDateHeader()) {
                return item.getDateLabel();
            }
        }
        return null;
    }

    public boolean hasConversationMessages() {
        for (ChatMessageItem item : items) {
            if (item.isMessage()) {
                return true;
            }
        }
        return false;
    }

    public List<Long> getSelectedMessageIds() {
        List<Long> selectedIds = new ArrayList<>();
        for (ChatMessageItem item : items) {
            if (item.isMessage() && item.isSelected() && item.getMessageId() > 0) {
                selectedIds.add(item.getMessageId());
            }
        }
        return selectedIds;
    }

    public List<String> getSelectedMessageBodies() {
        List<String> bodies = new ArrayList<>();
        for (ChatMessageItem item : items) {
            if (item.isMessage() && item.isSelected() && item.getBody() != null && !item.getBody().isEmpty()) {
                bodies.add(item.getBody());
            }
        }
        return bodies;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void deleteSelectedMessages() {
        List<ChatMessageItem> remaining = new ArrayList<>();
        for (ChatMessageItem item : items) {
            if (!(item.isMessage() && item.isSelected())) {
                remaining.add(item);
            }
        }
        items.clear();
        items.addAll(remaining);
        selectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onMessageContentItemLongClickListener(false, 0);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelection() {
        for (ChatMessageItem item : items) {
            item.setSelected(false);
        }
        selectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onMessageContentItemLongClickListener(false, 0);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvDate;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llMessageRec, llMessageSend;
        private final AppCompatTextView tvMessageRec, tvMessageRecTime, tvMessageSend, tvMessageSendTime;

        public MessageViewHolder(View itemView) {
            super(itemView);
            llMessageRec = itemView.findViewById(R.id.llMessageRec);
            llMessageSend = itemView.findViewById(R.id.llMessageSend);
            tvMessageRec = itemView.findViewById(R.id.tvMessageRec);
            tvMessageRecTime = itemView.findViewById(R.id.tvMessageRecTime);
            tvMessageSend = itemView.findViewById(R.id.tvMessageSend);
            tvMessageSendTime = itemView.findViewById(R.id.tvMessageSendTime);
        }
    }
}