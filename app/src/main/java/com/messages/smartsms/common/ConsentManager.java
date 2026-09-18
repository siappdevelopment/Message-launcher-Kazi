package com.messages.smartsms.common;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConsentManager {
    private static volatile ConsentManager instance;
//    private static final String TEST_DEVICE_HASHED_ID = "";

    private final Context appContext;
    private ConsentInformation consentInformation;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

    public interface OnConsentGatheringCompleteListener {
        void onConsentGatheringComplete(@Nullable FormError error);
    }

    private ConsentManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.consentInformation = UserMessagingPlatform.getConsentInformation(appContext);
    }

    public static ConsentManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ConsentManager.class) {
                if (instance == null) {
                    instance = new ConsentManager(context);
                }
            }
        }
        return instance;
    }

    public void gatherConsent(Activity activity, OnConsentGatheringCompleteListener onComplete) {
        if (activity == null || activity.isFinishing()) {
            if (onComplete != null) {
                onComplete.onConsentGatheringComplete(null);
            }
            return;
        }

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
//        if (isDebuggable()) {
//            ConsentDebugSettings.Builder debugBuilder = new ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);
//            if (TEST_DEVICE_HASHED_ID != null && !TEST_DEVICE_HASHED_ID.trim().isEmpty()) {
//                debugBuilder.addTestDeviceHashedId(TEST_DEVICE_HASHED_ID.trim());
//            }
//            paramsBuilder.setConsentDebugSettings(debugBuilder.build());
//        }

        ConsentRequestParameters params = paramsBuilder.build();

        consentInformation.requestConsentInfoUpdate(activity, params, () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
            if (canRequestAds()) {
                initializeMobileAdsSdk();
            }
            if (onComplete != null) {
                onComplete.onConsentGatheringComplete(formError);
            }
        }), requestConsentError -> {
            if (canRequestAds()) {
                initializeMobileAdsSdk();
            }
            if (onComplete != null) {
                onComplete.onConsentGatheringComplete(requestConsentError);
            }
        });

        if (canRequestAds()) {
            initializeMobileAdsSdk();
        }
    }

    public boolean canRequestAds() {
        if (consentInformation == null) {
            consentInformation = UserMessagingPlatform.getConsentInformation(appContext);
        }
        return consentInformation.canRequestAds();
    }

    public boolean isPrivacyOptionsRequired() {
        if (consentInformation == null) {
            consentInformation = UserMessagingPlatform.getConsentInformation(appContext);
        }
        return consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showPrivacyOptionsForm(Activity activity, ConsentForm.OnConsentFormDismissedListener onDismissed) {
        if (activity == null || activity.isFinishing()) {
            if (onDismissed != null) {
                onDismissed.onConsentFormDismissed(null);
            }
            return;
        }
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onDismissed);
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        MobileAds.initialize(appContext, initializationStatus -> {
        });
    }

    private boolean isDebuggable() {
        return (appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}