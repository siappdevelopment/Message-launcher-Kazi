package com.messages.smart.sms.adapters;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messages.smart.sms.R;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.interfaces.OnMessageOpenListener;
import com.messages.smart.sms.interfaces.OnUnblockClickListener;
import com.messages.smart.sms.models.MessagesModel;

import java.io.InputStream;
import java.util.ArrayList;

public class BlockMessagesAdapter extends RecyclerView.Adapter<BlockMessagesAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<MessagesModel> arrayListMessagesModel;
    private final OnUnblockClickListener onUnblockClickListener;
    private OnMessageOpenListener onMessageOpenListener;

    public BlockMessagesAdapter(Context context, ArrayList<MessagesModel> arrayListMessagesModel, OnUnblockClickListener onUnblockClickListener) {
        this.context = context;
        this.arrayListMessagesModel = arrayListMessagesModel;
        this.onUnblockClickListener = onUnblockClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_block_messages, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.tvUnblock.setText(R.string.unblock);

        Bitmap photo = getContactPhoto(context, arrayListMessagesModel.get(position).getAddress());
        String name = arrayListMessagesModel.get(position).getName();

        if (photo != null) {
            holder.ivImage.setVisibility(VISIBLE);
            holder.tvName.setVisibility(GONE);
            holder.ivImage.setImageBitmap(photo);
            holder.ivImage.setPadding(0, 0, 0, 0);
        } else if (name != null && !name.isEmpty() && !name.equals(arrayListMessagesModel.get(position).getAddress())) {
            holder.ivImage.setVisibility(GONE);
            holder.tvName.setVisibility(VISIBLE);
            holder.tvName.setText(name.substring(0, 1).toUpperCase());
        } else {
            holder.ivImage.setVisibility(VISIBLE);
            holder.tvName.setVisibility(GONE);
            holder.ivImage.setImageResource(R.drawable.ic_user);
            holder.ivImage.setPadding(26, 26, 26, 26);
        }

        holder.tvTitle.setText(name);

        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || onMessageOpenListener == null) {
                return;
            }
            onMessageOpenListener.onMessageOpen(arrayListMessagesModel.get(adapterPosition));
        });

        holder.rlUnblock.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            onUnblockClickListener.onUnblockClick(arrayListMessagesModel, adapterPosition);
        });

        if (position == arrayListMessagesModel.size() - 1) {
            holder.ivDividerLine.setVisibility(INVISIBLE);
        } else {
            holder.ivDividerLine.setVisibility(VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return arrayListMessagesModel.size();
    }

    public void setOpenListener(OnMessageOpenListener onMessageOpenListener) {
        this.onMessageOpenListener = onMessageOpenListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvName, tvTitle, tvUnblock;
        private final AppCompatImageView ivImage, ivDividerLine;
        private final RelativeLayout rlUnblock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvUnblock = itemView.findViewById(R.id.tvUnblock);
            rlUnblock = itemView.findViewById(R.id.rlUnblock);
            ivDividerLine = itemView.findViewById(R.id.ivDividerLine);
        }
    }

    private Bitmap getContactPhoto(Context context, String phoneNumber) {
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_URI}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String photoUri = cursor.getString(0);
                    if (photoUri != null) {
                        InputStream input = context.getContentResolver().openInputStream(Uri.parse(photoUri));
                        cursor.close();
                        return Utils.decodeScaledBitmap(input, 256);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void messageMarkAsRead(Context context, String threadId) {
        try {
            ContentValues values = new ContentValues();
            values.put("read", 1);
            context.getContentResolver().update(Uri.parse("content://sms"), values, "thread_id=? AND read=0", new String[]{threadId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}