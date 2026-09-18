package com.messages.smart.sms.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.widget.AppCompatTextView;

import com.messages.smart.sms.R;

import java.util.List;

public class SerializeDropdownAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> arraySerialize;

    public SerializeDropdownAdapter(Context context, List<String> arraySerialize) {
        this.context = context;
        this.arraySerialize = arraySerialize;
    }

    @Override
    public int getCount() {
        return arraySerialize.size();
    }

    @Override
    public Object getItem(int i) {
        return arraySerialize.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_serialize_dropdown, parent, false);
        AppCompatTextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(arraySerialize.get(position));
        return view;
    }
}