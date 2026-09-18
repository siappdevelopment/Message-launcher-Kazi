package com.messages.smart.sms.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.ScreenFlowNavigator;
import com.messages.smart.sms.common.Utils;

public class CollectionActivity extends AppCompatActivity {
    private AppCompatTextView tvTitle, tvIntro1, tvIntro2, tvWhatWeUse, tvBullet1, tvBullet2, tvBullet3, tvBullet4, tvFooter, tvAgreeLabel, tvAgreeContinue;
    private AppCompatCheckBox cbAgree;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout btnSetDefault, slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;
    private boolean navigatedAway;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.applyStoredLocale(this);
        setContentView(R.layout.activity_collection);

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
        tvIntro1 = findViewById(R.id.tvIntro1);
        tvIntro2 = findViewById(R.id.tvIntro2);
        tvWhatWeUse = findViewById(R.id.tvWhatWeUse);
        tvBullet1 = findViewById(R.id.tvBullet1);
        tvBullet2 = findViewById(R.id.tvBullet2);
        tvBullet3 = findViewById(R.id.tvBullet3);
        tvBullet4 = findViewById(R.id.tvBullet4);
        tvFooter = findViewById(R.id.tvFooter);
        tvAgreeLabel = findViewById(R.id.tvAgreeLabel);
        tvAgreeContinue = findViewById(R.id.tvAgreeContinue);
        cbAgree = findViewById(R.id.cbAgree);
        View llAgreeRow = findViewById(R.id.llAgreeRow);
        ShimmerFrameLayout btnAgreeContinue = findViewById(R.id.btnAgreeContinue);

        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);

        setupFooterLinks();

        if (!AdPlacement.getCollectionAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getCollectionAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadBannerAd(this, AdPlacement.getCollectionBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getCollectionNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "small");
            }
        }

        llAgreeRow.setOnClickListener(v -> cbAgree.toggle());

        btnAgreeContinue.setOnClickListener(v -> {
            if (navigatedAway || isFinishing()) {
                return;
            }
            if (!cbAgree.isChecked()) {
                Toast.makeText(this, R.string.collection_agree_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            navigatedAway = true;
            Utils.setCollectionCompleted(getApplicationContext(), true);
            ScreenFlowNavigator.continueAfter(CollectionActivity.this, AdPlacement.SCREEN_COLLECTION);
        });
    }

    private void setupFooterLinks() {
        String fullText = getString(R.string.collection_footer);
        String terms = getString(R.string.terms);
        String privacy = getString(R.string.privacy_policy);

        SpannableString spannable = new SpannableString(fullText);
        applyClickableSpan(spannable, fullText, terms, this::openTerms);
        applyClickableSpan(spannable, fullText, privacy, this::openPrivacyPolicy);

        tvFooter.setText(spannable);
        tvFooter.setMovementMethod(LinkMovementMethod.getInstance());
        tvFooter.setHighlightColor(Color.TRANSPARENT);
    }

    private void applyClickableSpan(SpannableString spannable, String fullText, String linkText, Runnable onClick) {
        int start = fullText.indexOf(linkText);
        if (start < 0) {
            return;
        }
        int end = start + linkText.length();
        int linkColor = ContextCompat.getColor(this, R.color.primary);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(linkColor);
                ds.setUnderlineText(true);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openPrivacyPolicy() {
        String url = AdPlacement.getPrivacyPolicy();
        if (url != null && !url.isEmpty() && Utils.openHttpUrl(this, url)) {
            overridePendingTransition(0, 0);
        } else {
            Toast.makeText(this, "Privacy Policy URL Not Found !", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTerms() {
        String url = AdPlacement.getTermsConditions();
        if (url != null && !url.isEmpty() && Utils.openHttpUrl(this, url)) {
            overridePendingTransition(0, 0);
        } else {
            Toast.makeText(this, "Terms & Conditions URL Not Found !", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.collection_title));
            tvIntro1.setText(getString(R.string.collection_intro_1));
            tvIntro2.setText(getString(R.string.collection_intro_2));
            tvWhatWeUse.setText(getString(R.string.collection_what_we_use));
            tvBullet1.setText(getString(R.string.collection_bullet_1));
            tvBullet2.setText(getString(R.string.collection_bullet_2));
            tvBullet3.setText(getString(R.string.collection_bullet_3));
            tvBullet4.setText(getString(R.string.collection_bullet_4));
            tvAgreeLabel.setText(getString(R.string.collection_agree_checkbox));
            tvAgreeContinue.setText(getString(R.string.collection_agree_continue));
            setupFooterLinks();
        }
    }
}