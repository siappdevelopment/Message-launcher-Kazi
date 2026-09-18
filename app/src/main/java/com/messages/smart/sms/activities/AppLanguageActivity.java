package com.messages.smart.sms.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.messages.smart.sms.R;
import com.messages.smart.sms.adapters.AppLanguageAdapter;
import com.messages.smart.sms.common.AdPlacement;
import com.messages.smart.sms.common.ScreenFlowNavigator;
import com.messages.smart.sms.common.Utils;
import com.messages.smart.sms.interfaces.OnLanguageClickListener;

import java.util.ArrayList;

public class AppLanguageActivity extends AppCompatActivity implements OnLanguageClickListener {
    private AppCompatImageView ivBack;
    private LinearLayout llTitle, llHeader;
    private AppCompatTextView tvTitle, tvSubTitle, tvDone, tvNext;
    private RecyclerView rvLanguage;
    private RelativeLayout rlAdView, rlBannerAdView, rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer, slNativeShimmer;
    public LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    private AppLanguageAdapter appLanguageAdapter;

    private String savedLanguageCode;
    private String savedLanguageName;
    private String pendingLanguageCode;
    private String pendingLanguageName;
    private boolean languageChangedInSession;

    private final ArrayList<Integer> arrayListIcon = new ArrayList<>();
    private final ArrayList<String> arrayListName = new ArrayList<>();
    private final ArrayList<String> arrayListSubName = new ArrayList<>();
    private final ArrayList<String> arrayListCode = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getBooleanExtra(ScreenFlowNavigator.EXTRA_LANGUAGE_FLOW_STARTING, false)) {
            Utils.isAppLanguageStarting = true;
        }
        setContentView(R.layout.activity_app_language);

        initViews();
    }

    private void initViews() {
        llTitle = findViewById(R.id.llTitle);
        tvTitle = findViewById(R.id.tvTitle);
        llHeader = findViewById(R.id.llHeader);
        ivBack = findViewById(R.id.ivBack);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        tvDone = findViewById(R.id.tvDone);
        rvLanguage = findViewById(R.id.rvLanguage);
        tvNext = findViewById(R.id.tvNext);

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
        if (!AdPlacement.getLanguageAdShow()) {
            rlAdView.setVisibility(View.GONE);
        } else {
            rlAdView.setVisibility(View.VISIBLE);

            if ("banner".equalsIgnoreCase(AdPlacement.getLanguageAdType())) {
                rlBannerAdView.setVisibility(View.VISIBLE);
                rlNativeAdView.setVisibility(View.GONE);
                AdPlacement.loadAdaptiveBannerAd(this, AdPlacement.getLanguageBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd);
            } else {
                rlBannerAdView.setVisibility(View.GONE);
                rlNativeAdView.setVisibility(View.VISIBLE);
                AdPlacement.loadNativeAd(this, AdPlacement.getLanguageNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "large");
            }
        }

        if (Utils.isAppLanguageStarting) {
            llTitle.setVisibility(VISIBLE);
            llHeader.setVisibility(GONE);
            tvNext.setVisibility(View.GONE);
        } else {
            llTitle.setVisibility(GONE);
            llHeader.setVisibility(VISIBLE);
            tvDone.setVisibility(View.GONE);
        }

        loadLanguages();
        setLanguageAdapter();

        ivBack.setOnClickListener(view -> finishWithoutSaving());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithoutSaving();
            }
        });

        tvDone.setOnClickListener(view -> {
            applySelectedLanguage();
            finishAfterLanguageApplied();
        });

        tvNext.setOnClickListener(view -> {
            Runnable goNext = () -> {
                applySelectedLanguage();
                if (Utils.isAppLanguageStarting) {
                    ScreenFlowNavigator.continueAfter(AppLanguageActivity.this, AdPlacement.SCREEN_LANGUAGE);
                } else {
                    finishAfterLanguageApplied();
                }
            };

            if (Utils.isAppLanguageStarting && AdPlacement.getLanguageInterstitialAdShow()) {
                AdPlacement.loadInterstitialAd(this, AdPlacement.getOtherInterstitialId(), goNext::run);
            } else {
                goNext.run();
            }
        });
    }

    private void loadLanguages() {
        arrayListIcon.clear();
        arrayListName.clear();
        arrayListSubName.clear();
        arrayListCode.clear();

        arrayListIcon.add(R.drawable.ic_english);
        arrayListIcon.add(R.drawable.ic_hindi);
        arrayListIcon.add(R.drawable.ic_russian);
        arrayListIcon.add(R.drawable.ic_italian);
        arrayListIcon.add(R.drawable.ic_french);
        arrayListIcon.add(R.drawable.ic_spanish);
        arrayListIcon.add(R.drawable.ic_portuguese_portugal);
        arrayListIcon.add(R.drawable.ic_german);
        arrayListIcon.add(R.drawable.ic_japanese);
        arrayListIcon.add(R.drawable.ic_korean);
        arrayListIcon.add(R.drawable.ic_portuguese_brazil);
        arrayListIcon.add(R.drawable.ic_afrikaans);
        arrayListIcon.add(R.drawable.ic_greek);
        arrayListIcon.add(R.drawable.ic_finnish);
        arrayListIcon.add(R.drawable.ic_danish);
        arrayListIcon.add(R.drawable.ic_croatian);
        arrayListIcon.add(R.drawable.ic_lithuanian);
        arrayListIcon.add(R.drawable.ic_dutch);
        arrayListIcon.add(R.drawable.ic_romanian);
        arrayListIcon.add(R.drawable.ic_swedish);
        arrayListIcon.add(R.drawable.ic_thai);
        arrayListIcon.add(R.drawable.ic_filipino);
        arrayListIcon.add(R.drawable.ic_turkish);
        arrayListIcon.add(R.drawable.ic_ukrainian);
        arrayListIcon.add(R.drawable.ic_vietnamese);
        arrayListIcon.add(R.drawable.ic_chinese);

        arrayListName.add("English (Default)");
        arrayListName.add("Hindi");
        arrayListName.add("Russian");
        arrayListName.add("Italian");
        arrayListName.add("French");
        arrayListName.add("Spanish");
        arrayListName.add("Portuguese (Portugal)");
        arrayListName.add("German");
        arrayListName.add("Japanese");
        arrayListName.add("Korean");
        arrayListName.add("Portuguese (Brazil)");
        arrayListName.add("Afrikaans");
        arrayListName.add("Greek");
        arrayListName.add("Finnish");
        arrayListName.add("Danish");
        arrayListName.add("Croatian");
        arrayListName.add("Lithuanian");
        arrayListName.add("Dutch");
        arrayListName.add("Romanian");
        arrayListName.add("Swedish");
        arrayListName.add("Thai");
        arrayListName.add("Filipino");
        arrayListName.add("Turkish");
        arrayListName.add("Ukrainian");
        arrayListName.add("Vietnamese");
        arrayListName.add("Chinese");

        arrayListSubName.add("English");
        arrayListSubName.add("हिंदी");
        arrayListSubName.add("Русский");
        arrayListSubName.add("Italiano");
        arrayListSubName.add("Français");
        arrayListSubName.add("Española");
        arrayListSubName.add("Português");
        arrayListSubName.add("Deutsch");
        arrayListSubName.add("日本語");
        arrayListSubName.add("한국인");
        arrayListSubName.add("Português");
        arrayListSubName.add("Afrikaans");
        arrayListSubName.add("ελληνικά");
        arrayListSubName.add("Suomalainen");
        arrayListSubName.add("Dansk");
        arrayListSubName.add("Hrvatski");
        arrayListSubName.add("Lietuvių");
        arrayListSubName.add("Nederlands");
        arrayListSubName.add("Română");
        arrayListSubName.add("Svenska");
        arrayListSubName.add("แบบไทย");
        arrayListSubName.add("Filipino");
        arrayListSubName.add("Türkçe");
        arrayListSubName.add("українська");
        arrayListSubName.add("Tiếng Việt");
        arrayListSubName.add("中国人");

        arrayListCode.add("en");
        arrayListCode.add("hi");
        arrayListCode.add("ru");
        arrayListCode.add("it");
        arrayListCode.add("fr");
        arrayListCode.add("es");
        arrayListCode.add("pt");
        arrayListCode.add("de");
        arrayListCode.add("ja");
        arrayListCode.add("ko");
        arrayListCode.add("pt-BR");
        arrayListCode.add("af");
        arrayListCode.add("el");
        arrayListCode.add("fi");
        arrayListCode.add("da");
        arrayListCode.add("hr");
        arrayListCode.add("lt");
        arrayListCode.add("nl");
        arrayListCode.add("ro");
        arrayListCode.add("sv");
        arrayListCode.add("th");
        arrayListCode.add("fil");
        arrayListCode.add("tr");
        arrayListCode.add("uk");
        arrayListCode.add("vi");
        arrayListCode.add("zh");

        setLanguageAdapter();
    }

    private void loadSavedLanguage() {
        savedLanguageCode = Utils.getAppLanguageNew(getApplicationContext());
        savedLanguageName = Utils.getAppLanguageNameNew(getApplicationContext());
        if (!languageChangedInSession) {
            pendingLanguageCode = savedLanguageCode;
            pendingLanguageName = savedLanguageName;
        }
        Utils.appLanguage = pendingLanguageCode;
        Utils.appLanguageName = pendingLanguageName;
    }

    private void updateLanguageUi() {
        Context localized = Utils.createLocaleContext(this, pendingLanguageCode != null ? pendingLanguageCode : savedLanguageCode);
        if (localized == null) {
            localized = this;
        }
        tvTitle.setText(localized.getString(R.string.select_language));
        tvSubTitle.setText(localized.getString(R.string.select_language));
        tvDone.setText(localized.getString(R.string.done));
        tvNext.setText(localized.getString(R.string.next));
    }

    private void setLanguageAdapter() {
        appLanguageAdapter = new AppLanguageAdapter(this, arrayListIcon, arrayListName, arrayListSubName, arrayListCode, this);
        rvLanguage.setLayoutManager(new LinearLayoutManager(this));
        rvLanguage.setAdapter(appLanguageAdapter);
    }

    private void applySelectedLanguage() {
        String languageCode = pendingLanguageCode;
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = savedLanguageCode;
        }
        String languageName = pendingLanguageName;
        if (languageName == null || languageName.isEmpty()) {
            languageName = savedLanguageName;
        }
        Utils.saveAndApplyAppLanguage(this, languageCode, languageName);
        languageChangedInSession = false;
    }

    private void finishAfterLanguageApplied() {
        finish();
        overridePendingTransition(0, 0);
    }

    private void finishWithoutSaving() {
        languageChangedInSession = false;
        pendingLanguageCode = savedLanguageCode;
        pendingLanguageName = savedLanguageName;
        Utils.appLanguage = savedLanguageCode;
        Utils.appLanguageName = savedLanguageName;
        finish();
        overridePendingTransition(0, 0);
    }

    private void previewSelectedLanguage(String languageCode) {
        updateLanguageUi();
        if (appLanguageAdapter != null) {
            appLanguageAdapter.updateSelectedLanguage(languageCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedLanguage();
        previewSelectedLanguage(pendingLanguageCode);
    }

    @Override
    public void onLanguageClick(String value) {
        languageChangedInSession = true;
        pendingLanguageCode = value;
        int index = arrayListCode.indexOf(value);
        pendingLanguageName = index >= 0 ? arrayListName.get(index) : savedLanguageName;
        Utils.appLanguage = pendingLanguageCode;
        Utils.appLanguageName = pendingLanguageName;
        previewSelectedLanguage(value);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (Utils.isAppLanguageStarting) {
                tvNext.setVisibility(VISIBLE);
            } else {
                tvDone.setVisibility(VISIBLE);
            }
        }, 500);
    }
}