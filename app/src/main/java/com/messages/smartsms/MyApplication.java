package com.messages.smartsms;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.messages.smartsms.activities.ClEndActivity;
import com.messages.smartsms.activities.LauncherHomeActivity;
import com.messages.smartsms.activities.MessagesPopActivity;
import com.messages.smartsms.activities.SplashActivity;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.Utils;

import java.util.Date;
import java.util.Objects;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private AppOpenAd appOpenAd = null;
    private boolean isLoadingAd = false;
    private Activity currentActivity;
    private long loadTime = 0;
    public static int interstitialCount;
    public static int rightSwipeInterstitialCount;
    public static int interstitialSwitchCount;
    public static int interstitialMessageListCount;
    public static int interstitialMessageContentBackCount;

    private static boolean isAppInForeground = false;

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Utils.wrapContext(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.applyStoredLocale(this);
        AdPlacement.ensureClEndConfig(this);

        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    private boolean isUnderAdShowLimit() {
        return AdPlacement.canShowAppOpenAd(this);
    }

    private void incrementAdShowCount() {
        AdPlacement.setAppOpenLastShowTime(this, System.currentTimeMillis());
    }

    public void fetchAd() {
        if (!AdPlacement.canRequestAds(this)) {
            return;
        }

        if (!AdPlacement.getAppOpenAdShow() || !isUnderAdShowLimit()) {
            appOpenAd = null;
            return;
        }

        if (isAdAvailable()) {
            return;
        }

        if (isLoadingAd) {
            return;
        }

        String adUnitId = AdPlacement.getAppOpenId();
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            return;
        }

        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(this, adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(getApplicationContext(), adValue));

                MyApplication.this.appOpenAd = ad;
                MyApplication.this.isLoadingAd = false;
                MyApplication.this.loadTime = (new Date()).getTime();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                MyApplication.this.isLoadingAd = false;
            }
        });
    }

    private boolean wasLoadTimeLessThanNHoursAgo() {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * (long) 4));
    }

    public boolean isAdAvailable() {
        return AdPlacement.getAppOpenAdShow() && isUnderAdShowLimit() && appOpenAd != null && wasLoadTimeLessThanNHoursAgo();
    }

    private boolean shouldShowAppOpenAd(@NonNull Activity activity) {
        return !(activity instanceof SplashActivity) && !(activity instanceof LauncherHomeActivity) && !(activity instanceof ClEndActivity) && !(activity instanceof MessagesPopActivity);
    }

    public void showAdIfAvailable(@NonNull final Activity activity, @NonNull final OnShowAdCompleteListener onShowAdCompleteListener) {
        if (!shouldShowAppOpenAd(activity)) {
            onShowAdCompleteListener.onShowAdComplete();
            return;
        }

        if (!AdPlacement.canRequestAds(this)) {
            onShowAdCompleteListener.onShowAdComplete();
            return;
        }

        if (!AdPlacement.getAppOpenAdShow() || !isUnderAdShowLimit()) {
            appOpenAd = null;
            onShowAdCompleteListener.onShowAdComplete();
            return;
        }

        if (isAdAvailable()) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    MyApplication.this.appOpenAd = null;
                    MyApplication.this.fetchAd();
                    onShowAdCompleteListener.onShowAdComplete();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    MyApplication.this.appOpenAd = null;
                    MyApplication.this.fetchAd();
                    AdPlacement.completeGoogleAppOpenOrQuizFallback(activity, onShowAdCompleteListener::onShowAdComplete);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    MyApplication.this.incrementAdShowCount();
                }
            });
            try {
                appOpenAd.show(activity);
            } catch (Exception e) {
                appOpenAd = null;
                fetchAd();
                AdPlacement.completeGoogleAppOpenOrQuizFallback(activity, onShowAdCompleteListener::onShowAdComplete);
            }
        } else {
            if (AdPlacement.getGoogleAdFailedShowQuiz()) {
                AdPlacement.loadAfterSplashAppOpenAd(activity, AdPlacement.getAppOpenId(), onShowAdCompleteListener::onShowAdComplete);
            } else {
                fetchAd();
                onShowAdCompleteListener.onShowAdComplete();
            }
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        if (currentActivity != null && shouldShowAppOpenAd(currentActivity)) {
            if (AdPlacement.getAppOpenDialogShow()) {
                showAppOpenDialog(currentActivity, () -> showAdIfAvailable(currentActivity, () -> {
                }));
            } else {
                showAdIfAvailable(currentActivity, () -> {
                });
            }
        }
    }

    private void showAppOpenDialog(Activity activity, Runnable onDismiss) {
        if (!isAdAvailable()) {
            onDismiss.run();
            return;
        }

        if (activity == null || activity.isFinishing()) {
            onDismiss.run();
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(0));
        dialog.setContentView(R.layout.dialog_loading_ads);
        dialog.setCancelable(false);
        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing() && !activity.isFinishing() && !activity.isDestroyed()) {
                Utils.clearWindowFocusSafely(dialog.getWindow());
                dialog.dismiss();
            }
            onDismiss.run();
        }, 1500);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        isAppInForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isAppInForeground = false;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!isLoadingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Utils.clearWindowFocusSafely(activity);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}