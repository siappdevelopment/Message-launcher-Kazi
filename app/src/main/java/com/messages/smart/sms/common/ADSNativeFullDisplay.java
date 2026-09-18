package com.messages.smart.sms.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

public class ADSNativeFullDisplay {
    public static NativeAd AdmobNativeAd;

    public interface PreloadCallback {
        void onAdLoaded();

        void onAdFailed();
    }

    public static void preloadNativeAd(final Context context, String adsId, PreloadCallback callback) {
        if (adsId == null || adsId.isEmpty()) {
            if (callback != null) callback.onAdFailed();
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(context, adsId).forNativeAd(nativeAd -> {
            if (AdmobNativeAd != null) {
                AdmobNativeAd.destroy();
            }
            AdmobNativeAd = nativeAd;
            AdmobNativeAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));
            Utils.trackScreen(context, "FULL_NATIVE_LOAD");
            if (callback != null) callback.onAdLoaded();
        }).withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Utils.trackScreen(context, "FULL_NATIVE_FAILED");
                if (callback != null) callback.onAdFailed();
                super.onAdFailedToLoad(adError);
            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }
}