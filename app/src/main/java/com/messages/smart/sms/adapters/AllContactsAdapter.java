package com.messages.smart.sms.adapters;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.messages.smart.sms.R;
import com.messages.smart.sms.activities.MessagesContentActivity;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.models.ContactsModel;

import java.util.ArrayList;

public class AllContactsAdapter extends RecyclerView.Adapter<AllContactsAdapter.ViewHolder> {
    private final Context context;
    private ArrayList<ContactsModel> arrayListContactsModel;
    private final AppCompatEditText etSearch;

    private int previousExpanded = -1;
    private String name;

    public AllContactsAdapter(Context context, ArrayList<ContactsModel> arrayListContactsModel, AppCompatEditText etSearch) {
        this.context = context;
        this.arrayListContactsModel = arrayListContactsModel;
        this.etSearch = etSearch;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_all_contacts, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (arrayListContactsModel.get(position).getPhoto() != null) {
            holder.ivImage.setVisibility(VISIBLE);
            holder.tvImage.setVisibility(GONE);
            Glide.with(context).load(arrayListContactsModel.get(position).getPhoto()).placeholder(R.drawable.ic_user).into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(GONE);
            holder.tvImage.setVisibility(VISIBLE);
            holder.tvImage.setText(arrayListContactsModel.get(position).getName().substring(0, 1));
        }

        holder.tvName.setText(arrayListContactsModel.get(position).getName());

        holder.llExpandView.setVisibility(arrayListContactsModel.get(position).isExpanded() ? View.VISIBLE : GONE);

        holder.tvNumber.setText(arrayListContactsModel.get(position).getNumber());

        if (position == arrayListContactsModel.size() - 1) {
            holder.ivDividerLine.setVisibility(INVISIBLE);
        } else {
            holder.ivDividerLine.setVisibility(VISIBLE);
        }

        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= arrayListContactsModel.size()) {
                return;
            }

            if (previousExpanded == adapterPosition) {
                arrayListContactsModel.get(adapterPosition).setExpanded(false);
                previousExpanded = -1;
            } else {
                if (previousExpanded != -1 && previousExpanded < arrayListContactsModel.size()) {
                    arrayListContactsModel.get(previousExpanded).setExpanded(false);
                    notifyItemChanged(previousExpanded);
                }
                arrayListContactsModel.get(adapterPosition).setExpanded(true);
                previousExpanded = adapterPosition;
            }
            notifyItemChanged(adapterPosition);
        });

        holder.ivCall.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= arrayListContactsModel.size()) {
                return;
            }

            previousExpanded = -1;
            arrayListContactsModel.get(adapterPosition).setExpanded(false);
            notifyItemChanged(adapterPosition);

            String callNumber = get10DigitNumber(arrayListContactsModel.get(adapterPosition).getNumber());
            if (callNumber.isEmpty()) {
                callNumber = arrayListContactsModel.get(adapterPosition).getNumber();
            }
            Utils.openDialer(context, callNumber);

            etSearch.setText("");
            etSearch.clearFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        });

        holder.ivMessage.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= arrayListContactsModel.size()) {
                return;
            }

            previousExpanded = -1;
            arrayListContactsModel.get(adapterPosition).setExpanded(false);
            notifyItemChanged(adapterPosition);

            if (arrayListContactsModel.get(adapterPosition).getName() != null && !arrayListContactsModel.get(adapterPosition).getName().isEmpty()) {
                name = arrayListContactsModel.get(adapterPosition).getName();
            } else {
                name = arrayListContactsModel.get(adapterPosition).getNumber();
            }

            Intent intent = new Intent(context, MessagesContentActivity.class);
            String contactNumber = arrayListContactsModel.get(adapterPosition).getNumber();
            intent.putExtra(MessagesContentActivity.EXTRA_NAME, name);
            intent.putExtra(MessagesContentActivity.EXTRA_ADDRESS, contactNumber);
            String threadId = resolveThreadId(contactNumber);
            if (threadId != null && !threadId.isEmpty()) {
                intent.putExtra(MessagesContentActivity.EXTRA_THREAD_ID, threadId);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Utils.clearActivityTransition(context);

            etSearch.setText("");
            etSearch.clearFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        });
    }

    @Override
    public int getItemCount() {
        return arrayListContactsModel.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivImage, ivCall, ivMessage, ivDividerLine;
        private final AppCompatTextView tvImage, tvName, tvNumber;
        private final LinearLayout llExpandView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvImage = itemView.findViewById(R.id.tvImage);
            tvName = itemView.findViewById(R.id.tvName);
            llExpandView = itemView.findViewById(R.id.llExpandView);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            ivCall = itemView.findViewById(R.id.ivCall);
            ivMessage = itemView.findViewById(R.id.ivMessage);
            ivDividerLine = itemView.findViewById(R.id.ivDividerLine);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(ArrayList<ContactsModel> searchList) {
        previousExpanded = -1;
        arrayListContactsModel = new ArrayList<>();
        for (ContactsModel contactsModel : searchList) {
            contactsModel.setExpanded(false);
            arrayListContactsModel.add(contactsModel);
        }
        notifyDataSetChanged();
    }

    private String resolveThreadId(String contactNumber) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            return "";
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        try {
            return String.valueOf(Telephony.Threads.getOrCreateThreadId(context, contactNumber));
        } catch (SecurityException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return "";
        }
    }

    private String get10DigitNumber(String input) {
        if (input == null) {
            return "";
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        if (digits.length() == 10) {
            return digits;
        }
        return "";
    }
}