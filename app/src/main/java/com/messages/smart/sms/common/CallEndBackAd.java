package com.messages.smart.sms.common;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

@SuppressWarnings("all")
public class CallEndBackAd {
    public static int adsClickEvent = 0;
    public static int adsBackClick = 0;
    public static Boolean isAdsShowEnable = false;
    public static int fullScreenAdsPosition = 0;
    public static boolean fullScreenAdsFailed = false;
    public static OnCompeteAds onCompleteAdCallBack;
    public static boolean isAdsEnabled = true;
    private static InterstitialAd fullScreenAds;
    private static AppOpenAd appOpenAd;
    public static boolean isAdShowing = true;

    public static void fullScreenAdShow(Activity context, OnCompeteAds onFinishAd, boolean... doShowAds) {
        onCompleteAdCallBack = onFinishAd;
        if (AdPlacement.getClEndBackAdType().equalsIgnoreCase("appopen")) {
            admobAppOpenAd(context);
        } else {
            admobFullScreenAd(context);
        }
    }

    public static void admobAppOpenAd(Activity context) {
        if (appOpenAd != null) {
            adsShowCheckEvent(false);
            try {
                appOpenAd.show(context);
                appOpenAd = null;
            } catch (Exception e) {
                appOpenAd = null;
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                }
            }
        } else {
            adsShowCheckEvent(true);
            if (onCompleteAdCallBack != null) {
                onCompleteAdCallBack.onCompeteAds(false);
                onCompleteAdCallBack = null;
            }
        }

        if (isAdsEnabled) {
            isAdsEnabled = false;
            admobAppOpenAdLoad(context);
        }
    }

    public static void admobFullScreenAd(Activity context) {
        if (fullScreenAds != null) {
            adsShowCheckEvent(false);
            try {
                fullScreenAds.show(context);
                fullScreenAds = null;
            } catch (Exception e) {
                fullScreenAds = null;
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                }
            }
        } else {
            adsShowCheckEvent(true);
            if (onCompleteAdCallBack != null) {
                onCompleteAdCallBack.onCompeteAds(false);
                onCompleteAdCallBack = null;
            }
        }
    }

    public static void admobAppOpenAdLoad(Activity context) {
        if (!AdPlacement.canRequestAds(context)) {
            isAdsEnabled = true;
            return;
        }
        if (appOpenAd != null) {
            isAdsEnabled = true;
            return;
        }

        String adUnitId = AdPlacement.getAppOpenId();
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            return;
        }

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(context, adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));

                isAdsEnabled = true;
                appOpenAd = ad;
                Utils.trackScreen(context, "CL_END_APP_OPEN_LOAD");
                onAppOpenListner(context);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Utils.trackScreen(context, "CL_END_APP_OPEN_FAILED");
                isAdsEnabled = true;
            }
        });
    }

    private static void onAppOpenListner(Activity context) {
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(true);
                    onCompleteAdCallBack = null;
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                    onCompleteAdCallBack = null;
                }
            }
        });
    }

    public static void adsShowCheckEvent(Boolean check) {
        if (check) {
            isAdShowing = false;
            isAdsShowEnable = true;
        } else {
            isAdShowing = true;
            isAdsShowEnable = false;
        }
    }

    public static void loadAd(Activity context) {
        if (AdPlacement.getClEndBackAdType().equalsIgnoreCase("appopen")) {
            admobAppOpenAdLoad(context);
        } else {
            admobFullScreenAdLoad(context);
        }
    }

    private static void admobFullScreenAdLoad(Activity context) {
        if (!AdPlacement.canRequestAds(context)) {
            isAdsEnabled = true;
            return;
        }
        if (fullScreenAds != null) {
            isAdsEnabled = true;
            return;
        }

        String adUnitId = AdPlacement.getClEndBackAdInterstitialId();
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            return;
        }

        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialAd.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(context, adValue));

                isAdsEnabled = true;
                CallEndBackAd.fullScreenAds = interstitialAd;
                Utils.trackScreen(context, "CL_END_INTER_LOAD");
                onContactListner(context);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Utils.trackScreen(context, "CL_END_INTER_FAILED");
                isAdsEnabled = true;
            }
        });
    }

    public static void onContactListner(Context context) {
        fullScreenAds.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(true);
                    onCompleteAdCallBack = null;
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adsShowCheckEvent(true);
                if (onCompleteAdCallBack != null) {
                    onCompleteAdCallBack.onCompeteAds(false);
                    onCompleteAdCallBack = null;
                }
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    public interface OnCompeteAds {
        void onCompeteAds(boolean b);
    }
}