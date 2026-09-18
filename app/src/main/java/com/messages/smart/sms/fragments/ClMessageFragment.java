package com.messages.smart.sms.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import com.messages.smart.sms.R;
import com.messages.smart.sms.common.Utils;

public class ClMessageFragment extends Fragment {
    private LinearLayout llMessage1, llMessage2, llMessage3, llMessageCustom;
    private AppCompatImageView ivSelectMessage1, ivSendMessage1, ivSelectMessage2, ivSendMessage2, ivSelectMessage3, ivSendMessage3, ivSendCustomMessage;
    private AppCompatEditText etCustomMessage;

    private int selectedPosition = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clmessage, container, false);

        findIDs(view);

        return view;
    }

    private void findIDs(View view) {
        llMessage1 = view.findViewById(R.id.llMessage1);
        ivSelectMessage1 = view.findViewById(R.id.ivSelectMessage1);
        ivSendMessage1 = view.findViewById(R.id.ivSendMessage1);
        llMessage2 = view.findViewById(R.id.llMessage2);
        ivSelectMessage2 = view.findViewById(R.id.ivSelectMessage2);
        ivSendMessage2 = view.findViewById(R.id.ivSendMessage2);
        llMessage3 = view.findViewById(R.id.llMessage3);
        ivSelectMessage3 = view.findViewById(R.id.ivSelectMessage3);
        ivSendMessage3 = view.findViewById(R.id.ivSendMessage3);
        llMessageCustom = view.findViewById(R.id.llMessageCustom);
        etCustomMessage = view.findViewById(R.id.etCustomMessage);
        ivSendCustomMessage = view.findViewById(R.id.ivSendCustomMessage);

        initialEvents();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialEvents() {
        selectMessage(1);

        llMessage1.setOnClickListener(v -> selectMessage(1));

        llMessage2.setOnClickListener(v -> selectMessage(2));

        llMessage3.setOnClickListener(v -> selectMessage(3));

        llMessageCustom.setOnClickListener(v -> selectMessage(4));

        etCustomMessage.setOnTouchListener((v, event) -> {
            selectMessage(4);
            return false;
        });

        ivSendMessage1.setOnClickListener(v -> sendMessage("Sorry, I can't talk right now."));

        ivSendMessage2.setOnClickListener(v -> sendMessage("Can I call you back later?"));

        ivSendMessage3.setOnClickListener(v -> sendMessage("In a meeting right now."));

        ivSendCustomMessage.setOnClickListener(v -> {
            String message = etCustomMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please Enter Message", Toast.LENGTH_SHORT).show();
                return;
            }

            sendMessage(message);
        });
    }

    private void selectMessage(int position) {
        boolean stayingOnCustom = selectedPosition == 4 && position == 4;

        if (position != 4) {
            clearEditTextFocus();
        }

        ivSelectMessage1.setImageResource(R.drawable.ic_clmessage_unselect);
        ivSelectMessage2.setImageResource(R.drawable.ic_clmessage_unselect);
        ivSelectMessage3.setImageResource(R.drawable.ic_clmessage_unselect);

        ivSendMessage1.setVisibility(View.GONE);
        ivSendMessage2.setVisibility(View.GONE);
        ivSendMessage3.setVisibility(View.GONE);
        ivSendCustomMessage.setVisibility(View.GONE);

        selectedPosition = position;

        switch (position) {
            case 1:
                ivSelectMessage1.setImageResource(R.drawable.ic_clmessage_select);
                ivSendMessage1.setVisibility(View.VISIBLE);
                break;
            case 2:
                ivSelectMessage2.setImageResource(R.drawable.ic_clmessage_select);
                ivSendMessage2.setVisibility(View.VISIBLE);
                break;
            case 3:
                ivSelectMessage3.setImageResource(R.drawable.ic_clmessage_select);
                ivSendMessage3.setVisibility(View.VISIBLE);
                break;
            case 4:
                ivSendCustomMessage.setVisibility(View.VISIBLE);
                if (!stayingOnCustom || !etCustomMessage.hasFocus()) {
                    focusEditText();
                }
                break;
        }
    }

    private void clearEditTextFocus() {
        etCustomMessage.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etCustomMessage.getWindowToken(), 0);
        }
    }

    private void focusEditText() {
        if (etCustomMessage == null || getContext() == null) {
            return;
        }

        etCustomMessage.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(etCustomMessage, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void sendMessage(String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("smsto:"));
            intent.putExtra("sms_body", message);
            startActivity(intent);
            Utils.clearActivityTransition(getActivity());

            if (getActivity() != null) {
                getActivity().finish();
                Utils.clearActivityTransition(getActivity());
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "No SMS App Found", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboardIfNeeded() {
        if (etCustomMessage == null || getContext() == null) {
            return;
        }
        View view = getView();
        View tokenView = etCustomMessage;
        if (view != null && view.getWindowToken() != null) {
            tokenView = view;
        }
        etCustomMessage.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && tokenView.getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboardIfNeeded();
    }
}