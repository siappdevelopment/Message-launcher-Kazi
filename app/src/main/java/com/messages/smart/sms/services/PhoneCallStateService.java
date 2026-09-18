package com.messages.smart.sms.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.CallEndLaunchHelper;

import java.util.Date;

public class PhoneCallStateService extends BroadcastReceiver {
    public static boolean isIncomingCallSend = false;

    private static boolean isIncomingCallMain = false;
    private static boolean isMissCalls = false;
    static boolean isOutgoingCall = false;
    private static boolean isRingingCall = false;
    private static boolean isShowScreenCall = false;
    private static String outgoingNumber;
    private static Date callStartTime = new Date();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            return;
        }

        if (!"android.intent.action.PHONE_STATE".equals(action)) {
            return;
        }

        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state == null) {
                return;
            }

            if (outgoingNumber == null || outgoingNumber.isEmpty()) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (incomingNumber == null || incomingNumber.isEmpty()) {
                    incomingNumber = intent.getStringExtra("incoming_number");
                }
                if (incomingNumber != null && !incomingNumber.isEmpty()) {
                    outgoingNumber = incomingNumber;
                }
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                if (!isIncomingCallMain && isRingingCall) {
                    isMissCalls = true;
                    isIncomingCallMain = false;
                    isOutgoingCall = false;
                }
                String callType = isIncomingCallMain ? "Incoming" : isMissCalls ? "Missed Call" : "Outgoing";
                if (!isShowScreenCall) {
                    openNewActivity(context, outgoingNumber, callStartTime, new Date(), callType);
                    isShowScreenCall = true;
                    return;
                }
                outgoingNumber = null;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                callStartTime = new Date();
                if (!isRingingCall) {
                    isOutgoingCall = true;
                } else {
                    isIncomingCallSend = true;
                    isIncomingCallMain = true;
                }
                isShowScreenCall = false;
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                callStartTime = new Date();
                isRingingCall = true;
                isShowScreenCall = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openNewActivity(Context context, String number, Date start, Date end, String callType) {
        if (!AdPlacement.getClEndScreenShow()) {
            resetCallFlags();
            return;
        }

        CallEndLaunchHelper.openAfterCallEnded(context, number, start, end, callType);
        resetCallFlags();
    }

    private static void resetCallFlags() {
        outgoingNumber = null;
        isMissCalls = false;
        isIncomingCallMain = false;
        isOutgoingCall = false;
    }
}