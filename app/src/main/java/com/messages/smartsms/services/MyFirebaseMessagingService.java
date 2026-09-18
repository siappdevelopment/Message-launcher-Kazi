package com.messages.smartsms.services;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.messages.smartsms.MyApplication;
import com.messages.smartsms.R;
import com.messages.smartsms.common.ADSNativeFullDisplay;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.CallEndLaunchHelper;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        Log.d("MyFCMService", "onMessageReceived: from " + remoteMessage.getFrom());
    }

    @Override
    public void handleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

//        Log.d("MyFCMService", "handleIntent: received intent");
        Map<String, String> data = null;
        RemoteMessage remoteMessage = null;
        if (intent != null && intent.getExtras() != null) {
            remoteMessage = new RemoteMessage(intent.getExtras());
            data = remoteMessage.getData();
        }

        if (remoteMessage != null) {
//            Log.d("MyFCMService", "handleIntent: RemoteMessage found");
            if (MyApplication.isAppInForeground()) {
//                Log.d("MyFCMService", "handleIntent: App is in foreground, returning");
                return;
            }

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null && !keyguardManager.isKeyguardLocked()) {
//                Log.d("MyFCMService", "handleIntent: Device is unlocked, returning");
                return;
            }

            if (!AdPlacement.isCallEndPerformanceAllowed(this, true)) {
//                Log.d("MyFCMService", "handleIntent: CallEnd performance NOT allowed, returning");
                return;
            }

            String nativeId = AdPlacement.getClEndNativeId();
//            Log.d("MyFCMService", "handleIntent: Preloading native ad with ID: " + nativeId);
            final Map<String, String> finalData = data;
            final RemoteMessage finalRemoteMessage = remoteMessage;

            ADSNativeFullDisplay.preloadNativeAd(this, nativeId, new ADSNativeFullDisplay.PreloadCallback() {
                @Override
                public void onAdLoaded() {
//                    Log.d("MyFCMService", "preloadNativeAd: onAdLoaded");
                    launchCallEnd(finalData, finalRemoteMessage);
                }

                @Override
                public void onAdFailed() {
//                    Log.d("MyFCMService", "preloadNativeAd: onAdFailed");
                }
            });
            return;
        }

//        Log.d("MyFCMService", "handleIntent: RemoteMessage is null, calling super");
        super.handleIntent(intent);
    }

    private void launchCallEnd(Map<String, String> data, RemoteMessage remoteMessage) {
//        Log.d("MyFCMService", "launchCallEnd: triggered");
        if (CallEndLaunchHelper.tryOpenFromFcmData(this, data)) {
//            Log.d("MyFCMService", "launchCallEnd: Opened from FCM data");
            return;
        }

        if (remoteMessage.getNotification() != null) {
//            Log.d("MyFCMService", "launchCallEnd: Opening from Notification data");
            CallEndLaunchHelper.openGenericForFirebaseNotification(this, remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
            return;
        }

//        Log.d("MyFCMService", "launchCallEnd: Opening generic notification");
        CallEndLaunchHelper.openGenericForFirebaseNotification(this, getString(R.string.app_name), "");
    }

    @Override
    public void onNewToken(@NonNull String token) {
//        Log.d("MyFCMService", "onNewToken: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
//        Log.d("MyFCMService", "sendRegistrationToServer: " + token);
    }
}