package com.messages.smartsms.activities;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.messages.smartsms.R;
import com.messages.smartsms.adapters.ClEndPagerAdapter;
import com.messages.smartsms.common.ADSNativeFullDisplay;
import com.messages.smartsms.common.AdPlacement;
import com.messages.smartsms.common.CallEndBackAd;
import com.messages.smartsms.common.CallEndPendingLaunch;

public class ClEndActivity extends AppCompatActivity {
    public static final String EXTRA_SKIP_OVERLAY_PROMPT = "skip_overlay_prompt";
    public static final String EXTRA_ALLOW_SHOW_ON_LOCKSCREEN = "allow_show_on_lockscreen";
    public static final String EXTRA_LOCKED_CALL_END_NOTIFICATION = "locked_call_end_notification";

    private AppCompatTextView tvTimeType, tvDuration;
    private AppCompatImageView ivCall;
    private TabLayout tlOption;
    private ViewPager2 vpData;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    private LinearLayout llCallView, llBannerAd;
    private FrameLayout flNativeAd, fullAdContainer;
    private RelativeLayout rlFullAd;
    private AppCompatImageView ivClose;

    private String timeType, duration;
    private NativeAd fullScreenNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdPlacement.ensureClEndConfig(this);
        setContentView(R.layout.activity_clend);

        AdPlacement.gatherConsent(this, error -> initViews());
    }

    private void initViews() {
        Intent intent = getIntent();
        timeType = intent.getStringExtra("timeType") != null ? intent.getStringExtra("timeType") : "";
        duration = intent.getStringExtra("duration") != null ? intent.getStringExtra("duration") : "";

        llCallView = findViewById(R.id.llCallView);
        tvTimeType = findViewById(R.id.tvTimeType);
        tvDuration = findViewById(R.id.tvDuration);
        ivCall = findViewById(R.id.ivCall);
        tlOption = findViewById(R.id.tlOption);
        vpData = findViewById(R.id.vpData);
        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);
        rlFullAd = findViewById(R.id.rlFullAd);
        fullAdContainer = findViewById(R.id.fullAdContainer);
        ivClose = findViewById(R.id.ivClose);

        setupFullAdContainer();
        initialEvents();
    }

    private void setupFullAdContainer() {
        CallEndPendingLaunch.cancelCallEndNotification(this);

        Intent intent = getIntent();
        String callType = intent != null ? intent.getStringExtra("CallType") : null;
        if (AdPlacement.isNotificationFullAdFlow(intent, callType) && tryShowNotificationFullAd()) {
            return;
        }
        showCallStateView();
    }

    @SuppressLint("InflateParams")
    private boolean tryShowNotificationFullAd() {
        if (ADSNativeFullDisplay.AdmobNativeAd == null || fullAdContainer == null) {
            return false;
        }

        NativeAd preloadedAd = ADSNativeFullDisplay.AdmobNativeAd;
        ADSNativeFullDisplay.AdmobNativeAd = null;

        NativeAdView adView = (NativeAdView) LayoutInflater.from(this).inflate(R.layout.notification_full_ad, null);
        AdPlacement.populateNativeAdView(preloadedAd, adView, "large");
        fullAdContainer.removeAllViews();
        fullAdContainer.addView(adView);
        fullScreenNativeAd = preloadedAd;

        showNotificationFullAdView();
        return true;
    }

    private void showCallStateView() {
        showAd();
        llCallView.setVisibility(View.VISIBLE);
        rlFullAd.setVisibility(View.GONE);
        fullAdContainer.setVisibility(View.GONE);
    }

    private void showNotificationFullAdView() {
        llCallView.setVisibility(View.GONE);
        rlFullAd.setVisibility(View.VISIBLE);
        fullAdContainer.setVisibility(View.VISIBLE);

        ivClose.setVisibility(AdPlacement.getNotificationCloseButtonShow() ? View.VISIBLE : View.GONE);
        ivClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallEndPendingLaunch.cancelCallEndNotification(this);
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (keyguardManager == null || !keyguardManager.isKeyguardLocked()) {
                CallEndPendingLaunch.clear(this);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        if (fullScreenNativeAd != null) {
            fullScreenNativeAd.destroy();
            fullScreenNativeAd = null;
        }
        if (fullAdContainer != null) {
            fullAdContainer.removeAllViews();
        }
        super.onDestroy();
    }

    private void initialEvents() {
        if (AdPlacement.isNetworkAvailable(this) && AdPlacement.showClEndBackAd(this)) {
            CallEndBackAd.loadAd(this);
        }

        setupViewPager();

        tvTimeType.setText(timeType);
        tvDuration.setText(duration);

        ivCall.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
            ClEndActivity.this.finish();
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getBack();
            }
        });
    }

    private void showAd() {
        if (!AdPlacement.isNetworkAvailable(this) || !AdPlacement.getClEndAdShow()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }

            rlAdView.setVisibility(View.GONE);
            return;
        }

        rlAdView.setVisibility(View.VISIBLE);

        if ("banner".equalsIgnoreCase(AdPlacement.getClEndAdType())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }

            rlBannerAdView.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.GONE);
            AdPlacement.loadAdaptiveBannerAd(this, AdPlacement.getClEndBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
        } else {
            rlBannerAdView.setVisibility(View.GONE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(this, AdPlacement.getClEndNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "large");
        }
    }

    @SuppressLint("InflateParams")
    private void setupViewPager() {
        vpData.setSaveEnabled(false);
        vpData.setAdapter(new ClEndPagerAdapter(this));
        vpData.setOffscreenPageLimit(4);
        vpData.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                hideKeyboard();
            }
        });

        new TabLayoutMediator(tlOption, vpData, (tab, position) -> {
            View view = getLayoutInflater().inflate(R.layout.item_tab_icon_cl_end, null);
            AppCompatImageView ivTabIcon = view.findViewById(R.id.ivTabIcon);

            switch (position) {
                case 0:
                    ivTabIcon.setImageResource(R.drawable.ic_weather_cl_end);
                    break;
                case 1:
                    ivTabIcon.setImageResource(R.drawable.ic_message_cl_end);
                    break;
                case 2:
                    ivTabIcon.setImageResource(R.drawable.ic_reminder_cl_end);
                    break;
                case 3:
                    ivTabIcon.setImageResource(R.drawable.ic_more_cl_end);
                    break;
            }
            tab.setCustomView(view);
        }).attach();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            focus.clearFocus();
        }
        View tokenView = focus != null ? focus : vpData;
        if (tokenView == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && tokenView.getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
        }
    }

    public void getBack() {
        String callType = getIntent().getStringExtra("CallType");
        boolean isFirebaseFlow = "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);

        boolean showAd;
        if (isFirebaseFlow) {
            showAd = AdPlacement.getNotificationBackAdShow();
        } else {
            showAd = AdPlacement.getClEndBackAdShow();
        }
        if (showAd) {
            CallEndBackAd.fullScreenAdShow(ClEndActivity.this, check -> {
                if (check) {
                    AdPlacement.setClEndLastShowTime(getApplicationContext(), System.currentTimeMillis());
                }
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
            });
        } else {
            finish();
        }
    }
}