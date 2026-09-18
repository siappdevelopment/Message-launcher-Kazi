package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.FirebaseApp;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.ScreenFlowNavigator;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.common.VPNHelper;
import com.messages.smart.sms.helpers.RemoteConfigHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private Dialog vpnDetectDialog;
    private boolean splashFlowStarted;
    private static final int PERMISSION_REQUEST_CODE = 101;

    private LinearLayout llContainAds;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    private LinearLayout llBannerAd;
    private FrameLayout flNativeAd;
    private boolean hasNavigatedNext;
    private boolean splashAdvanceScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        if (VPNHelper.isVPNActive(this)) {
            showVpnDetectDialog();
            return;
        }

        startSplashFlow();
    }

    private void showVpnDetectDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (vpnDetectDialog != null && vpnDetectDialog.isShowing()) {
            return;
        }

        vpnDetectDialog = new Dialog(this);
        vpnDetectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        vpnDetectDialog.setContentView(R.layout.dialog_vpn_detect);
        vpnDetectDialog.setCancelable(false);
        if (vpnDetectDialog.getWindow() != null) {
            vpnDetectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            vpnDetectDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        AppCompatTextView btnReCheck = vpnDetectDialog.findViewById(R.id.btnReCheck);
        if (btnReCheck != null) {
            btnReCheck.setOnClickListener(v -> handleVpnRecheck());
        }

        applyDialogSidePadding(vpnDetectDialog);
        vpnDetectDialog.show();
    }

    private void handleVpnRecheck() {
        if (VPNHelper.isVPNActive(this)) {
            return;
        }
        if (vpnDetectDialog != null && vpnDetectDialog.isShowing()) {
            vpnDetectDialog.dismiss();
        }
        startSplashFlow();
    }

    private void applyDialogSidePadding(Dialog dialog) {
        if (dialog.getWindow() == null) {
            return;
        }
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, getResources().getDisplayMetrics());
        dialog.getWindow().getDecorView().setPadding(padding, 0, padding, 0);
    }

    @Override
    protected void onDestroy() {
        if (vpnDetectDialog != null && vpnDetectDialog.isShowing()) {
            vpnDetectDialog.dismiss();
        }
        super.onDestroy();
    }

    private void startSplashFlow() {
        if (splashFlowStarted || isFinishing() || isDestroyed()) {
            return;
        }
        splashFlowStarted = true;

        FirebaseApp.initializeApp(this);

        try {
            it();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AdPlacement.gatherConsent(this, error -> RemoteConfigHelper.fetchRemoteConfig(this, success -> initViews()));
    }

    private void it() {
        Uri referrer = getReferrer();
        if (referrer != null && getPreferences(MODE_PRIVATE).getBoolean(referrer.toString(), false)) {
            return;
        }

        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(this).build();
        backgroundTaskExecutor.execute(() -> getInstallReferrerFromClient(referrerClient));
    }

    private final ExecutorService backgroundTaskExecutor = Executors.newSingleThreadExecutor();

    public void getInstallReferrerFromClient(InstallReferrerClient referrerClient) {
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        ReferrerDetails response = referrerClient.getInstallReferrer();
                        String referrerUrl = response.getInstallReferrer();

                        AdPlacement.setReferrerUrl(SplashActivity.this, referrerUrl);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    referrerClient.endConnection();
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {

            }
        });
    }

    private void initViews() {
        llContainAds = findViewById(R.id.llContainAds);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        clickEvents();
    }

    private void clickEvents() {
        showSplashAd();
        saveInstallDate();
        checkPermissions();
    }

    private void showSplashAd() {
        if (!AdPlacement.getSplashAdShow()) {
            if (rlAdView != null) {
                rlAdView.setVisibility(View.GONE);
            }
            return;
        }

        if (rlAdView != null) {
            rlAdView.setVisibility(View.VISIBLE);
        }

        if ("banner".equalsIgnoreCase(AdPlacement.getSplashAdType())) {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(View.VISIBLE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.GONE);
            }
            AdPlacement.loadBannerAd(this, AdPlacement.getSplashBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
        } else {
            if (rlBannerAdView != null) {
                rlBannerAdView.setVisibility(View.GONE);
            }
            if (rlNativeAdView != null) {
                rlNativeAdView.setVisibility(View.VISIBLE);
            }
            AdPlacement.loadNativeAd(this, AdPlacement.getSplashNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
        }
    }

    private void saveInstallDate() {
        SharedPreferences sharedPreferences = getSharedPreferences("appInstallDate", MODE_PRIVATE);
        if (!sharedPreferences.contains("install_date")) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            sharedPreferences.edit().putString("install_date", today).apply();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            return;
        }

        goNextScreen();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.trackScreenOnce(this, "POST_NOTIFICATIONS_GRANTED");
            }

            goNextScreen();
        }
    }

    private void goNextScreen() {
        if (splashAdvanceScheduled || hasNavigatedNext) {
            return;
        }
        splashAdvanceScheduled = true;
        long delay = AdPlacement.getSplashDuration() * 1000L;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed() || hasNavigatedNext) {
                return;
            }
            showAfterSplashAd();
        }, delay);
    }

    private void showAfterSplashAd() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (llContainAds == null) {
            navigateNextScreen();
            return;
        }

        if (!AdPlacement.getAfterSplashAdShow()) {
            llContainAds.setVisibility(GONE);
            navigateNextScreen();
            return;
        }

        llContainAds.setVisibility(VISIBLE);

        if ("inter".equalsIgnoreCase(AdPlacement.getAfterSplashAdType())) {
            AdPlacement.loadAfterSplashInterstitialAd(this, AdPlacement.getAfterSplashInterstitialId(), this::navigateNextScreen);
        } else {
            AdPlacement.loadAfterSplashAppOpenAd(this, AdPlacement.getAppOpenId(), this::navigateNextScreen);
        }
    }

    private void navigateNextScreen() {
        if (isFinishing() || isDestroyed() || hasNavigatedNext) {
            return;
        }
        hasNavigatedNext = true;
        Utils.clearWindowFocusSafely(this);
        ScreenFlowNavigator.openNext(this);
    }
}