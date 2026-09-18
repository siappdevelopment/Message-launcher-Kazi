package com.messages.smart.sms.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.IntroNavigation;

public class Intro3Activity extends AppCompatActivity {
    private AppCompatTextView tvTitle, tvDescription, tvNext;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout btnNext, slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd, llIndicator;
    private FrameLayout flNativeAd;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_3);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        initViews();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        btnNext = findViewById(R.id.btnNext);
        tvNext = findViewById(R.id.tvNext);
        llIndicator = findViewById(R.id.llIndicator);
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
        IntroNavigation.setupIntroButtonIndicators(this, llIndicator, 3);

        if (!AdPlacement.getIntroButtonAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getIntroButtonAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadBannerAd(this, AdPlacement.getIntroButtonBannerId3(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getIntroButtonNativeId3(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
            }
        }

        btnNext.setOnClickListener(view -> IntroNavigation.goToNextIntroButtonScreen(Intro3Activity.this, 3));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.intro_3_title));
            tvDescription.setText(getString(R.string.intro_3_description));
            tvNext.setText(getString(R.string.next));
        }
    }
}