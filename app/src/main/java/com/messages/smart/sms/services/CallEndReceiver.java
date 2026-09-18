package com.messages.smart.sms.services;

import android.telecom.Call;
import android.telecom.CallScreeningService;

import androidx.annotation.NonNull;

public class CallEndReceiver extends CallScreeningService {
    @Override
    public void onScreenCall(@NonNull Call.Details details) {
        respondToCall(details, new CallResponse.Builder().build());
    }
}